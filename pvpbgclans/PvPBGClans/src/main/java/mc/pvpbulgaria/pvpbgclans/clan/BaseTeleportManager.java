package mc.pvpbulgaria.pvpbgclans.clan;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaseTeleportManager {

    private final PvPBGClans plugin;

    private final Map<UUID, PendingTp> pending = new HashMap<>();

    public BaseTeleportManager(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    private String raw(String key) {
        return CC.color(plugin.getConfig().getString("messages." + key, ""));
    }

    private String prefix() {
        return CC.color(plugin.getConfig().getString("messages.prefix", "&7"));
    }

    public boolean isTeleporting(UUID u) {
        synchronized (pending) { return pending.containsKey(u); }
    }

    /**
     * Original start: preserved but delegates to new overload with default finish message key.
     */
    public void start(Player p, Location target) {
        start(p, target, prefix() + raw("base-teleported"));
    }

    /**
     * Start teleport with a custom final message (finishMessage will be sent when teleport completes).
     */
    public void start(Player p, Location target, String finishMessage) {
        // Delegate to the full overload using the default warmup template (base-warmup)
        String warmupTemplate = prefix() + raw("base-warmup");
        start(p, target, finishMessage, warmupTemplate);
    }

    /**
     * Start teleport with a custom final message and a custom warmup template.
     * warmupTemplate may include the token %seconds% which will be replaced each tick.
     */
    public void start(Player p, Location target, String finishMessage, String warmupTemplate) {
        int warmup = plugin.getConfig().getInt("clan.base-warmup-seconds", 5);
        if (warmup < 0) warmup = 0;

        UUID u = p.getUniqueId();

        synchronized (pending) {
            if (pending.containsKey(u)) {
                p.sendMessage(prefix() + raw("base-already-teleporting"));
                return;
            }
        }

        Location start = p.getLocation().clone(); // movement check uses blocks

        PendingTp pt = new PendingTp(target.clone(), start, warmup, finishMessage);

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player pl = Bukkit.getPlayer(u);
            if (pl == null || !pl.isOnline()) {
                cancel(u, false);
                return;
            }

            // if expired, teleport
            if (pt.secondsLeft <= 0) {
                cancel(u, false);
                pl.teleport(pt.target);
                // send the custom finish message (already contains colors/prefix if desired)
                pl.sendMessage(pt.finishMessage);
                return;
            }

            String warm = warmupTemplate == null ? (prefix() + raw("base-warmup")) : warmupTemplate;
            pl.sendMessage(CC.color(warm.replace("%seconds%", String.valueOf(pt.secondsLeft))));
            pt.secondsLeft--;
        }, 0L, 20L).getTaskId();

        pt.taskId = task;

        synchronized (pending) { pending.put(u, pt); }
    }

    /**
     * Start teleport with explicit warmup seconds and a custom template.
     */
    public void start(Player p, Location target, String finishMessage, String warmupTemplate, int warmupSeconds) {
        int warmup = warmupSeconds;
        if (warmup < 0) warmup = 0;

        UUID u = p.getUniqueId();

        synchronized (pending) {
            if (pending.containsKey(u)) {
                p.sendMessage(prefix() + raw("base-already-teleporting"));
                return;
            }
        }

        Location start = p.getLocation().clone(); // movement check uses blocks

        PendingTp pt = new PendingTp(target.clone(), start, warmup, finishMessage);

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player pl = Bukkit.getPlayer(u);
            if (pl == null || !pl.isOnline()) {
                cancel(u, false);
                return;
            }

            // if expired, teleport
            if (pt.secondsLeft <= 0) {
                cancel(u, false);
                pl.teleport(pt.target);
                // send the custom finish message (already contains colors/prefix if desired)
                pl.sendMessage(pt.finishMessage);
                return;
            }

            String warm = warmupTemplate == null ? (prefix() + raw("base-warmup")) : warmupTemplate;
            pl.sendMessage(CC.color(warm.replace("%seconds%", String.valueOf(pt.secondsLeft))));
            pt.secondsLeft--;
        }, 0L, 20L).getTaskId();

        pt.taskId = task;

        synchronized (pending) { pending.put(u, pt); }
    }

    public void cancel(UUID u, boolean moved) {
        PendingTp pt;
        synchronized (pending) { pt = pending.remove(u); }
        if (pt == null) return;

        Bukkit.getScheduler().cancelTask(pt.taskId);

        if (moved) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(prefix() + raw("base-cancelled-move"));
        }
    }

    /** Called from PlayerMoveEvent. Cancels if block changed. */
    public void handleMove(Player p, Location from, Location to) {
        if (to == null) return;

        PendingTp pt;
        synchronized (pending) { pt = pending.get(p.getUniqueId()); }
        if (pt == null) return;

        // cancel only on block change (so head movement doesn't cancel)
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancel(p.getUniqueId(), true);
        }
    }

    private static final class PendingTp {
        final Location target;
        final Location start;
        int secondsLeft;
        int taskId;
        final String finishMessage;

        PendingTp(Location target, Location start, int secondsLeft, String finishMessage) {
            this.target = target;
            this.start = start;
            this.secondsLeft = secondsLeft;
            this.finishMessage = finishMessage == null ? "" : finishMessage;
        }
    }
}
