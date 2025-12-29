package mc.pvpbulgaria.pvpbgclans.listeners;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ClanChatListener implements Listener {

    private final PvPBGClans plugin;

    public ClanChatListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return CC.color(plugin.getConfig().getString("messages.prefix", "&7"));
    }

    private String raw(String key) {
        return CC.color(plugin.getConfig().getString("messages." + key, ""));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();

        if (!plugin.getClanManager().isClanChatToggled(p.getUniqueId())) return;

        final Clan clan = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) return;

        e.setCancelled(true);

        final String msg = e.getMessage();
        // use level-based prefix from ClanManager
        final String clanPrefix = plugin.getClanManager().getClanPrefix(clan);
        final String playerName = p.getName();

        // Copy members async-safe (avoid iterating live set off-thread)
        final java.util.List<java.util.UUID> members = new java.util.ArrayList<>(clan.getMembers());

        // Do Bukkit API calls sync
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String format = raw("clan-chat-format")
                    .replace("%clan%", clanPrefix)
                    .replace("%player%", playerName)
                    .replace("%message%", msg);

            for (java.util.UUID u : members) {
                Player online = org.bukkit.Bukkit.getPlayer(u);
                if (online != null) online.sendMessage(format);
            }

            Bukkit.getConsoleSender().sendMessage("[CLAN-CHAT] " + format);
        });
    }
}
