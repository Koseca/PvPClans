package mc.pvpbulgaria.pvpbgclans.gui;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatPromptListener implements Listener {

    private final PvPBGClans plugin;

    public ChatPromptListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getChatPrompts().has(p)) return;

        e.setCancelled(true);

        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("cancel")) {
            plugin.getChatPrompts().cancel(p);
            p.sendMessage(CC.color("&cCancelled."));
            return;
        }

        plugin.getChatPrompts().handle(p, msg);
    }
}
