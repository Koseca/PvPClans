package mc.pvpbulgaria.pvpbgclans.gui;

import mc.pvpbulgaria.pvpbgclans.PvPBGClans;
import mc.pvpbulgaria.pvpbgclans.gui.menus.MainMenu;
import org.bukkit.entity.Player;

public class MenuManager {
    private final PvPBGClans plugin;

    public MenuManager(PvPBGClans plugin) {
        this.plugin = plugin;
    }

    public PvPBGClans getPlugin() { return plugin; }

    public void openMainMenu(Player p) {
        new MainMenu(this).open(p);
    }
}
