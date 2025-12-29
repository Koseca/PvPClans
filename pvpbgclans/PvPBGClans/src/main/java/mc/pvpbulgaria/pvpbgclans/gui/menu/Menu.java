package mc.pvpbulgaria.pvpbgclans.gui.menu;

import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class Menu implements InventoryHolder {

    protected final MenuManager menus;
    protected Inventory inv;

    public Menu(MenuManager menus) {
        this.menus = menus;
    }

    public abstract String title();
    public abstract int size();
    public abstract void draw(Player p);

    public void open(Player p) {
        this.inv = Bukkit.createInventory(this, size(), title());
        draw(p);
        p.openInventory(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void set(int slot, ItemStack item) {
        inv.setItem(slot, item);
    }

    public void handleClick(Player p, int slot, ItemStack clicked) {}
    public void handleClose(Player p) {}
}
