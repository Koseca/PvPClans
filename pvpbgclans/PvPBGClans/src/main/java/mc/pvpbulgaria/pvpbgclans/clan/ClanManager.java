package mc.pvpbulgaria.pvpbgclans.clan;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import mc.pvpbulgaria.pvpbgclans.utils.CC;

public class ClanManager {

    private final PvPBGClans plugin;

    // clanName(lowercase) -> Clan
    private final Map<String, Clan> clansByName = new HashMap<>();
    // playerUuid -> clanName(lowercase)
    private final Map<UUID, String> clanByPlayer = new HashMap<>();

    // clan chat toggles (not persisted)
    private final Set<UUID> clanChatToggled = new HashSet<>();

    // disband confirmations (not persisted)
    private final Map<UUID, Long> disbandConfirmUntil = new HashMap<>();

    // invitations (not persisted)
    private final Map<UUID, Invite> invites = new HashMap<>();

    private final File clansFile;

    // async save batching
    private final Object saveLock = new Object();
    private int pendingSaveTaskId = -1;

    public ClanManager(PvPBGClans plugin) {
        this.plugin = plugin;
        this.clansFile = new File(plugin.getDataFolder(), "clans.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
    }

    /**
     * Returns the formatted clan prefix according to the clan's current level.
     * Reads templates from config path: clan.levels.prefixes (list). Index 0 = level 1.
     * Templates may contain %clan% which will be replaced with the clan name.
     * Falls back to messages.prefix or the raw clan name when not configured.
     */
    public String getClanPrefix(Clan clan) {
        if (clan == null) return "";
        List<String> prefixes = plugin.getConfig().getStringList("clan.levels.prefixes");
        if (prefixes == null || prefixes.isEmpty()) {
            String fallback = plugin.getConfig().getString("messages.prefix", "&7") + clan.getName();
            return CC.color(fallback);
        }

        int level = getClanLevel(clan);
        int idx = Math.max(0, Math.min(level - 1, prefixes.size() - 1));
        String tpl = prefixes.get(idx);
        if (tpl == null) tpl = plugin.getConfig().getString("messages.prefix", "&7") + clan.getName();
        return CC.color(tpl.replace("%clan%", clan.getName()));
    }

    /* -------------------- Load / Save -------------------- */

    public void loadAll() {
        synchronized (clansByName) {
            clansByName.clear();
            clanByPlayer.clear();
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(clansFile);
        if (!cfg.isConfigurationSection("clans")) return;

        for (String clanNameKey : cfg.getConfigurationSection("clans").getKeys(false)) {
            String path = "clans." + clanNameKey;

            String name = clanNameKey;
            String leaderStr = cfg.getString(path + ".leader");
            long createdAt = cfg.getLong(path + ".createdAt", System.currentTimeMillis());
            List<String> memberList = cfg.getStringList(path + ".members");

            if (leaderStr == null) continue;

            UUID leader;
            try { leader = UUID.fromString(leaderStr); }
            catch (Exception ex) { continue; }

            Clan clan = new Clan(name, leader, createdAt);
            // Clan constructor should add leader; still safe if it does
            clan.addMember(leader);

            for (String s : memberList) {
                try { clan.addMember(UUID.fromString(s)); }
                catch (Exception ignored) {}
            }

            // -------------------- STATS (TROPHIES) --------------------
            // Prefer new path: stats.trophies, fallback to old stats.kills
            String trophiesPath = path + ".stats.trophies";
            String killsPath = path + ".stats.kills";
            String readSection = cfg.isConfigurationSection(trophiesPath) ? trophiesPath : killsPath;

            if (cfg.isConfigurationSection(readSection)) {
                for (String uuidStr : cfg.getConfigurationSection(readSection).getKeys(false)) {
                    try {
                        UUID u = UUID.fromString(uuidStr);
                        int t = cfg.getInt(readSection + "." + uuidStr, 0);
                        clan.setTrophies(u, t);
                    } catch (Exception ignored) {}
                }
            }

            // ensure every current member has an entry
            for (UUID u : clan.getMembers()) {
                clan.setTrophies(u, clan.getTrophies(u));
            }

            // -------------------- BASE --------------------
            String b = path + ".base";
            if (cfg.isConfigurationSection(b)) {
                String worldName = cfg.getString(b + ".world");
                double x = cfg.getDouble(b + ".x");
                double y = cfg.getDouble(b + ".y");
                double z = cfg.getDouble(b + ".z");
                float yaw = (float) cfg.getDouble(b + ".yaw");
                float pitch = (float) cfg.getDouble(b + ".pitch");

                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    clan.setBase(new org.bukkit.Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
                }
            }

            String key = name.toLowerCase();
            synchronized (clansByName) {
                clansByName.put(key, clan);
                for (UUID u : clan.getMembers()) {
                    clanByPlayer.put(u, key);
                }
            }
        }
    }

    /** public helper for listeners (trophies, etc.) */
    public void markDirty() {
        queueSave();
    }

    private void queueSave() {
        synchronized (saveLock) {
            if (pendingSaveTaskId != -1) return;

            pendingSaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override public void run() {
                            saveAllToDisk();
                        }
                    });

                    synchronized (saveLock) {
                        pendingSaveTaskId = -1;
                    }
                }
            }, 40L).getTaskId(); // ~2s debounce
        }
    }

    /** Safe to call async; uses snapshots only (no Bukkit API). */
    private void saveAllToDisk() {
        Map<String, ClanSnapshot> snap = new HashMap<>();
        synchronized (clansByName) {
            for (Map.Entry<String, Clan> e : clansByName.entrySet()) {
                snap.put(e.getKey(), ClanSnapshot.from(e.getValue()));
            }
        }

        YamlConfiguration out = new YamlConfiguration();

        for (ClanSnapshot cs : snap.values()) {
            String base = "clans." + cs.name;

            out.set(base + ".leader", cs.leader.toString());
            out.set(base + ".createdAt", cs.createdAt);

            List<String> members = new ArrayList<>();
            for (UUID u : cs.members) members.add(u.toString());
            out.set(base + ".members", members);

            // stats: trophies (only for members)
            for (UUID u : cs.members) {
                out.set(base + ".stats.trophies." + u.toString(), cs.trophies.getOrDefault(u, 0));
            }

            // base
            if (cs.baseWorld != null) {
                out.set(base + ".base.world", cs.baseWorld);
                out.set(base + ".base.x", cs.baseX);
                out.set(base + ".base.y", cs.baseY);
                out.set(base + ".base.z", cs.baseZ);
                out.set(base + ".base.yaw", cs.baseYaw);
                out.set(base + ".base.pitch", cs.basePitch);
            } else {
                out.set(base + ".base", null);
            }
        }

        try {
            out.save(clansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save clans.yml: " + e.getMessage());
        }
    }

    public void shutdownAndFlushSave() {
        synchronized (saveLock) {
            if (pendingSaveTaskId != -1) {
                Bukkit.getScheduler().cancelTask(pendingSaveTaskId);
                pendingSaveTaskId = -1;
            }
        }
        saveAllToDisk(); // ok on shutdown
    }

    /* -------------------- Clan basics -------------------- */

    public boolean clanExists(String name) {
        return name != null && clansByName.containsKey(name.toLowerCase());
    }

    public Clan getClanByName(String name) {
        return name == null ? null : clansByName.get(name.toLowerCase());
    }

    public Clan getClanByPlayer(UUID uuid) {
        if (uuid == null) return null;
        String key;
        synchronized (clansByName) {
            key = clanByPlayer.get(uuid);
        }
        return key == null ? null : clansByName.get(key);
    }

    public boolean isInClan(UUID uuid) {
        return getClanByPlayer(uuid) != null;
    }

    public int getMaxMembers() {
        return plugin.getConfig().getInt("clan.max-members", 3);
    }

    public int getInviteExpireSeconds() {
        return plugin.getConfig().getInt("clan.invite-expire-seconds", 60);
    }

    public int getDisbandConfirmSeconds() {
        return plugin.getConfig().getInt("clan.disband-confirm-seconds", 10);
    }

    /* -------------------- Clan chat toggles -------------------- */

    public boolean isClanChatToggled(UUID uuid) {
        synchronized (clanChatToggled) {
            return clanChatToggled.contains(uuid);
        }
    }

    public boolean toggleClanChat(UUID uuid) {
        synchronized (clanChatToggled) {
            if (clanChatToggled.contains(uuid)) {
                clanChatToggled.remove(uuid);
                return false;
            }
            clanChatToggled.add(uuid);
            return true;
        }
    }

    public void clearClanChatToggles(Collection<UUID> members) {
        synchronized (clanChatToggled) {
            clanChatToggled.removeAll(members);
        }
    }

    /* -------------------- Disband confirm -------------------- */

    public void startDisbandConfirm(UUID leader) {
        long until = System.currentTimeMillis() + (getDisbandConfirmSeconds() * 1000L);
        synchronized (disbandConfirmUntil) {
            disbandConfirmUntil.put(leader, until);
        }
    }

    public boolean canConfirmDisband(UUID leader) {
        synchronized (disbandConfirmUntil) {
            Long until = disbandConfirmUntil.get(leader);
            if (until == null) return false;
            if (System.currentTimeMillis() > until) {
                disbandConfirmUntil.remove(leader);
                return false;
            }
            return true;
        }
    }

    public void clearDisbandConfirm(UUID leader) {
        synchronized (disbandConfirmUntil) {
            disbandConfirmUntil.remove(leader);
        }
    }

    /* -------------------- Create / Disband / Transfer -------------------- */

    public Clan createClan(String name, UUID leader) {
        String key = name.toLowerCase();
        Clan clan = new Clan(name, leader, System.currentTimeMillis());

        synchronized (clansByName) {
            clansByName.put(key, clan);
            for (UUID u : clan.getMembers()) clanByPlayer.put(u, key);
        }

        queueSave();
        return clan;
    }

    public void disbandClan(Clan clan) {
        if (clan == null) return;
        String key = clan.getName().toLowerCase();

        synchronized (clansByName) {
            for (UUID u : clan.getMembers()) clanByPlayer.remove(u);
            clansByName.remove(key);
        }

        // remove invites to this clan
        synchronized (invites) {
            Iterator<Map.Entry<UUID, Invite>> it = invites.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Invite> e = it.next();
                if (e.getValue().clanKey.equalsIgnoreCase(key)) it.remove();
            }
        }

        clearClanChatToggles(clan.getMembers());
        clearDisbandConfirm(clan.getLeader());

        queueSave();
    }

    public boolean transferLeadership(Clan clan, UUID currentLeader, UUID newLeader) {
        if (clan == null || currentLeader == null || newLeader == null) return false;
        if (!clan.getLeader().equals(currentLeader)) return false;
        if (!clan.isMember(newLeader)) return false;

        clan.transferLeadership(newLeader);

        // safety: cancel pending disband on transfer
        clearDisbandConfirm(currentLeader);

        queueSave();
        return true;
    }

    /* -------------------- Members (stats reset on join) -------------------- */

    public boolean addMember(Clan clan, UUID uuid) {
        if (clan == null || uuid == null) return false;
        if (clan.isMember(uuid)) return true;

        if (clan.size() >= getMaxMembers()) return false;

        String key = clan.getName().toLowerCase();
        synchronized (clansByName) {
            // IMPORTANT: reset stats when joining a clan
            clan.resetStats(uuid);
            clan.addMember(uuid);

            clanByPlayer.put(uuid, key);
        }

        queueSave();
        return true;
    }

    public void removeMember(Clan clan, UUID uuid) {
        if (clan == null || uuid == null) return;

        synchronized (clansByName) {
            clan.removeMember(uuid); // should also remove trophies in Clan.removeMember
            clanByPlayer.remove(uuid);
        }

        queueSave();
    }

    /* -------------------- Invites -------------------- */

    public boolean hasInvite(UUID target) {
        cleanupInvite(target);
        synchronized (invites) {
            return invites.containsKey(target);
        }
    }

    public Clan getPendingInviteClan(UUID player) {
        if (player == null) return null;
        cleanupInvite(player);

        Invite inv;
        synchronized (invites) {
            inv = invites.get(player);
        }
        if (inv == null) return null;

        return getClanByName(inv.clanKey); // clanKey is lowercase name
    }

    public String getPendingInviteClanName(UUID player) {
        Clan c = getPendingInviteClan(player);
        return c == null ? null : c.getName();
    }

    public void createInvite(UUID target, Clan clan) {
        long expiresAt = System.currentTimeMillis() + (getInviteExpireSeconds() * 1000L);
        synchronized (invites) {
            invites.put(target, new Invite(clan.getName().toLowerCase(), expiresAt));
        }
    }

    public boolean canJoin(UUID player, String clanName) {
        if (player == null || clanName == null) return false;
        cleanupInvite(player);

        String key = clanName.toLowerCase();
        synchronized (invites) {
            Invite inv = invites.get(player);
            return inv != null && inv.clanKey.equalsIgnoreCase(key);
        }
    }

    public void consumeInvite(UUID player) {
        synchronized (invites) {
            invites.remove(player);
        }
    }

    private void cleanupInvite(UUID player) {
        synchronized (invites) {
            Invite inv = invites.get(player);
            if (inv != null && System.currentTimeMillis() > inv.expiresAt) {
                invites.remove(player);
            }
        }
    }

    private static final class Invite {
        final String clanKey;
        final long expiresAt;
        Invite(String clanKey, long expiresAt) {
            this.clanKey = clanKey;
            this.expiresAt = expiresAt;
        }
    }

    /* -------------------- Trophies / Leveling / Ranking -------------------- */

    /** Set a player's trophy count (works for online/offline players who are members of the clan).
     *  Marks data dirty so it will be saved. */
    public void setPlayerTrophies(Clan clan, UUID player, int amount) {
        if (clan == null || player == null) return;
        synchronized (clansByName) {
            if (!clan.isMember(player)) return;
            clan.setTrophies(player, Math.max(0, amount));
        }
        markDirty();
    }

    /** Total trophies = sum of member trophies (1 kill = 1 trophy). */
    public int getClanTotalTrophies(Clan clan) {
        int total = 0;
        for (UUID u : clan.getMembers()) total += clan.getTrophies(u);
        return total;
    }

    /** Config list length = number of levels (max 10). Must be ascending. */
    public List<Integer> getLevelThresholds() {
        List<Integer> list = plugin.getConfig().getIntegerList("clan.levels.thresholds");
        if (list == null || list.isEmpty()) return Arrays.asList(0);
//        if (list.size() > 10) list = list.subList(0, 10);
        return list;
    }

    /** Returns level number (1..N). */
    public int getClanLevel(Clan clan) {
        int trophies = getClanTotalTrophies(clan);
        List<Integer> th = getLevelThresholds();

        int level = 1;
        for (int i = 0; i < th.size(); i++) {
            if (trophies >= th.get(i)) level = i + 1;
            else break;
        }
        return level;
    }

    /** Next level required trophies, or -1 if max level. */
    public int getNextLevelRequirement(Clan clan) {
        int level = getClanLevel(clan);
        List<Integer> th = getLevelThresholds();
        if (level >= th.size()) return -1;
        return th.get(level); // next threshold
    }

    /** Rank 1 = best (most trophies). */
    public int getClanRank(Clan clan) {
        List<Clan> all;
        synchronized (clansByName) {
            all = new ArrayList<>(clansByName.values());
        }

        all.sort((a, b) -> Integer.compare(getClanTotalTrophies(b), getClanTotalTrophies(a)));

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getName().equalsIgnoreCase(clan.getName())) return i + 1;
        }
        return -1;
    }

    public List<Clan> getTopClans(int limit) {
        List<Clan> all;
        synchronized (clansByName) {
            all = new ArrayList<>(clansByName.values());
        }

        all.sort((a, b) -> Integer.compare(getClanTotalTrophies(b), getClanTotalTrophies(a)));

        if (limit < 1) limit = 1;
        if (all.size() > limit) return all.subList(0, limit);
        return all;
    }

    /* -------------------- Snapshot -------------------- */

    private static final class ClanSnapshot {
        final String name;
        final UUID leader;
        final long createdAt;
        final Set<UUID> members;
        final Map<UUID, Integer> trophies;

        // base (primitive, async-safe)
        final String baseWorld;
        final double baseX, baseY, baseZ;
        final float baseYaw, basePitch;

        static ClanSnapshot from(Clan c) {
            Set<UUID> members = new HashSet<>(c.getMembers());
            Map<UUID, Integer> trophies = new HashMap<>();
            for (UUID u : members) trophies.put(u, c.getTrophies(u));

            String w = null;
            double x=0,y=0,z=0;
            float yaw=0,pitch=0;

            org.bukkit.Location loc = c.getBase();
            if (loc != null && loc.getWorld() != null) {
                w = loc.getWorld().getName();
                x = loc.getX(); y = loc.getY(); z = loc.getZ();
                yaw = loc.getYaw(); pitch = loc.getPitch();
            }

            return new ClanSnapshot(c.getName(), c.getLeader(), c.getCreatedAt(), members, trophies, w, x, y, z, yaw, pitch);
        }

        private ClanSnapshot(String name, UUID leader, long createdAt,
                             Set<UUID> members, Map<UUID,Integer> trophies,
                             String baseWorld, double baseX, double baseY, double baseZ, float baseYaw, float basePitch) {
            this.name = name;
            this.leader = leader;
            this.createdAt = createdAt;
            this.members = members;
            this.trophies = trophies;
            this.baseWorld = baseWorld;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
            this.baseYaw = baseYaw;
            this.basePitch = basePitch;
        }
    }
}
