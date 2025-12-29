package mc.pvpbulgaria.pvpbgclans.gui.menus;

import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import mc.pvpbulgaria.pvpbgclans.gui.menu.ItemBuilder;
import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ClanStatsMenu extends Menu {

    public ClanStatsMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&dClan Stats"); }
    @Override public int size() { return 54; }

    @Override
    public void draw(Player p) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { new MainMenu(menus).open(p); return; }

        int trophies = menus.getPlugin().getClanManager().getClanTotalTrophies(clan);
        int level = menus.getPlugin().getClanManager().getClanLevel(clan);
        int rank = menus.getPlugin().getClanManager().getClanRank(clan);
        int next = menus.getPlugin().getClanManager().getNextLevelRequirement(clan);
        int maxLevel = menus.getPlugin().getClanManager().getLevelThresholds().size();

        set(4, new ItemBuilder(Material.NETHER_STAR).name("&d" + clan.getName())
                .lore("&7Trophies: &f" + trophies,
                        "&7Level: &f" + level + "&7/&f" + maxLevel,
                        "&7Rank: &f#" + rank,
                        next == -1 ? "&7Next: &aMAX" : "&7Next: &f" + next + " &7trophies")
                .build());

        List<UUID> members = new ArrayList<>(clan.getMembers());
        members.sort((a,b) -> Integer.compare(clan.getTrophies(b), clan.getTrophies(a)));

        int slot = 9;
        for (UUID u : members) {
            if (slot >= 54) break;
            String n = Bukkit.getOfflinePlayer(u).getName();
            if (n == null) n = u.toString();

            set(slot++, new ItemBuilder(Material.PAPER)
                    .name("&f" + n)
                    .lore("&7Trophies: &f" + clan.getTrophies(u))
                    .build());
        }

        set(49, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 49) new MainMenu(menus).open(p);
    }
}
