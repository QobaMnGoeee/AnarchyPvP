package kz.anarchy.pvp;

import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    // Combat
    private int combatDuration;

    // BossBar
    private boolean bossBarEnabled;
    private String bossBarText;
    private BarColor bossBarColor;

    // Messages
    private String msgPvpStart;
    private String msgPvpEnd;
    private String msgOpponentChanged;
    private String msgCommandBlocked;
    private String msgPortalBlocked;
    private String msgTeleportBlocked;
    private String msgLogoutBroadcast;

    // Allowed commands
    private List<String> allowedCommands;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        saveDefault();
        load();
    }

    private void saveDefault() {
        File folder = new File(plugin.getDataFolder().getParentFile(), "QobaMnPvP");
        if (!folder.exists()) folder.mkdirs();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream stream = plugin.getResource("config.yml")) {
                if (stream != null) {
                    Files.copy(stream, configFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("config.yml сақтау қатесі: " + e.getMessage());
            }
        }
    }

    private void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from resource
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Дефолт конфиг оқу қатесі: " + e.getMessage());
        }

        combatDuration    = config.getInt("combat.duration", 30);
        bossBarEnabled    = config.getBoolean("bossbar.enabled", true);
        bossBarText       = config.getString("bossbar.text", "⚔ PvP: {opponent} | {time}с қалды");
        bossBarColor      = parseBarColor(config.getString("bossbar.color", "RED"));

        msgPvpStart        = color(config.getString("messages.pvp-start",        "§eСіз §f{opponent}§e ойыншысымен PvP бастадыңыз."));
        msgPvpEnd          = color(config.getString("messages.pvp-end",          "§aСіз PvP режимінен шықтыңыз."));
        msgOpponentChanged = color(config.getString("messages.opponent-changed",  "§e⚔ PvP қарсыласыңыз ауысты: §f{opponent}"));
        msgCommandBlocked  = color(config.getString("messages.command-blocked",   "§c✗ §ePvP режимінде команда қолдану мүмкін емес!"));
        msgPortalBlocked   = color(config.getString("messages.portal-blocked",    "§c✗ §ePvP режимінде портал қолдану мүмкін емес!"));
        msgTeleportBlocked = color(config.getString("messages.teleport-blocked",  "§c✗ §ePvP режимінде телепорт мүмкін емес!"));
        msgLogoutBroadcast = color(config.getString("messages.logout-broadcast",  "§8[§cPvP§8] §f{player} §cPvP кезінде серверден шықты! §8(§f{killer} §8жеңді)"));

        allowedCommands = config.getStringList("allowed-commands");
    }

    private BarColor parseBarColor(String value) {
        try {
            return BarColor.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Қате bossbar color: " + value + ". RED қолданылады.");
            return BarColor.RED;
        }
    }

    /** Translates &-color codes to §-codes */
    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&0", "§0").replace("&1", "§1").replace("&2", "§2")
                   .replace("&3", "§3").replace("&4", "§4").replace("&5", "§5")
                   .replace("&6", "§6").replace("&7", "§7").replace("&8", "§8")
                   .replace("&9", "§9").replace("&a", "§a").replace("&b", "§b")
                   .replace("&c", "§c").replace("&d", "§d").replace("&e", "§e")
                   .replace("&f", "§f").replace("&l", "§l").replace("&m", "§m")
                   .replace("&n", "§n").replace("&o", "§o").replace("&r", "§r")
                   .replace("&k", "§k");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getCombatDuration()      { return combatDuration; }
    public boolean isBossBarEnabled()   { return bossBarEnabled; }
    public String getBossBarText()      { return bossBarText; }
    public BarColor getBossBarColor()   { return bossBarColor; }

    public String getMsgPvpStart()        { return msgPvpStart; }
    public String getMsgPvpEnd()          { return msgPvpEnd; }
    public String getMsgOpponentChanged() { return msgOpponentChanged; }
    public String getMsgCommandBlocked()  { return msgCommandBlocked; }
    public String getMsgPortalBlocked()   { return msgPortalBlocked; }
    public String getMsgTeleportBlocked() { return msgTeleportBlocked; }
    public String getMsgLogoutBroadcast() { return msgLogoutBroadcast; }

    public List<String> getAllowedCommands() { return allowedCommands; }
}
