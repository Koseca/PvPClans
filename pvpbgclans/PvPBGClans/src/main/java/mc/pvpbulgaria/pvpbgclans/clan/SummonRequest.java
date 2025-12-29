package mc.pvpbulgaria.pvpbgclans.clan;

import org.bukkit.Location;
import java.util.UUID;

public class SummonRequest {

    public final UUID summoner;
    public final Location location;
    public final long expiresAt;

    public SummonRequest(UUID summoner, Location location, long expiresAt) {
        this.summoner = summoner;
        this.location = location;
        this.expiresAt = expiresAt;
    }
}
