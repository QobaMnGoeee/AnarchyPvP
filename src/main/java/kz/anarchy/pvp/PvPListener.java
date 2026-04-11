package kz.anarchy.pvp;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PvPListener implements Listener {

    private final PvPManager manager;

    public PvPListener(PvPManager manager) {
        this.manager = manager;
    }

    // ── Зиян тигізу ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        manager.enterPvP(attacker, victim);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    // ── Серверден шығу ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isInPvP(player)) {
            manager.handleLogout(player);
        }
    }

    // ── Ойыншы өлімі ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!manager.isInPvP(player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer != null && killer.isOnline()) {
            killer.sendMessage(
                    ConfigManager.color("&aSіз §f" + player.getName() + " §aойыншысын жеңдіңіз!")
            );
        }

        manager.exitPvP(player, true);
    }

    // ── Команда тексеру ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;

        String cmdLower = event.getMessage().toLowerCase();
        for (String allowed : Main.getInstance().getConfigManager().getAllowedCommands()) {
            if (cmdLower.startsWith(allowed.toLowerCase())) return;
        }

        event.setCancelled(true);
        player.sendMessage(Main.getInstance().getConfigManager().getMsgCommandBlocked());
    }

    // ── Портал тексеру ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;

        event.setCancelled(true);
        player.sendMessage(Main.getInstance().getConfigManager().getMsgPortalBlocked());
    }

    // ── Телепорт тексеру ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND
                || cause == PlayerTeleportEvent.TeleportCause.PLUGIN
                || cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {

            event.setCancelled(true);
            player.sendMessage(Main.getInstance().getConfigManager().getMsgTeleportBlocked());
        }
    }
}
