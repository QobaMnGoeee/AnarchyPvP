package kz.anarchy.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PvPManager {

    private final Map<UUID, UUID> pvpPairs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Main plugin;

    public PvPManager(Main plugin) {
        this.plugin = plugin;
    }

    // ── PvP тег қою ──────────────────────────────────────────────────────────

    public void enterPvP(Player attacker, Player victim) {
        tagPlayer(attacker, victim);
        tagPlayer(victim, attacker);
    }

    private void tagPlayer(Player player, Player opponent) {
        UUID playerUUID   = player.getUniqueId();
        UUID opponentUUID = opponent.getUniqueId();
        ConfigManager cfg = plugin.getConfigManager();

        boolean isNew            = !pvpPairs.containsKey(playerUUID);
        boolean opponentChanged  = !isNew && !opponentUUID.equals(pvpPairs.get(playerUUID));

        pvpPairs.put(playerUUID, opponentUUID);
        cancelTask(playerUUID);
        startTimer(player, opponentUUID);

        if (isNew) {
            player.sendMessage(cfg.getMsgPvpStart().replace("{opponent}", opponent.getName()));
        } else if (opponentChanged) {
            player.sendMessage(cfg.getMsgOpponentChanged().replace("{opponent}", opponent.getName()));
        }
    }

    // ── Таймер & BossBar ─────────────────────────────────────────────────────

    private void cancelTask(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void startTimer(Player player, UUID opponentUUID) {
        ConfigManager cfg    = plugin.getConfigManager();
        int duration         = cfg.getCombatDuration();
        UUID playerUUID      = player.getUniqueId();

        removeBossBar(playerUUID, player);

        String opponentName = getOpponentName(opponentUUID);
        String template     = cfg.getBossBarText();

        BossBar bar = null;
        if (cfg.isBossBarEnabled()) {
            bar = Bukkit.createBossBar(
                    ConfigManager.color(formatBossBarTitle(template, opponentName, duration)),
                    cfg.getBossBarColor(),
                    BarStyle.SEGMENTED_10
            );
            bar.addPlayer(player);
            bossBars.put(playerUUID, bar);
        }

        final int[]   remaining = {duration};
        final BossBar finalBar  = bar;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                exitPvP(player, false);
                return;
            }
            if (finalBar != null) {
                UUID currentOpponentUUID = pvpPairs.get(playerUUID);
                String opName = getOpponentName(currentOpponentUUID);
                String title  = formatBossBarTitle(template, opName, remaining[0]);
                finalBar.setTitle(ConfigManager.color(title));
                finalBar.setProgress(Math.max(0.0, (double) remaining[0] / duration));
            }
        }, 20L, 20L);

        tasks.put(playerUUID, task);
    }

    private void removeBossBar(UUID uuid, Player player) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removePlayer(player);
            bar.removeAll();
        }
    }

    public String getOpponentName(UUID uuid) {
        if (uuid == null) return "Unknown";
        Player p = Bukkit.getPlayer(uuid);
        return p != null && p.isOnline() ? p.getName() : "Unknown";
    }

    private String formatBossBarTitle(String template, String opponentName, int time) {
        return template
                .replace("{opponent}", opponentName != null ? opponentName : "Unknown")
                .replace("{time}", String.valueOf(time));
    }

    // ── Logout өңдеу ─────────────────────────────────────────────────────────

    /**
     * PvP кезінде ойыннан шыққанда шақырылады.
     *
     * @param player    шыққан ойыншы
     * @param dropItems true  → заттар жерге түседі (қалыпты ойыншылар)
     *                  false → заттар сақталады   (anarchypvp.owner.allow рұқсаты барлар)
     */
    public void handleLogout(Player player, boolean dropItems) {
        UUID playerUUID   = player.getUniqueId();
        UUID opponentUUID = pvpPairs.get(playerUUID);
        String opponentName = getOpponentName(opponentUUID);

        if (dropItems) {
            dropInventory(player);
        }

        String broadcast = plugin.getConfigManager().getMsgLogoutBroadcast()
                .replace("{player}", player.getName())
                .replace("{killer}", opponentName);
        Bukkit.broadcastMessage(broadcast);

        exitPvP(player, true);
    }

    private void dropInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        Location loc  = player.getLocation();
        World world   = loc.getWorld();

        List<ItemStack> toDrop = new ArrayList<>();

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) toDrop.add(item.clone());
        }
        for (ItemStack item : inv.getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) toDrop.add(item.clone());
        }
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) toDrop.add(offhand.clone());

        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(new ItemStack(Material.AIR));

        for (ItemStack item : toDrop) {
            if (world != null) world.dropItemNaturally(loc, item);
        }
    }

    // ── PvP аяқтау ───────────────────────────────────────────────────────────

    public void exitPvP(Player player, boolean silent) {
        UUID playerUUID   = player.getUniqueId();
        UUID opponentUUID = pvpPairs.remove(playerUUID);
        cancelTask(playerUUID);
        removeBossBar(playerUUID, player);

        if (!silent) {
            player.sendMessage(plugin.getConfigManager().getMsgPvpEnd());
        }

        // Қарсыласты да PvP-дан шығарамыз
        if (opponentUUID != null && pvpPairs.containsKey(opponentUUID)) {
            Player opponent = Bukkit.getPlayer(opponentUUID);
            pvpPairs.remove(opponentUUID);
            cancelTask(opponentUUID);
            if (opponent != null && opponent.isOnline()) {
                removeBossBar(opponentUUID, opponent);
                if (!silent) {
                    opponent.sendMessage(plugin.getConfigManager().getMsgPvpEnd());
                }
            }
        }
    }

    public boolean isInPvP(Player player) {
        return pvpPairs.containsKey(player.getUniqueId());
    }

    public void clearAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        pvpPairs.clear();
    }
}
