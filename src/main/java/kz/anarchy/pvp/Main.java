package kz.anarchy.pvp;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private PvPManager pvpManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        pvpManager = new PvPManager(this);
        getServer().getPluginManager().registerEvents(new PvPListener(pvpManager), this);
        getLogger().info("     AnarchyPvP  қосылды     ");
    }

    @Override
    public void onDisable() {
        pvpManager.clearAll();
        getLogger().info("AnarchyPvP өшірілді");
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PvPManager getPvpManager() {
        return pvpManager;
    }
}
