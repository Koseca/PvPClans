package mc.pvpbulgaria.pvpbgclans.utils;

import org.bukkit.ChatColor;

public final class CC {
    private CC() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
