package mc.pvpbulgaria.pvpbgclans.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatPromptManager {
    public interface Prompt {
        void onInput(Player p, String msg);
        void onCancel(Player p);
    }

    private final Plugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatPromptManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void begin(Player p, Prompt prompt) {
        prompts.put(p.getUniqueId(), prompt);
    }

    public boolean has(Player p) {
        return prompts.containsKey(p.getUniqueId());
    }

    /**
     * Cancels a prompt and runs the onCancel callback on the main server thread.
     */
    public void cancel(final Player p) {
        final Prompt pr = prompts.remove(p.getUniqueId());
        if (pr == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> pr.onCancel(p));
    }

    /**
     * Called from async listener; schedules the actual handler on main thread.
     */
    public void handle(final Player p, final String msg) {
        final Prompt pr = prompts.remove(p.getUniqueId());
        if (pr == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> pr.onInput(p, msg));
    }
}
