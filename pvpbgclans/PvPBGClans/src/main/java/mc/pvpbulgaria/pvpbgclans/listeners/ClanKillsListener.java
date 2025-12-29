package mc.pvpbulgaria.pvpbgclans.listeners;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ClanKillsListener implements Listener {

    private final PvPBGClans plugin;

    public ClanKillsListener(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // optional safety: don't count suicides
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        Clan clan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
        if (clan == null) return;

        // 1 kill = 1 trophy (ONLY while in clan)
        clan.addTrophy(killer.getUniqueId());

        // async debounced save
        plugin.getClanManager().markDirty();
    }
}
