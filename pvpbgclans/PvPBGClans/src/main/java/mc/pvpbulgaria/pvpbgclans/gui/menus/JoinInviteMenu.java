package mc.pvpbulgaria.pvpbgclans.gui.menus;

import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import mc.pvpbulgaria.pvpbgclans.gui.menu.ItemBuilder;
import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JoinInviteMenu extends Menu {

    public JoinInviteMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&eJoin Clan"); }
    @Override public int size() { return 27; }

    @Override
    public void draw(Player p) {
        String clanName = menus.getPlugin().getClanManager().getPendingInviteClanName(p.getUniqueId());

        if (clanName == null) {
            set(13, new ItemBuilder(Material.BARRIER).name("&cNo pending invite")
                    .lore("&7Ask a leader to invite you").build());
        } else {
            set(11, new ItemBuilder(Material.PAPER).name("&aInvite found")
                    .lore("&7Clan: &f" + clanName).build());

            set(15, new ItemBuilder(Material.EMERALD).name("&aJoin " + clanName)
                    .lore("&7Click to join").build());
        }

        set(22, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 22) { new MainMenu(menus).open(p); return; }

        String clanName = menus.getPlugin().getClanManager().getPendingInviteClanName(p.getUniqueId());
        if (clanName != null && slot == 15) {
            p.closeInventory();
            // perform join logic directly to avoid issuing a command string
            mc.pvpbulgaria.pvpbgclans.clan.Clan clan = menus.getPlugin().getClanManager().getClanByName(clanName);
            if (clan != null) {
                boolean added = menus.getPlugin().getClanManager().addMember(clan, p.getUniqueId());
                if (added) {
                    p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.joined", "&aYou joined clan &e%clan%&a.")).replace("%clan%", clan.getName()));
                } else {
                    p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + "&cFailed to join clan."));
                }
            } else {
                p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + "&cClan not found."));
            }
        }
    }
}
