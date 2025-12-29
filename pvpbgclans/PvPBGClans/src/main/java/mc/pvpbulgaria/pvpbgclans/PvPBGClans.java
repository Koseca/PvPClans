package mc.pvpbulgaria.pvpbgclans;

import mc.pvpbulgaria.pvpbgclans.clan.BaseTeleportManager;
import mc.pvpbulgaria.pvpbgclans.clan.ClanManager;
import mc.pvpbulgaria.pvpbgclans.clan.JoinCooldownManager;
import mc.pvpbulgaria.pvpbgclans.clan.ClanSummonManager;

import mc.pvpbulgaria.pvpbgclans.commands.ClanCommand;
import mc.pvpbulgaria.pvpbgclans.gui.ChatPromptListener;
import mc.pvpbulgaria.pvpbgclans.gui.ChatPromptManager;
import mc.pvpbulgaria.pvpbgclans.gui.GuiListener;
import mc.pvpbulgaria.pvpbgclans.gui.MenuManager;

import mc.pvpbulgaria.pvpbgclans.listeners.*;
import mc.pvpbulgaria.pvpbgclans.placeholders.ClansPlaceholders;
import mc.pvpbulgaria.pvpbgclans.listeners.EssentialsClanPlaceholderListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPBGClans extends JavaPlugin {

    private static PvPBGClans instance;

    private ClanManager clanManager;
    private JoinCooldownManager joinCooldowns;
    private BaseTeleportManager baseTeleportManager;
    private ClanSummonManager clanSummonManager;

    private MenuManager menuManager;
    private ChatPromptManager chatPrompts;

    /* ================= GETTERS ================= */

    public static PvPBGClans getInstance() {
        return instance;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public JoinCooldownManager getJoinCooldowns() {
        return joinCooldowns;
    }

    public BaseTeleportManager getBaseTeleportManager() {
        return baseTeleportManager;
    }

    public ClanSummonManager getClanSummonManager() {
        return clanSummonManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ChatPromptManager getChatPrompts() {
        return chatPrompts;
    }

    /* ================= LIFECYCLE ================= */

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Managers
        this.clanManager = new ClanManager(this);
        this.clanManager.loadAll();

        this.joinCooldowns = new JoinCooldownManager(this);
        this.joinCooldowns.load();

        this.baseTeleportManager = new BaseTeleportManager(this);
        this.clanSummonManager = new ClanSummonManager(this);

        // Commands
        ClanCommand clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);

        // GUI & prompts
        this.chatPrompts = new ChatPromptManager(this);
        this.menuManager = new MenuManager(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatPromptListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanKillsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanFriendlyFireListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BaseTeleportMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanSummonMoveListener(this), this);
        // Replace {CLAN} in Essentials format with level-based prefix (if used)
        Bukkit.getPluginManager().registerEvents(new EssentialsClanPlaceholderListener(this), this);

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClansPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        getLogger().info("PvPBGClans enabled.");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) {
            clanManager.shutdownAndFlushSave();
        }
        if (joinCooldowns != null) {
            joinCooldowns.save();
        }
        getLogger().info("PvPBGClans disabled.");
    }
}
