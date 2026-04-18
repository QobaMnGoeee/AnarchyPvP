package kz.anarchy.pvp;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;

public class PvPListener implements Listener {

    /**
     * Бұл рұқсаты бар ойыншылар:
     *  1) PvP кезінде ойыннан шықса заттарын жоғалтпайды
     *  2) PvP кезінде барлық командаларды қолдана алады
     *  3) Олардың команда арқылы PvP ойыншыны телепорт ете алады
     */
    private static final String OWNER_PERM = "anarchypvp.owner.allow";

    private final PvPManager manager;

    public PvPListener(PvPManager manager) {
        this.manager = manager;
    }

    // ── Зақым тигізу → PvP тег ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        manager.enterPvP(attacker, victim);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Player p) return p;
        }
        return null;
    }

    // ── Ойыннан шығу → logout өңдеу ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;

        /*
         * FIX 1: anarchypvp.owner.allow рұқсаты бар ойыншы шықса
         *         заттары жерге түспейді (dropItems = false).
         *         Қалыпты ойыншыларда заттар түседі (dropItems = true).
         */
        boolean dropItems = !player.hasPermission(OWNER_PERM);
        manager.handleLogout(player, dropItems);
    }

    // ── Өлім → PvP аяқтау ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!manager.isInPvP(player)) return;

        Player killer = event.getEntity().getKiller();
        if (killer != null && killer.isOnline()) {
            String msg = Main.getInstance().getConfigManager().getMsgPvpEnd()
                    + " §7(§f" + killer.getName() + "§7 жеңді)";
            player.sendMessage(msg);
        }

        manager.exitPvP(player, true);
    }

    // ── Команда блоктау ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;

        /*
         * FIX 2: anarchypvp.owner.allow рұқсаты бар ойыншыларға
         *         PvP кезінде барлық командалар рұқсат етілген.
         */
        if (player.hasPermission(OWNER_PERM)) return;

        String cmdLower = event.getMessage().toLowerCase();
        for (String allowed : Main.getInstance().getConfigManager().getAllowedCommands()) {
            // Нақты команда немесе аргументпен бастауын тексеру
            if (cmdLower.equals(allowed.toLowerCase()) ||
                cmdLower.startsWith(allowed.toLowerCase() + " ")) {
                return;
            }
        }

        event.setCancelled(true);
        player.sendMessage(Main.getInstance().getConfigManager().getMsgCommandBlocked());
    }

    // ── Портал блоктау ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;
        if (player.hasPermission(OWNER_PERM)) return;

        event.setCancelled(true);
        player.sendMessage(Main.getInstance().getConfigManager().getMsgPortalBlocked());
    }

    // ── Телепорт блоктау ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInPvP(player)) return;
        if (player.hasPermission(OWNER_PERM)) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        /*
         * FIX 3: COMMAND және PLUGIN себептерін блоктамаймыз.
         *
         * Логика:
         *  - Егер ойыншының өзі команда терсе → onCommand оны бұрыннан блоктайды,
         *    сондықтан PlayerTeleportEvent мүлде іске қосылмайды.
         *  - Егер PlayerTeleportEvent(COMMAND/PLUGIN) іске қосылса, бұл дегеніміз
         *    ADMIN команда берді (мысалы /tp arman MEN) → рұқсат береміз.
         *
         * Тек портал арқылы өту блокталады:
         */
        switch (cause) {
            case NETHER_PORTAL:
            case END_PORTAL:
            case END_GATEWAY:
                event.setCancelled(true);
                player.sendMessage(Main.getInstance().getConfigManager().getMsgTeleportBlocked());
                break;
            default:
                // COMMAND, PLUGIN, UNKNOWN — рұқсат (admin телепорты)
                break;
        }
    }
}
