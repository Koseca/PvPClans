package mc.pvpbulgaria.pvpbgclans.clan;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Clan {

    private final String name; // unique key
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final long createdAt;
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> trophies = new java.util.concurrent.ConcurrentHashMap<>();

    public Clan(String name, UUID leader, long createdAt) {
        this.name = name;
        this.leader = leader;
        this.createdAt = createdAt;

        // IMPORTANT: ensure leader is a member AND has 0 trophies
        addMember(leader);
    }

    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public long getCreatedAt() { return createdAt; }
    private Location base;
    public Location getBase() { return base; }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public ClanRole getRole(UUID uuid) {
        if (uuid != null && uuid.equals(leader)) return ClanRole.LEADER;
        return ClanRole.MEMBER;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
        kills.putIfAbsent(uuid, 0);
        trophies.putIfAbsent(uuid, 0);
    }

    public int size() {
        return members.size();
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        kills.remove(uuid);
        trophies.remove(uuid); // leaving/kick => remove trophies
    }

    public void transferLeadership(UUID newLeader) {
        if (newLeader == null) return;
        this.leader = newLeader;
        addMember(newLeader); // ensure still in members + has stats entry
    }

    // kills

    public int getKills(UUID uuid) {
        Integer k = kills.get(uuid);
        return k == null ? 0 : k;
    }

    public void setKills(UUID uuid, int value) {
        kills.put(uuid, Math.max(0, value));
    }

    public void addKill(UUID uuid) {
        kills.put(uuid, getKills(uuid) + 1);
    }

    public void resetStats(UUID uuid) {
        kills.remove(uuid); // joining a new clan => start from 0
        trophies.remove(uuid); // joining a new clan => start from 0
    }

    public Map<UUID, Integer> getKillsMap() {
        return kills;
    }

    // trophies
    public int getTrophies(UUID uuid) {
        Integer t = trophies.get(uuid);
        return t == null ? 0 : t;
    }

    public void setTrophies(UUID uuid, int value) {
        trophies.put(uuid, Math.max(0, value));
    }

    public void addTrophy(UUID uuid) {
        trophies.put(uuid, getTrophies(uuid) + 1);
    }

    public Map<UUID, Integer> getTrophiesMap() {
        return trophies;
    }

    // base
    public void setBase(org.bukkit.Location base) { this.base = base; }
}
