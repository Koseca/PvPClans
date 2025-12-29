package mc.pvpbulgaria.pvpbgclans.gui.menus;

import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;
import mc.pvpbulgaria.pvpbgclans.gui.menu.ItemBuilder;
import mc.pvpbulgaria.pvpbgclans.gui.menu.Menu;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainMenu extends Menu {

    public MainMenu(MenuManager menus) {
        super(menus);
    }

    @Override public String title() { return CC.color("&bClans"); }
    @Override public int size() { return 27; }

    @Override
    public void draw(Player p) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());

        if (clan == null) {
            set(11, new ItemBuilder(Material.ANVIL).name("&aCreate Clan")
                    .lore("&7Click to create a clan", "&7Type name in chat", "&7Type &ccancel &7to cancel").build());

            set(13, new ItemBuilder(Material.PAPER).name("&eJoin Clan")
                    .lore("&7Click to join from your invite").build());

            set(15, new ItemBuilder(Material.DIAMOND).name("&bTop Clans")
                    .lore("&7Leaderboard").build());
        } else {
            set(10, new ItemBuilder(Material.BOOK).name("&bClan Info / List")
                    .lore("&7Members, leader").build());

            set(11, new ItemBuilder(Material.NETHER_STAR).name("&dClan Stats")
                    .lore("&7Trophies, level, rank").build());

            set(12, new ItemBuilder(Material.DIAMOND).name("&bTop Clans")
                    .lore("&7Leaderboard").build());

            set(13, new ItemBuilder(Material.NAME_TAG).name("&aInvite Player")
                    .lore("&7Leader only").build());

            set(14, new ItemBuilder(Material.REDSTONE).name("&cLeave Clan")
                    .lore("&7Leader cannot leave").build());

            set(15, new ItemBuilder(Material.EMERALD).name("&eTransfer Leadership")
                    .lore("&7Leader only").build());

            set(16, new ItemBuilder(Material.IRON_SWORD).name("&cKick Member")
                    .lore("&7Leader only", "&7Online picker + offline by name").build());

            boolean cc = menus.getPlugin().getClanManager().isClanChatToggled(p.getUniqueId());
            set(22, new ItemBuilder(Material.BOOK_AND_QUILL).name(cc ? "&aClan Chat: ON" : "&cClan Chat: OFF")
                    .lore("&7Click to toggle").build());

            set(26, new ItemBuilder(Material.TNT).name("&4Disband Clan")
                    .lore("&7Leader only", "&7Requires confirmation").build());
        }
    }

    @Override
    public void handleClick(Player p, int slot, ItemStack clicked) {
        Clan clan = menus.getPlugin().getClanManager().getClanByPlayer(p.getUniqueId());

        if (clan == null) {
            if (slot == 11) {
                p.closeInventory();
                p.sendMessage(CC.color("&aType your clan name in chat. (&ccancel&a to cancel)"));

                menus.getPlugin().getChatPrompts().begin(p, new mc.pvpbulgaria.pvpbgclans.gui.ChatPromptManager.Prompt() {
                    @Override public void onInput(Player p, String msg) {
                        // perform create directly via ClanManager to avoid executing the command string (prevents command echo in chat)
                        if (menus.getPlugin().getClanManager().isInClan(p.getUniqueId())) {
                            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.already-in-clan", "&cYou are already in a clan.")));
                            return;
                        }

                        int min = menus.getPlugin().getConfig().getInt("clan.name-min", 3);
                        int max = menus.getPlugin().getConfig().getInt("clan.name-max", 16);
                        String regex = menus.getPlugin().getConfig().getString("clan.allowed-regex", "^[a-zA-Z0-9_]+$");

                        if (msg.length() < min || msg.length() > max || !msg.matches(regex)) {
                            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.invalid-name", "&cInvalid clan name.")));
                            return;
                        }

                        if (menus.getPlugin().getClanManager().clanExists(msg)) {
                            p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.clan-exists", "&cA clan with that name already exists.")));
                            return;
                        }

                        menus.getPlugin().getClanManager().createClan(msg, p.getUniqueId());
                        p.sendMessage(CC.color(menus.getPlugin().getConfig().getString("messages.prefix", "&7") + menus.getPlugin().getConfig().getString("messages.created", "&aClan &e%clan% &acreated!")).replace("%clan%", msg));
                     }
                     @Override public void onCancel(Player p) {}
                 });
                 return;
            }
            if (slot == 13) { new JoinInviteMenu(menus).open(p); return; }
            if (slot == 15) { new TopMenu(menus, 0).open(p); return; }
        } else {
            if (slot == 10) { new ClanListMenu(menus).open(p); return; }
            if (slot == 11) { new ClanStatsMenu(menus).open(p); return; }
            if (slot == 12) { new TopMenu(menus, 0).open(p); return; }
            if (slot == 13) { p.closeInventory(); p.sendMessage(CC.color("&8[&bClans&8] " + "&cUsage: /clan invite <player>")); return; }
            if (slot == 14) { p.performCommand("clan leave"); p.closeInventory(); return; }
            if (slot == 15) { new TransferMenu(menus).open(p); return; }
            if (slot == 16) { new KickMenu(menus).open(p); return; }
            if (slot == 22) {
                menus.getPlugin().getClanManager().toggleClanChat(p.getUniqueId());
                open(p);
                return;
            }
            if (slot == 26) { new DisbandConfirmMenu(menus).open(p); return; }
        }
    }
}
