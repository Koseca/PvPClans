package mc.pvpbulgaria.pvpbgclans.kothclans;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import mc.pvpbulgaria.pvpbgclans.clan.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KothClansPvP implements Listener {

    private final PvPBGClans plugin;
    private final ClanManager clanManager;

    // Избрани точки за /clan wand
    private final Map<Player, Block[]> selections = new HashMap<>();

    // Собственици на кулите и техните зони
    private final Map<String, String> towerOwners = new HashMap<>(); // towerName -> clanName
    private final Map<String, Location[]> towerZones = new HashMap<>(); // towerName -> {corner1, corner2}

    public KothClansPvP(PvPBGClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    // ---------------- Команди ----------------
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        // /clan wand
        if (args.length == 1 && args[0].equalsIgnoreCase("wand")) {
            ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
            player.getInventory().addItem(axe);
            player.sendMessage("§aВзехте брадвата! Изберете ляв и десен ъгъл.");
            return true;
        }

        // /clan make <towerName>
        if (args.length == 2 && args[0].equalsIgnoreCase("make")) {
            String towerName = args[1];
            Block[] points = selections.get(player);
            if (points == null || points[0] == null || points[1] == null) {
                player.sendMessage("§cМоля, първо задайте левия и десния ъгъл с /clan wand!");
                return true;
            }
            towerZones.put(towerName, new Location[]{points[0].getLocation(), points[1].getLocation()});
            player.sendMessage("§aКулата '" + towerName + "' е създадена успешно!");
            return true;
        }

        // /clan settrophies
        if (args.length == 1 && args[0].equalsIgnoreCase("settrophies")) {
            Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage("§cНе си в клан!");
                return true;
            }

            List<Player> onlineMembers = getOnlineMembers(clan);
            for (Player member : onlineMembers) {
                int current = clan.getTrophies(member.getUniqueId());
                clanManager.setPlayerTrophies(clan, member.getUniqueId(), current + 5);
                member.sendMessage("§aПолучихте 5 trophies от вашия клан!");
            }

            player.sendMessage("§aРаздадохте 5 trophies на всички членове на клана!");
            return true;
        }

        return false;
    }

    // ---------------- Селекция на зона ----------------
    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInHand().getType() != Material.DIAMOND_AXE) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Block[] points = selections.getOrDefault(player, new Block[2]);

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                points[0] = block;
                player.sendMessage("§eЛяв ъгъл зададен!");
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                points[1] = block;
                player.sendMessage("§eДесен ъгъл зададен!");
            }

            selections.put(player, points);
            event.setCancelled(true);
        }
    }

    // ---------------- Проверка за зона ----------------
    private boolean isInZone(Player player, Location corner1, Location corner2) {
        Location loc = player.getLocation();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    // ---------------- Завземане на кулата ----------------
    private void claimTower(String towerName, Clan clan) {
        towerOwners.put(towerName, clan.getName());

        List<Player> onlineMembers = getOnlineMembers(clan);
        for (Player member : onlineMembers) {
            int current = clan.getTrophies(member.getUniqueId());
            clanManager.setPlayerTrophies(clan, member.getUniqueId(), current + 5);
            member.sendMessage("§aВашият клан притежава кулата и получихте 5 trophies!");
        }
    }

    // ---------------- Смяна на собственик при убийство ----------------
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Clan deadClan = clanManager.getClanByPlayer(dead.getUniqueId());
        if (deadClan == null) return;

        for (Map.Entry<String, Location[]> entry : towerZones.entrySet()) {
            String towerName = entry.getKey();
            Location[] zone = entry.getValue();

            // Ако убитият е в зоната и неговият клан е собственик
            if (isInZone(dead, zone[0], zone[1]) &&
                    deadClan.getName().equals(towerOwners.get(towerName))) {

                // Проверка за друг клан в зоната
                for (Player p : dead.getWorld().getPlayers()) {
                    Clan pClan = clanManager.getClanByPlayer(p.getUniqueId());
                    if (pClan != null && !pClan.getName().equals(deadClan.getName()) &&
                            isInZone(p, zone[0], zone[1])) {

                        claimTower(towerName, pClan); // нов клан става собственик
                        p.sendMessage("§aВашият клан е завзел кулата!");
                        break;
                    }
                }
            }
        }
    }

    // ---------------- Помощен метод за онлайн членове ----------------
    private List<Player> getOnlineMembers(Clan clan) {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID uuid : clan.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) onlineMembers.add(p);
        }
        return onlineMembers;
    }

    // ---------------- Получаване на собственик на кулата ----------------
    public String getTowerOwner(String towerName) {
        return towerOwners.get(towerName);
    }
}