package mc.pvpbulgaria.pvpbgclans.gui;

import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getInventory() == null) return;

        if (e.getInventory().getHolder() instanceof Menu) {
            e.setCancelled(true);
            Menu menu = (Menu) e.getInventory().getHolder();
            menu.handleClick((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory() == null) return;
        if (e.getInventory().getHolder() instanceof Menu) {
            ((Menu) e.getInventory().getHolder()).handleClose((Player) e.getPlayer());
        }
    }
}
