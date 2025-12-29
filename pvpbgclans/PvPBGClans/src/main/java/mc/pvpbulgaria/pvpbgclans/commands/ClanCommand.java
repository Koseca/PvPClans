package mc.pvpbulgaria.pvpbgclans.commands;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.clan.JoinCooldownManager;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final PvPBGClans plugin;

    public ClanCommand(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    private String msg(String path) {
        return CC.color(plugin.getConfig().getString("messages.prefix", "&7") +
                plugin.getConfig().getString("messages." + path, ""));
    }

    private String raw(String path) {
        return CC.color(plugin.getConfig().getString("messages." + path, ""));
    }

    private String prefix() {
        return CC.color(plugin.getConfig().getString("messages.prefix", "&7"));
    }

    private void send(CommandSender s, String text) {
        s.sendMessage(CC.color(text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            send(sender, prefix() + raw("players-only"));
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0) {
            plugin.getMenuManager().openMainMenu(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                help(p);
                return true;

            case "reload":
                if (!p.hasPermission("pvpbgclans.admin.reload")) {
                    send(p, prefix() + raw("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                send(p, prefix() + raw("reloaded"));
                return true;

            case "create":
                create(p, args);
                return true;

            case "disband":
                disband(p, args);
                return true;

            case "info":
                info(p);
                return true;

            case "transfer":
                transfer(p, args);
                return true;

            case "invite":
                invite(p, args);
                return true;

            case "setbase":
                setBase(p);
                return true;

            case "base":
                base(p);
                return true;

            case "removebase":
                removeBase(p);
                return true;

            case "join":
                join(p, args);
                return true;

            case "kick":
                kick(p, args);
                return true;

            case "list":
                list(p);
                return true;

            case "leave":
                leave(p);
                return true;

            case "stats":
                stats(p);
                return true;

            case "top":
                top(p);
                return true;

            case "chat":
                chat(p);
                return true;

            case "settrophies":
                if (!p.hasPermission("pvpbgclans.admin.settrophies")) { send(p, prefix() + raw("no-permission")); return true; }
                setTrophies(p, args);
                return true;

            case "summon":
                summon(p);
                break;

            case "tpyes":
                plugin.getClanSummonManager().accept(p);
                break;

            case "tpno":
                plugin.getClanSummonManager().deny(p);
                break;

            default:
                send(p, prefix() + raw("unknown-subcommand"));
                return true;
        }
        return false;
    }
    private void summon(Player p) {
        plugin.getClanSummonManager().sendSummon(p);
    }

    private void help(Player p) {
        send(p, CC.color("&8&m------------------------"));
        send(p, CC.color("&b/clan help"));
        if (p.hasPermission("pvpbgclans.admin.settrophies")) send(p, CC.color("&b/clan settrophies <player> <amount>"));
        send(p, CC.color("&b/clan create <name>"));
        send(p, CC.color("&b/clan info"));
        send(p, CC.color("&b/clan stats"));
        send(p, CC.color("&b/clan top"));
        send(p, CC.color("&b/clan chat"));
        send(p, CC.color("&b/clan setbase"));
        send(p, CC.color("&b/clan base"));
        send(p, CC.color("&b/clan removebase"));
        send(p, CC.color("&b/clan disband &7(then /clan disband confirm)"));
        send(p, CC.color("&b/clan transfer <player>"));
        send(p, CC.color("&b/clan invite <player>"));
        send(p, CC.color("&b/clan join <clan>"));
        send(p, CC.color("&b/clan leave"));
        send(p, CC.color("&b/clan list"));
        send(p, CC.color("&b/clan kick <player>"));
        send(p, CC.color("&b/clan summon"));
        send(p, CC.color("&b/clan tpyes"));
        send(p, CC.color("&b/clan tpno"));

        if (p.hasPermission("pvpbgclans.admin.reload")) {
            send(p, CC.color("&b/clan reload"));
        }
        send(p, CC.color("&8&m------------------------"));
    }

    private void create(Player p, String[] args) {

        if (plugin.getClanManager().isInClan(p.getUniqueId())) {
            send(p, prefix() + raw("already-in-clan"));
            return;
        }

        if (args.length < 2) {
            send(p, CC.color(prefix() + "&cUsage: /clan create <name>"));
            return;
        }

        String name = args[1];

        int min = plugin.getConfig().getInt("clan.name-min", 3);
        int max = plugin.getConfig().getInt("clan.name-max", 16);
        String regex = plugin.getConfig().getString("clan.allowed-regex", "^[a-zA-Z0-9_]+$");

        if (name.length() < min || name.length() > max || !name.matches(regex)) {
            send(p, prefix() + raw("invalid-name"));
            return;
        }

        if (plugin.getClanManager().clanExists(name)) {
            send(p, prefix() + raw("clan-exists"));
            return;
        }

        plugin.getClanManager().createClan(name, p.getUniqueId());
        send(p, (prefix() + raw("created")).replace("%clan%", name));
    }

    private void chat(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        boolean nowOn = plugin.getClanManager().toggleClanChat(p.getUniqueId());
        send(p, prefix() + raw(nowOn ? "clan-chat-on" : "clan-chat-off"));
    }


    private void disband(Player p, String[] args) {

        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) {
            send(p, prefix() + raw("must-be-in-clan"));
            return;
        }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("not-leader"));
            return;
        }

        // /clan disband confirm
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            if (!plugin.getClanManager().canConfirmDisband(p.getUniqueId())) {
                send(p, prefix() + raw("disband-expired"));
                return;
            }
            plugin.getClanManager().clearDisbandConfirm(p.getUniqueId());
            plugin.getClanManager().disbandClan(clan);
            send(p, prefix() + raw("disbanded"));
            return;
        }

        // optional: /clan disband cancel
        if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
            plugin.getClanManager().clearDisbandConfirm(p.getUniqueId());
            send(p, prefix() + raw("disband-cancelled"));
            return;
        }

        // first step: prompt confirmation
        plugin.getClanManager().startDisbandConfirm(p.getUniqueId());
        send(p, (prefix() + raw("disband-confirm"))
                .replace("%seconds%", String.valueOf(plugin.getClanManager().getDisbandConfirmSeconds())));
    }

    private void info(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) {
            send(p, prefix() + raw("must-be-in-clan"));
            return;
        }

        send(p, CC.color("&8&m------------------------"));
        send(p, CC.color("&bClan: &f" + clan.getName()));
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeader());
        send(p, CC.color("&bLeader: &f" + (leader != null ? leader.getName() : clan.getLeader().toString())));
        send(p, CC.color("&bMembers: &f" + clan.getMembers().size() + "&7/&f" + plugin.getClanManager().getMaxMembers()));
        send(p, CC.color("&8&m------------------------"));
    }

    private void stats(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        int trophies = plugin.getClanManager().getClanTotalTrophies(clan);
        int level = plugin.getClanManager().getClanLevel(clan);
        int rank = plugin.getClanManager().getClanRank(clan);
        int next = plugin.getClanManager().getNextLevelRequirement(clan);
        int maxLevel = plugin.getClanManager().getLevelThresholds().size();

        int online = 0;
        for (java.util.UUID u : clan.getMembers()) {
            if (org.bukkit.Bukkit.getPlayer(u) != null) online++;
        }

        send(p, CC.color(raw("stats-header")));
        send(p, CC.color(raw("stats-title").replace("%clan%", clan.getName())));

        send(p, CC.color(raw("stats-trophies").replace("%trophies%", String.valueOf(trophies))));
        send(p, CC.color(raw("stats-level")
                .replace("%level%", String.valueOf(level))
                .replace("%maxlevel%", String.valueOf(maxLevel))));
        send(p, CC.color(raw("stats-rank").replace("%rank%", String.valueOf(rank))));
        send(p, CC.color(raw("stats-online")
                .replace("%online%", String.valueOf(online))
                .replace("%total%", String.valueOf(clan.size()))));

        if (next == -1) send(p, CC.color(raw("stats-max")));
        else send(p, CC.color(raw("stats-next").replace("%next%", String.valueOf(next))));

        send(p, CC.color(raw("stats-footer")));
        for (java.util.UUID u : clan.getMembers()) {
            String name = org.bukkit.Bukkit.getOfflinePlayer(u).getName();
            if (name == null) name = u.toString();

            int memberTrophies = clan.getTrophies(u);

            send(p, CC.color(raw("stats-line")
                    .replace("%player%", name)
                    .replace("%kills%", String.valueOf(memberTrophies))));
        }

        send(p, CC.color(raw("stats-footer")));
    }

    private void transfer(Player p, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) {
            send(p, prefix() + raw("must-be-in-clan"));
            return;
        }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("not-leader"));
            return;
        }

        if (args.length < 2) {
            send(p, CC.color(prefix() + "&cUsage: /clan transfer <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            send(p, CC.color(prefix() + "&cThat player is not online."));
            return;
        }

        if (!clan.isMember(target.getUniqueId())) {
            send(p, prefix() + raw("not-member"));
            return;
        }

        if (plugin.getClanManager().transferLeadership(clan, p.getUniqueId(), target.getUniqueId())) {
            send(p, (prefix() + raw("transfer-success")).replace("%player%", target.getName()));
            send(target, CC.color(prefix() + "&aYou are now the clan leader of &e" + clan.getName() + "&a."));
        }
    }

    private void invite(Player p, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("not-leader"));
            return;
        }

        if (args.length < 2) {
            send(p, CC.color(prefix() + "&cUsage: /clan invite <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { send(p, prefix() + raw("not-online")); return; }
        if (target.getUniqueId().equals(p.getUniqueId())) { send(p, prefix() + raw("cant-invite-self")); return; }

        if (plugin.getClanManager().isInClan(target.getUniqueId())) {
            send(p, CC.color(prefix() + "&cThat player is already in a clan."));
            return;
        }

        if (clan.size() >= plugin.getClanManager().getMaxMembers()) {
            send(p, (prefix() + raw("clan-full")).replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers())));
            return;
        }

        if (plugin.getClanManager().hasInvite(target.getUniqueId())) {
            send(p, prefix() + raw("already-invited"));
            return;
        }

        int hours = plugin.getConfig().getInt("clan.join-cooldown-hours", 0);
        long left = plugin.getJoinCooldowns().getRemainingMillis(target.getUniqueId());
        if (left > 0) {
            send(p, prefix() + raw("join-cooldown-leader")
                    .replace("%time%", JoinCooldownManager.formatTime(left)));
            return;
        }

        plugin.getClanManager().createInvite(target.getUniqueId(), clan);

        send(p, (prefix() + raw("invite-sent"))
                .replace("%player%", target.getName())
                .replace("%clan%", clan.getName()));

        send(target, (prefix() + raw("invite-received"))
                .replace("%inviter%", p.getName())
                .replace("%clan%", clan.getName())
                .replace("%seconds%", String.valueOf(plugin.getClanManager().getInviteExpireSeconds())));
    }

    private void join(Player p, String[] args) {
        if (plugin.getClanManager().isInClan(p.getUniqueId())) {
            send(p, prefix() + raw("already-in-clan"));
            return;
        }

        if (args.length < 2) {
            send(p, CC.color(prefix() + "&cUsage: /clan join <clan>"));
            return;
        }

        String clanName = args[1];
        Clan clan = plugin.getClanManager().getClanByName(clanName);
        if (clan == null) {
            send(p, CC.color(prefix() + "&cClan not found."));
            return;
        }

        // check invite
        if (!plugin.getClanManager().canJoin(p.getUniqueId(), clan.getName())) {
            send(p, prefix() + raw("join-no-invite"));
            return;
        }

        long left = plugin.getJoinCooldowns().getRemainingMillis(p.getUniqueId());
        if (left > 0) {
            send(p, prefix() + raw("join-cooldown-player")
                    .replace("%time%", mc.pvpbulgaria.pvpbgclans.clan.JoinCooldownManager.formatTime(left)));
            return;
        }

        if (clan.size() >= plugin.getClanManager().getMaxMembers()) {
            send(p, (prefix() + raw("clan-full")).replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers())));
            plugin.getClanManager().consumeInvite(p.getUniqueId());
            return;
        }

        boolean added = plugin.getClanManager().addMember(clan, p.getUniqueId());
        plugin.getClanManager().consumeInvite(p.getUniqueId());

        if (!added) {
            send(p, (prefix() + raw("clan-full")).replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers())));
            return;
        }

        send(p, (prefix() + raw("joined")).replace("%clan%", clan.getName()));
    }

    private void leave(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("leader-cant-leave"));
            return;
        }

        plugin.getClanManager().removeMember(clan, p.getUniqueId());
        send(p, prefix() + raw("left"));
        int hours = plugin.getConfig().getInt("clan.join-cooldown-hours", 0);
        plugin.getJoinCooldowns().setJoinCooldown(p.getUniqueId(), hours);
    }

    private void kick(Player p, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("not-leader"));
            return;
        }

        if (args.length < 2) {
            send(p, prefix() + raw("kick-usage"));
            return;
        }

        String targetName = args[1];

        if (targetName.equalsIgnoreCase(p.getName())) {
            send(p, prefix() + raw("kick-self"));
            return;
        }

        // Find by clan membership names (case-insensitive) so offline casing doesn't matter
        UUID targetUuid = null;
        org.bukkit.OfflinePlayer targetOff = null;

        for (UUID u : clan.getMembers()) {
            if (u.equals(clan.getLeader())) continue;

            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(u);
            String n = (off != null) ? off.getName() : null;

            if (n != null && n.equalsIgnoreCase(targetName)) {
                targetUuid = u;
                targetOff = off;
                break;
            }
        }

        if (targetUuid == null) {
            // better than "player-not-found" because they might exist but not be in clan
            send(p, prefix() + raw("not-member"));
            return;
        }

        // kick (works offline because we kick by UUID)
        plugin.getClanManager().removeMember(clan, targetUuid);

        String displayName = (targetOff != null && targetOff.getName() != null) ? targetOff.getName() : targetName;

        // ONE message to leader (no duplicates)
        send(p, (prefix() + raw("kick-success")).replace("%player%", displayName));

        // notify target only if online
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            send(online, (prefix() + raw("kicked"))
                    .replace("%clan%", clan.getName())
                    .replace("%by%", p.getName()));
        }

        // apply join cooldown (join-only, create is allowed)
        int hours = plugin.getConfig().getInt("clan.join-cooldown-hours", 0);
        plugin.getJoinCooldowns().setJoinCooldown(targetUuid, hours);
    }

    private void top(Player p) {
        int size = plugin.getConfig().getInt("clan.top-size", 10);
        if (size > 50) size = 50; // safety

        java.util.List<Clan> top = plugin.getClanManager().getTopClans(size);

        send(p, CC.color(raw("top-header")));
        send(p, CC.color(raw("top-title").replace("%count%", String.valueOf(top.size()))));

        for (int i = 0; i < top.size(); i++) {
            Clan c = top.get(i);
            int trophies = plugin.getClanManager().getClanTotalTrophies(c);
            int level = plugin.getClanManager().getClanLevel(c);

            send(p, CC.color(raw("top-line")
                    .replace("%pos%", String.valueOf(i + 1))
                    .replace("%clan%", c.getName())
                    .replace("%trophies%", String.valueOf(trophies))
                    .replace("%level%", String.valueOf(level))));
        }

        send(p, CC.color(raw("top-footer")));
    }


    private void list(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        String header = CC.color(raw("list-header"));
        String footer = CC.color(raw("list-footer"));

        String leaderName = Bukkit.getOfflinePlayer(clan.getLeader()).getName();
        if (leaderName == null) leaderName = clan.getLeader().toString();

        List<String> memberNames = new ArrayList<>();
        for (UUID u : clan.getMembers()) {
            String n = Bukkit.getOfflinePlayer(u).getName();
            if (n == null) n = u.toString();
            memberNames.add(n);
        }
        Collections.sort(memberNames, String.CASE_INSENSITIVE_ORDER);

        send(p, header);
        send(p, CC.color(raw("list-title").replace("%clan%", clan.getName())));
        send(p, CC.color(raw("list-leader").replace("%leader%", leaderName)));
        send(p, CC.color(raw("list-members")
                .replace("%count%", String.valueOf(clan.size()))
                .replace("%max%", String.valueOf(plugin.getClanManager().getMaxMembers()))
                .replace("%members%", String.join(", ", memberNames))));
        send(p, footer);
    }

    /// base
    private void setBase(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("setbase-leader-only"));
            return;
        }

        // LEVEL GATE: require clan level for setting base
        int required = plugin.getConfig().getInt("clan.levels.commands.setbase-required-level", 1);
        int current = plugin.getClanManager().getClanLevel(clan);
        if (current < required) {
            String tpl = plugin.getConfig().getString("messages.setbase-required-level", "&cClan level %level% is required to set base.");
            send(p, prefix() + raw("setbase-leader-only") + " " + CC.color(tpl.replace("%level%", String.valueOf(required))));
            return;
        }

        clan.setBase(p.getLocation().clone());
        plugin.getClanManager().markDirty();
        send(p, prefix() + raw("base-set"));
    }

    private void base(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (clan.getBase() == null) {
            send(p, prefix() + raw("base-not-set"));
            return;
        }

        plugin.getBaseTeleportManager().start(p, clan.getBase());
    }

    private void removeBase(Player p) {
        Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { send(p, prefix() + raw("must-be-in-clan")); return; }

        if (!clan.getLeader().equals(p.getUniqueId())) {
            send(p, prefix() + raw("removebase-leader-only"));
            return;
        }

        clan.setBase(null);
        plugin.getClanManager().markDirty();
        send(p, prefix() + raw("base-removed"));
    }

    private void setTrophies(Player p, String[] args) {
        if (args.length < 3) {
            send(p, CC.color(prefix() + "&cUsage: /clan settrophies <player> <amount>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            send(p, CC.color(prefix() + "&cPlayer not found."));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            send(p, CC.color(prefix() + "&cInvalid number format."));
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            send(p, prefix() + raw("target-not-in-clan"));
            return;
        }

        plugin.getClanManager().setPlayerTrophies(clan, target.getUniqueId(), amount);
        String playerName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        send(p, (prefix() + raw("trophies-set"))
                .replace("%player%", playerName)
                .replace("%amount%", String.valueOf(amount)));

        // notify the target player if online
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            send(online, (prefix() + raw("trophies-updated")).replace("%amount%", String.valueOf(amount)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], Arrays.asList("help","create","info","list","disband","transfer","invite","join","leave","kick","reload","summon","tpyes","tpno","settrophies"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("transfer")) {
            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            return filter(args[1], names);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            return filter(args[1], names);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            // simple: suggest nothing (or you can suggest clan names if you want)
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {
            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            return filter(args[1], names);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("settrophies")) {
            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            return filter(args[1], names);
        }

        return Collections.emptyList();
    }

    private List<String> filter(String token, List<String> options) {
        String t = token == null ? "" : token.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(t)) out.add(o);
        }
        return out;
    }
}
