package mc.pvpbulgaria.pvpbgclans.placeholders;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.clan.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClansPlaceholders extends PlaceholderExpansion {

    private final PvPBGClans plugin;

    public ClansPlaceholders(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Mentalkata, Ludakis";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "pvpbgclans";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(@NotNull Player player, @NotNull String identifier) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return "";

        if (identifier.equalsIgnoreCase("name")) {
            return clan.getName();
        }

        if (identifier.equalsIgnoreCase("role")) {
            return clan.getRole(player.getUniqueId()).name();
        }

        // keep compatibility: return formatted prefix for the clan according to level
        if (identifier.equalsIgnoreCase("prefix")) {
            return plugin.getClanManager().getClanPrefix(clan).replace("%clan%", clan.getName());
        }

        // new placeholder returns prefix template including the clan name explicitly
        if (identifier.equalsIgnoreCase("prefix_with_name")) {
            return plugin.getClanManager().getClanPrefix(clan);
        }

        return "";
    }
}
