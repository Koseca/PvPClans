package mc.pvpbulgaria.pvpbgclans.gui.menus;

import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import mc.pvpbulgaria.pvpbgclans.gui.menu.ItemBuilder;
import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TopMenu extends Menu {

    private final int page; // 0-based

    public TopMenu(MenuManager menus, int page) {
        super(menus);
        this.page = Math.max(0, page);
    }

    @Override public String title() { return CC.color("&bTop Clans &7(Page " + (page + 1) + ")"); }
    @Override public int size() { return 54; }

    @Override
    public void draw(Player p) {
        int topSize = menus.getPlugin().getConfig().getInt("clan.top-size", 10);
        if (topSize > 50) topSize = 50;

        List<Clan> top = menus.getPlugin().getClanManager().getTopClans(50); // keep enough for paging
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(top.size(), start + perPage);

        int slot = 0;
        for (int i = start; i < end; i++) {
            Clan c = top.get(i);
            int trophies = menus.getPlugin().getClanManager().getClanTotalTrophies(c);
            int level = menus.getPlugin().getClanManager().getClanLevel(c);

            set(slot++, new ItemBuilder(Material.DIAMOND)
                    .name("&7#" + (i + 1) + " &b" + c.getName())
                    .lore("&7Trophies: &f" + trophies, "&7Level: &f" + level)
                    .build());
        }

        set(45, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
        if (page > 0) set(48, new ItemBuilder(Material.ARROW).name("&ePrevious").lore("&7Page " + page).build());
        if (end < top.size()) set(50, new ItemBuilder(Material.ARROW).name("&eNext").lore("&7Page " + (page + 2)).build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 45) { new MainMenu(menus).open(p); return; }
        if (slot == 48 && page > 0) { new TopMenu(menus, page - 1).open(p); return; }
        if (slot == 50) { new TopMenu(menus, page + 1).open(p); }
    }
}
