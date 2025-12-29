package mc.pvpbulgaria.pvpbgclans.listeners;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ClanFriendlyFireListener implements Listener {

    private final PvPBGClans plugin;

    public ClanFriendlyFireListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity victimEnt = e.getEntity();
        if (!(victimEnt instanceof Player)) return;

        Player victim = (Player) victimEnt;

        Player attacker = getAttacker(e.getDamager());
        if (attacker == null) return;

        // same player / weird cases
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());
        if (victimClan == null) return;

        Clan attackerClan = plugin.getClanManager().getClanByPlayer(attacker.getUniqueId());
        if (attackerClan == null) return;

        // cancel if same clan
        if (victimClan.getName().equalsIgnoreCase(attackerClan.getName())) {
            e.setCancelled(true);
        }
    }

    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;

        // arrows/snowballs/etc.
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            Object shooter = proj.getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }

        return null;
    }
}
