package mc.pvpbulgaria.pvpbgclans.clan;



import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClanSummonManager {

    private final PvPBGClans plugin;

    // target -> summon request
    private final Map<UUID, SummonRequest> pending = new ConcurrentHashMap<>();

    // clanName -> untilMillis (cooldown until this timestamp)
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public ClanSummonManager(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    /* ================= SEND SUMMON ================= */

    public void sendSummon(Player summoner) {
        Clan clan = plugin.getClanManager().getClanByPlayer(summoner.getUniqueId());
        if (clan == null) {
            summoner.sendMessage(CC.color(plugin.getConfig().getString("messages.prefix", "&7") + plugin.getConfig().getString("messages.must-be-in-clan", "&cYou are not in a clan.")));
            return;
        }

        // LEVEL GATE: check required clan level for summons
        int required = plugin.getConfig().getInt("clan.levels.commands.summon-required-level", 1);
        int currentLevel = plugin.getClanManager().getClanLevel(clan);
        if (currentLevel < required) {
            String tpl = plugin.getConfig().getString("messages.summon-required-level", "&cClan level %level% is required to use this.");
            summoner.sendMessage(CC.color(plugin.getConfig().getString("messages.prefix", "&7") + tpl.replace("%level%", String.valueOf(required))));
            return;
        }

        // config values
        int cost = plugin.getConfig().getInt("clan.summon.cost-trophies", 5);
        // use minutes-based cooldown only. default to 120 minutes (2 hours) when not set
        int cooldownMinutes = plugin.getConfig().getInt("clan.summon.cooldown-minutes", 120);
        long now = System.currentTimeMillis();

        String clanKey = clan.getName();
        Long until = cooldowns.get(clanKey);
        if (until != null && until > now) {
            long left = until - now;
            String tpl = plugin.getConfig().getString("messages.summon-cooldown", "&cClan summon is on cooldown. Time left: &f%time%&c.");
            summoner.sendMessage(CC.color(tpl.replace("%time%", JoinCooldownManager.formatTime(left))));
            return;
        }

        // check cost (use summoner's trophies within the clan)
        if (cost > 0) {
            // Use total clan trophies as the pool for summons (not individual player's trophies)
            int clanTotal = plugin.getClanManager().getClanTotalTrophies(clan);
            if (clanTotal < cost) {
                String tpl = plugin.getConfig().getString("messages.summon-no-trophies", "&cYou need &f%cost% &ctrophies to summon (your clan has &f%have%&c).");
                summoner.sendMessage(CC.color(tpl.replace("%cost%", String.valueOf(cost)).replace("%have%", String.valueOf(clanTotal))));
                return;
            }

            int remaining = cost;

            // Prefer deducting from leader first, then other members (deterministic order)
            List<UUID> members = new ArrayList<>(clan.getMembers());
            // move leader to front
            members.remove(clan.getLeader());
            members.add(0, clan.getLeader());

            Map<UUID, Integer> deducted = new HashMap<>();

            for (UUID u : members) {
                if (remaining <= 0) break;
                int mt = clan.getTrophies(u);
                if (mt <= 0) continue;
                int take = Math.min(mt, remaining);
                clan.setTrophies(u, mt - take);
                deducted.put(u, take);
                remaining -= take;
            }

            // Sanity: remaining should be zero because clanTotal >= cost
            if (remaining > 0) {
                // This should not happen, but roll back in the unlikely case
                for (Map.Entry<UUID, Integer> e : deducted.entrySet()) {
                    UUID u = e.getKey();
                    clan.setTrophies(u, clan.getTrophies(u) + e.getValue());
                }
                summoner.sendMessage(CC.color("&cAn error occurred while deducting clan trophies. Try again later."));
                return;
            }

            // Persist change
            plugin.getClanManager().markDirty();

            String tpl = plugin.getConfig().getString("messages.summon-cost-deducted", "&a%cost% trophies deducted for summon.");
            summoner.sendMessage(CC.color(tpl.replace("%cost%", String.valueOf(cost))));

            // Notify members who had trophies deducted
            for (Map.Entry<UUID, Integer> e : deducted.entrySet()) {
                UUID u = e.getKey();
                int amt = e.getValue();
                Player pl = Bukkit.getPlayer(u);
                if (pl != null) {
                    pl.sendMessage(CC.color(plugin.getConfig().getString("messages.prefix", "&7") + "&c" + amt + " trophies were deducted from your clan for a summon."));
                }
            }
        }

        // apply cooldown for the clan (store until ms)
        if (cooldownMinutes > 0) {
            long untilMs = now + (cooldownMinutes * 60_000L);
            cooldowns.put(clanKey, untilMs);
        }

        Location loc = summoner.getLocation();

        // messages from config
        String receivedTpl = plugin.getConfig().getString("messages.summon-received", "&aYou have received a clan summon from &e%sender%&a. Type &e/clan tpyes &ato accept or &c/clan tpno &ato deny.");
        String sentMsg = plugin.getConfig().getString("messages.summon-request-sent", "&aClan summon request sent to online members.");

        for (UUID u : clan.getMembers()) {
            Player target = Bukkit.getPlayer(u);
            if (target == null || target.equals(summoner)) continue;

            pending.put(
                    target.getUniqueId(),
                    new SummonRequest(summoner.getUniqueId(), loc)
            );

            target.sendMessage(CC.color(receivedTpl.replace("%sender%", summoner.getName())));
        }

        summoner.sendMessage(CC.color(sentMsg));
    }


    /* ================= ACCEPT ================= */

    public void accept(Player player) {
        SummonRequest req = pending.remove(player.getUniqueId());
        if (req == null) {
            player.sendMessage("§cNo active clan summon.");
            return;
        }

        // Use the SAME teleport system as /clan base
        // Prepare custom finish message: use config message 'messages.summon-teleported' if present
        String finishKey = plugin.getConfig().getString("messages.summon-teleported", "&aTeleported to summon portal.");
        String prefix = plugin.getConfig().getString("messages.prefix", "&7");
        String finishMessage = CC.color(prefix + finishKey);

        // Prepare warmup template for summon; fallback to base-warmup if not set
        String warmupTpl = plugin.getConfig().getString("messages.summon-warmup", null);
        String warmupTemplate = warmupTpl == null ? (plugin.getConfig().getString("messages.prefix", "&7") + plugin.getConfig().getString("messages.base-warmup", "&eTeleporting in &f%seconds%&e..."))
                : (plugin.getConfig().getString("messages.prefix", "&7") + warmupTpl);

        // determine warmup seconds for summon (fall back to base warmup seconds)
        int summonWarmup = plugin.getConfig().getInt("clan.summon.warmup-seconds", plugin.getConfig().getInt("clan.base-warmup-seconds", 5));

        // Start teleport with warmup and custom finish message, using summon warmup seconds
        plugin.getBaseTeleportManager().start(player, req.location, finishMessage, warmupTemplate, summonWarmup);

        // Immediate warmup notice (use warmup template with seconds placeholder substituted)
        player.sendMessage(CC.color(warmupTemplate.replace("%seconds%", String.valueOf(summonWarmup))));

        // notify requester
        Player sender = Bukkit.getPlayer(req.sender);
        if (sender != null) {
            sender.sendMessage("§a" + player.getName() + " &lhas accepted the summon and is teleporting to your portal.");
        }
    }



    /* ================= DENY ================= */

    public void deny(Player player) {
        SummonRequest req = pending.remove(player.getUniqueId());
        if (req != null) {
            // notify requester
            Player sender = Bukkit.getPlayer(req.sender);
            if (sender != null) {
                sender.sendMessage("§c" + player.getName() + " &ahas denied your summon request.");
            }
            player.sendMessage("§cYou have denied the clan summon.");
            return;
        }
        player.sendMessage("§cNo active clan summon.");
    }

    /* ================= DATA ================= */

    private static class SummonRequest {
        final UUID sender;
        final Location location;

        SummonRequest(UUID sender, Location location) {
            this.sender = sender;
            this.location = location.clone();
        }
    }
}
