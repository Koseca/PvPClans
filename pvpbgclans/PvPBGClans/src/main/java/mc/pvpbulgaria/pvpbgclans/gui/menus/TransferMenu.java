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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferMenu extends Menu {

    private final List<UUID> targets = new ArrayList<>();

    public TransferMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&eTransfer Leadership"); }
    @Override public int size() { return 54; }

    @Override
    public void draw(Player p) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { new MainMenu(menus).open(p); return; }

        targets.clear();
        for (UUID u : clan.getMembers()) {
            if (u.equals(clan.getLeader())) continue;
            targets.add(u);
        }

        int slot = 0;
        for (UUID u : targets) {
            if (slot >= 45) break;
            String name = Bukkit.getOfflinePlayer(u).getName();
            if (name == null) name = u.toString();

            set(slot++, new ItemBuilder(Material.EMERALD)
                    .name("&e" + name)
                    .lore("&7Click to transfer").build());
        }

        set(49, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 49) { new MainMenu(menus).open(p); return; }
        if (slot >= 0 && slot < targets.size() && slot < 45) {
            String name = Bukkit.getOfflinePlayer(targets.get(slot)).getName();
            if (name == null) return;
            p.closeInventory();
            // perform transfer directly
            mc.pvpbulgaria.pvpbgclans.clan.Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
            if (clan == null) return;
            UUID newLeader = targets.get(slot);
            if (!clan.getLeader().equals(p.getUniqueId())) { p.sendMessage(CC.color("&cOnly the leader can transfer leadership.")); return; }
            menus.getPlugin().getClanManager().transferLeadership(clan, p.getUniqueId(), newLeader);
            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.transfer-success", "&aLeadership transferred to &e%player%&a.")).replace("%player%", name));
        }
    }
}
