package mc.pvpbulgaria.pvpbgclans.listeners;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class BaseTeleportMoveListener implements Listener {

    private final PvPBGClans plugin;

    public BaseTeleportMoveListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        plugin.getBaseTeleportManager().handleMove(e.getPlayer(), e.getFrom(), e.getTo());
    }
}
