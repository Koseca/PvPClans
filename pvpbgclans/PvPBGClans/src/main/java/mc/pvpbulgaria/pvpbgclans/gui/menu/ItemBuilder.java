package mc.pvpbulgaria.pvpbgclans.gui.menu;

import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final List<String> lore = new ArrayList<>();

    public ItemBuilder(Material mat) {
        this.item = new ItemStack(mat);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(CC.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        lore.clear();
        for (String s : lines) lore.add(CC.color(s));
        meta.setLore(lore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
