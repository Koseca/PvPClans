package mc.pvpbulgaria.pvpbgclans.gui.menus;

import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import mc.pvpbulgaria.pvpbgclans.gui.menu.ItemBuilder;
import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DisbandConfirmMenu extends Menu {

    public DisbandConfirmMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&4Confirm Disband"); }
    @Override public int size() { return 27; }

    @Override
    public void draw(Player p) {
        set(11, new ItemBuilder(Material.EMERALD_BLOCK).name("&aYES")
                .lore("&7Disband the clan").build());

        set(15, new ItemBuilder(Material.REDSTONE_BLOCK).name("&cNO")
                .lore("&7Cancel").build());

        set(22, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 22) { new MainMenu(menus).open(p); return; }
        if (slot == 15) { // cancel
            menus.getPlugin().getClanManager().clearDisbandConfirm(p.getUniqueId());
            new MainMenu(menus).open(p);
            return;
        }
        if (slot == 11) {
            p.closeInventory();
            mc.pvpbulgaria.pvpbgclans.clan.Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
            if (clan == null) {
                p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.must-be-in-clan", "&cYou must be in a clan.")));
                return;
            }
            if (!clan.getLeader().equals(p.getUniqueId())) {
                p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.not-leader", "&cOnly the clan leader can do this.")));
                return;
            }

            // directly disband
            menus.getPlugin().getClanManager().disbandClan(clan);
            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.disbanded", "&cClan disbanded.")));
        }
    }
}
