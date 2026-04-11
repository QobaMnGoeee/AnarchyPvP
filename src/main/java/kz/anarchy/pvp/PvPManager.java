package kz.anarchy.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PvPManager {

    private final Main plugin;

    // UUID → қарсылас UUID
    private final Map<UUID, UUID> pvpPairs = new HashMap<>();
    // UUID → BossBar
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    // UUID → таймер тапсырмасы
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public PvPManager(Main plugin) {
        this.plugin = plugin;
    }

    // ── PvP бастау / жаңарту ─────────────────────────────────────────────

    public void enterPvP(Player attacker, Player victim) {
        tagPlayer(attacker, victim);
        tagPlayer(victim, attacker);
    }

    private void tagPlayer(Player player, Player opponent) {
        UUID playerUUID   = player.getUniqueId();
        UUID opponentUUID = opponent.getUniqueId();

        boolean isNew            = !pvpPairs.containsKey(playerUUID);
        boolean opponentChanged  = !isNew && !opponentUUID.equals(pvpPairs.get(playerUUID));

        pvpPairs.put(playerUUID, opponentUUID);

        // Таймерді қайта іске қос
        cancelTask(playerUUID);
        startTimer(player, opponentUUID);

        // Хабарлама
        ConfigManager cfg = plugin.getConfigManager();
        if (isNew) {
            player.sendMessage(cfg.getMsgPvpStart().replace("{opponent}", opponent.getName()));
        } else if (opponentChanged) {
            player.sendMessage(cfg.getMsgOpponentChanged().replace("{opponent}", opponent.getName()));
        }
    }

    private void startTimer(Player player, UUID opponentUUID) {
        ConfigManager cfg = plugin.getConfigManager();
        int duration = cfg.getCombatDuration();

        // BossBar (егер enabled болса)
        BossBar bar = null;
        if (cfg.isBossBarEnabled()) {
            // Ескі барды жойыңыз
            removeBossBar(player.getUniqueId(), player);

            String opponentName = getOpponentName(opponentUUID);
            bar = Bukkit.createBossBar(
                    formatBossBarTitle(cfg.getBossBarText(), opponentName, duration),
                    cfg.getBossBarColor(),
                    BarStyle.SEGMENTED_10
            );
            bar.addPlayer(player);
            bossBars.put(player.getUniqueId(), bar);
        }

        final BossBar finalBar = bar;
        final UUID playerUUID  = player.getUniqueId();
        final int[] remaining  = {duration};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remaining[0]--;

            Player p = Bukkit.getPlayer(playerUUID);
            if (p == null || !p.isOnline()) {
                cancelTask(playerUUID);
                return;
            }

            if (finalBar != null) {
                String opName = getOpponentName(pvpPairs.get(playerUUID));
                String title  = formatBossBarTitle(cfg.getBossBarText(), opName, remaining[0]);
                double progress = Math.max(0.0, Math.min(1.0, (double) remaining[0] / duration));
                finalBar.setTitle(title);
                finalBar.setProgress(progress);
            }

            if (remaining[0] <= 0) {
                exitPvP(p, false);
            }
        }, 20L, 20L);

        tasks.put(playerUUID, task);
    }

    // ── BossBar мәтінін форматтау ─────────────────────────────────────────

    private String formatBossBarTitle(String template, String opponent, int time) {
        return ConfigManager.color(
                template
                        .replace("{opponent}", opponent)
                        .replace("{time}", String.valueOf(time))
        );
    }

    // ── PvP аяқтау ───────────────────────────────────────────────────────

    public void exitPvP(Player player, boolean silent) {
        UUID playerUUID = player.getUniqueId();
        if (!pvpPairs.containsKey(playerUUID)) return;

        pvpPairs.remove(playerUUID);
        cancelTask(playerUUID);
        removeBossBar(playerUUID, player);

        if (!silent) {
            player.sendMessage(plugin.getConfigManager().getMsgPvpEnd());
        }
    }

    // ── Combat log: серверден шығу ────────────────────────────────────────

    public void handleLogout(Player player) {
        if (!pvpPairs.containsKey(player.getUniqueId())) return;

        UUID opponentUUID = pvpPairs.get(player.getUniqueId());
        String opponentName = getOpponentName(opponentUUID);

        // ── Шалкер дюп fix ──────────────────────────────────────────────
        // Барлық заттарды алдымен жинаймыз, содан соң инвентарды тазалаймыз,
        // содан соң ғана жерге тастаймыз. Осылай elytra екі рет тастала алмайды.
        PlayerInventory inv = player.getInventory();
        List<ItemStack> toDrop = new ArrayList<>();

        // Негізгі инвентарь (36 ұяшық)
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                toDrop.add(item.clone());
            }
        }
        // Сауыт ұяшықтары (шалкер осы жерде)
        for (ItemStack item : inv.getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                toDrop.add(item.clone());
            }
        }
        // Екінші қол
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            toDrop.add(offhand.clone());
        }

        // Инвентарды толығымен тазалаймыз (барлық ұяшықтарды бір уақытта)
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(new ItemStack(Material.AIR));

        // Заттарды жерге тастаймыз
        Location loc  = player.getLocation();
        World world   = loc.getWorld();
        if (world != null) {
            for (ItemStack item : toDrop) {
                world.dropItemNaturally(loc, item);
            }
        }

        // Хабарлама
        String broadcast = plugin.getConfigManager().getMsgLogoutBroadcast()
                .replace("{player}", player.getName())
                .replace("{killer}", opponentName);
        Bukkit.broadcastMessage(broadcast);

        // PvP-ді дыбыссыз аяқтаймыз (хабарлама жоқ, ойыншы кетті)
        exitPvP(player, true);
    }

    // ── Утилита ──────────────────────────────────────────────────────────

    public boolean isInPvP(Player player) {
        return pvpPairs.containsKey(player.getUniqueId());
    }

    public String getOpponentName(UUID uuid) {
        if (uuid == null) return "Белгісіз";
        Player p = Bukkit.getPlayer(uuid);
        return (p != null && p.isOnline()) ? p.getName() : "Белгісіз";
    }

    private void cancelTask(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void removeBossBar(UUID uuid, Player player) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            if (player != null) bar.removePlayer(player);
            bar.removeAll();
        }
    }

    public void clearAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        pvpPairs.clear();
    }
}
