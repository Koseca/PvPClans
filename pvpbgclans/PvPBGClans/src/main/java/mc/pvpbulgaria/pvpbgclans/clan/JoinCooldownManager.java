package mc.pvpbulgaria.pvpbgclans.clan;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JoinCooldownManager {

    private final PvPBGClans plugin;
    private final File file;

    // player -> untilMillis
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public JoinCooldownManager(PvPBGClans plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
    }

    public void load() {
        cooldowns.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("cooldowns")) return;

        for (String uuidStr : cfg.getConfigurationSection("cooldowns").getKeys(false)) {
            try {
                UUID u = UUID.fromString(uuidStr);
                long until = cfg.getLong("cooldowns." + uuidStr, 0L);
                if (until > System.currentTimeMillis()) cooldowns.put(u, until);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> e : cooldowns.entrySet()) {
            if (e.getValue() > now) {
                out.set("cooldowns." + e.getKey().toString(), e.getValue());
            }
        }

        try { out.save(file); }
        catch (IOException e) {
            plugin.getLogger().severe("Failed to save cooldowns.yml: " + e.getMessage());
        }
    }

    public void setJoinCooldown(UUID player, int hours) {
        if (player == null) return;
        if (hours <= 0) return;

        long until = System.currentTimeMillis() + (hours * 3600_000L);
        cooldowns.put(player, until);
        save();
    }

    public boolean isOnCooldown(UUID player) {
        return getRemainingMillis(player) > 0;
    }

    public long getRemainingMillis(UUID player) {
        if (player == null) return 0;
        Long until = cooldowns.get(player);
        if (until == null) return 0;

        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            cooldowns.remove(player);
            save();
            return 0;
        }
        return left;
    }

    public static String formatTime(long millis) {
        long totalSec = Math.max(0, millis / 1000L);
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;

        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
