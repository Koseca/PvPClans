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

public class KickMenu extends Menu {

    private final List<UUID> onlineTargets = new ArrayList<>();

    public KickMenu(MenuManager menus) { super(menus); }

    @Override public String title() { return CC.color("&cKick Member"); }
    @Override public int size() { return 54; }

    @Override
    public void draw(Player p) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
        if (clan == null) { new MainMenu(menus).open(p); return; }

        onlineTargets.clear();
        for (UUID u : clan.getMembers()) {
            if (u.equals(clan.getLeader())) continue;
            Player pl = Bukkit.getPlayer(u);
            if (pl != null) onlineTargets.add(u);
        }

        int slot = 0;
        for (UUID u : onlineTargets) {
            if (slot >= 36) break;
            String name = Bukkit.getPlayer(u).getName();
            set(slot++, new ItemBuilder(Material.IRON_SWORD)
                    .name("&c" + name)
                    .lore("&7Click to kick (online)").build());
        }

        set(45, new ItemBuilder(Material.ANVIL).name("&cKick Offline by Name")
                .lore("&7Click, then type the name in chat", "&7Type &ccancel&7 to cancel").build());

        set(49, new ItemBuilder(Material.ARROW).name("&aBack").lore("&7Return").build());
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        if (slot == 49) { new MainMenu(menus).open(p); return; }

        if (slot >= 0 && slot < onlineTargets.size() && slot < 36) {
            Player t = Bukkit.getPlayer(onlineTargets.get(slot));
            if (t == null) return;
            p.closeInventory();
            // perform kick using ClanManager API
            mc.pvpbulgaria.pvpbgclans.clan.Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
            if (clan == null) return;
            if (!clan.getLeader().equals(p.getUniqueId())) {
                p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.not-leader", "&cOnly the clan leader can do this.")));
                return;
            }
            menus.getPlugin().getClanManager().removeMember(clan, t.getUniqueId());
            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.kick-success", "&aKicked &e%player% &afrom the clan.")).replace("%player%", t.getName()));
            if (t.isOnline()) t.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.kicked", "&cYou were kicked from clan &e%clan%&c by &e%by%&c.")).replace("%clan%", clan.getName()).replace("%by%", p.getName()));
             return;
        }

        if (slot == 45) {
            p.closeInventory();
            p.sendMessage(CC.color("&cType player name to kick in chat. (&ccancel&c to cancel)"));

            menus.getPlugin().getChatPrompts().begin(p, new mc.pvpbulgaria.pvpbgclans.gui.ChatPromptManager.Prompt() {
                @Override public void onInput(Player p, String msg) {
                    // offline kick by name: find member by name and remove
                    mc.pvpbulgaria.pvpbgclans.clan.Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());
                    if (clan == null) { p.sendMessage(CC.color("&cYou are not in a clan.")); return; }
                    if (!clan.getLeader().equals(p.getUniqueId())) { p.sendMessage(CC.color("&cOnly the clan leader can do this.")); return; }
                    java.util.UUID targetUuid = null;
                    for (java.util.UUID u : clan.getMembers()) {
                        org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(u);
                        String n = off != null ? off.getName() : null;
                        if (n != null && n.equalsIgnoreCase(msg)) { targetUuid = u; break; }
                    }
                    if (targetUuid == null) { p.sendMessage(CC.color("&cCould not find that member.")); return; }
                    menus.getPlugin().getClanManager().removeMember(clan, targetUuid);
                    p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.kick-success", "&aKicked &e%player% &afrom the clan.")).replace("%player%", msg));
                    Player onlineTarget = Bukkit.getPlayer(targetUuid);
                    if (onlineTarget != null) onlineTarget.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.kicked", "&cYou were kicked from clan &e%clan%&c by &e%by%&c.")).replace("%clan%", clan.getName()).replace("%by%", p.getName()));
                 }
                 @Override public void onCancel(Player p) {}
             });
         }
     }
 }
