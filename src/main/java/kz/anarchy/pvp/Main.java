package kz.anarchy.pvp;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private PvPManager pvpManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config жүктеу
        configManager = new ConfigManager(this);
        configManager.load();

        // Manager және Listener инициализациясы
        pvpManager = new PvPManager(this);
        getServer().getPluginManager().registerEvents(new PvPListener(pvpManager), this);

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║     AnarchyPvP  ҚОСЫЛДЫ     ║");
        getLogger().info("╚══════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        pvpManager.clearAll();
        getLogger().info("AnarchyPvP өшірілді.");
    }

    public static Main getInstance()         { return instance; }
    public ConfigManager getConfigManager()  { return configManager; }
    public PvPManager getPvPManager()        { return pvpManager; }
}
