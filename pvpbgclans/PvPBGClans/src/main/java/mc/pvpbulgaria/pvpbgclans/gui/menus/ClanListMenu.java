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

public class ClanListMenu extends Menu {

    public ClanListMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&bClan Members"); }
    @Override public int size() { return 54; }

    @Override
    public void draw(Player p) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { new MainMenu(menus).open(p); return; }

        String leaderName = Bukkit.getOfflinePlayer(clan.getLeader()).getName();
        if (leaderName == null) leaderName = clan.getLeader().toString();

        set(4, new ItemBuilder(Material.BOOK).name("&b" + clan.getName())
                .lore("&7Leader: &f" + leaderName,
                        "&7Members: &f" + clan.size() + "&7/&f" + menus.getPlugin().getClanManager().getMaxMembers())
                .build());

        List<UUID> members = new ArrayList<>(clan.getMembers());
        members.sort((a,b) -> {
            String an = Bukkit.getOfflinePlayer(a).getName(); if (an == null) an = a.toString();
            String bn = Bukkit.getOfflinePlayer(b).getName(); if (bn == null) bn = b.toString();
            return an.compareToIgnoreCase(bn);
        });

        int slot = 9;
        for (UUID u : members) {
            if (slot >= 54) break;
            String n = Bukkit.getOfflinePlayer(u).getName();
            if (n == null) n = u.toString();

            boolean isLeader = u.equals(clan.getLeader());
            set(slot++, new ItemBuilder(Material.PAPER)
                    .name((isLeader ? "&e" : "&f") + n)
                    .lore(isLeader ? "&7Role: &eLeader" : "&7Role: &fMember")
                    .build());
        }

        set(49, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 49) new MainMenu(menus).open(p);
    }
}
