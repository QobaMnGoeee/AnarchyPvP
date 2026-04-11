package kz.anarchy.pvp;

import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConfigManager {

    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;

    // Кэшталған мәндер
    private int combatDuration;
    private boolean bossBarEnabled;
    private String bossBarText;
    private BarColor bossBarColor;
    private String msgPvpStart;
    private String msgPvpEnd;
    private String msgOpponentChanged;
    private String msgCommandBlocked;
    private String msgPortalBlocked;
    private String msgTeleportBlocked;
    private String msgLogoutBroadcast;
    private List<String> allowedCommands;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        // QobaMnPvP папкасы plugins/ ішінде жасалады
        File folder = new File(plugin.getDataFolder().getParentFile(), "QobaMnPvP");
        if (!folder.exists()) folder.mkdirs();
        this.configFile = new File(folder, "config.yml");
    }

    public void load() {
        // Егер config.yml болмаса, plugin ішіндегі default-ты жазамыз
        if (!configFile.exists()) {
            saveDefault();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Default мәндерді plugin resources-тен аламыз
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }

        // Мәндерді кэшке жазамыз
        combatDuration       = config.getInt("combat.duration", 30);
        bossBarEnabled       = config.getBoolean("bossbar.enabled", true);
        bossBarText          = config.getString("bossbar.text", "⚔ PvP: {opponent} | {time}с қалды");
        bossBarColor         = parseBarColor(config.getString("bossbar.color", "RED"));
        msgPvpStart          = color(config.getString("messages.pvp-start", "&eСіз PvP бастадыңыз."));
        msgPvpEnd            = color(config.getString("messages.pvp-end", "&aСіз PvP режимінен шықтыңыз."));
        msgOpponentChanged   = color(config.getString("messages.opponent-changed", "&e⚔ PvP қарсыласыңыз ауысты: &f{opponent}"));
        msgCommandBlocked    = color(config.getString("messages.command-blocked", "&c✗ &ePvP режимінде команда қолдану мүмкін емес!"));
        msgPortalBlocked     = color(config.getString("messages.portal-blocked", "&c✗ &ePvP режимінде портал қолдану мүмкін емес!"));
        msgTeleportBlocked   = color(config.getString("messages.teleport-blocked", "&c✗ &ePvP режимінде телепорт мүмкін емес!"));
        msgLogoutBroadcast   = color(config.getString("messages.logout-broadcast",
                "&8[&cPvP&8] &f{player} &cPvP кезінде серверден шықты! &8(&f{killer} &8жеңді)"));
        allowedCommands      = config.getStringList("allowed-commands");
    }

    private void saveDefault() {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return;
            java.nio.file.Files.copy(in, configFile.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("config.yml жазу кезінде қате: " + e.getMessage());
        }
    }

    private BarColor parseBarColor(String name) {
        try {
            return BarColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Белгісіз bossbar color: " + name + ". RED қолданылады.");
            return BarColor.RED;
        }
    }

    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    // ── Геттерлер ───────────────────────────────────────────────────────────

    public int getCombatDuration()       { return combatDuration; }
    public boolean isBossBarEnabled()    { return bossBarEnabled; }
    public String getBossBarText()       { return bossBarText; }
    public BarColor getBossBarColor()    { return bossBarColor; }
    public String getMsgPvpStart()       { return msgPvpStart; }
    public String getMsgPvpEnd()         { return msgPvpEnd; }
    public String getMsgOpponentChanged(){ return msgOpponentChanged; }
    public String getMsgCommandBlocked() { return msgCommandBlocked; }
    public String getMsgPortalBlocked()  { return msgPortalBlocked; }
    public String getMsgTeleportBlocked(){ return msgTeleportBlocked; }
    public String getMsgLogoutBroadcast(){ return msgLogoutBroadcast; }
    public List<String> getAllowedCommands() { return allowedCommands; }
}
