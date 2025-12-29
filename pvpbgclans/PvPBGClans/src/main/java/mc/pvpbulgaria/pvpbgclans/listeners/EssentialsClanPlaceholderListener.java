package mc.pvpbulgaria.pvpbgclans.listeners;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Replaces {CLAN} in chat format with the clan prefix (level-based) or hides it when no clan.
 * Intended to be used with Essentials formats that contain {CLAN} to avoid % placeholders.
 */
public class EssentialsClanPlaceholderListener implements Listener {

    private final PvPBGClans plugin;

    public EssentialsClanPlaceholderListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event == null) return;
        String format = event.getFormat();
        if (format == null || !format.contains("{CLAN}")) return; // nothing to do

        Clan clan = plugin.getClanManager().getClanByPlayer(event.getPlayer().getUniqueId());
        String replacement = "";
        if (clan != null) {
            replacement = plugin.getClanManager().getClanPrefix(clan);
        }

        String newFormat = format.replace("{CLAN}", replacement);
        event.setFormat(newFormat);
    }
}

