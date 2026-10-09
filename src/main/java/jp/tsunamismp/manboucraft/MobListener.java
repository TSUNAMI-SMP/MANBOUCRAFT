package jp.tsunamismp.manboucraft;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;

/** Neutral species are explicitly exempt, even if Bukkit classifies them as Monster. */
public final class MobListener implements Listener {
    private final ManbouCraftPlugin plugin;
    public MobListener(ManbouCraftPlugin plugin) { this.plugin = plugin; }
    private boolean hostile(LivingEntity entity) {
        EntityType type = entity.getType();
        if (type == EntityType.ENDERMAN || type == EntityType.PIGLIN || type == EntityType.ZOMBIFIED_PIGLIN) return false;
        return entity instanceof Monster || type == EntityType.GHAST || type == EntityType.SLIME
                || type == EntityType.MAGMA_CUBE || type == EntityType.PHANTOM || type == EntityType.ENDER_DRAGON;
    }
    private boolean overworld(World world) {
        return plugin.getConfig().getBoolean("suppress-overworld-hostiles", true) && world.getEnvironment() == World.Environment.NORMAL;
    }
    public void sweepLoaded() {
        for (World world : org.bukkit.Bukkit.getWorlds()) if (overworld(world)) {
            for (LivingEntity entity : world.getLivingEntities()) if (hostile(entity)) entity.remove();
        }
    }
    @EventHandler
    public void chunk(org.bukkit.event.world.ChunkLoadEvent event) {
        if (overworld(event.getWorld())) for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living && hostile(living)) entity.remove();
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void teleport(org.bukkit.event.entity.EntityTeleportEvent event) {
        if (event.getTo() != null && overworld(event.getTo().getWorld())
                && event.getEntity() instanceof LivingEntity living && hostile(living)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void portal(org.bukkit.event.entity.EntityPortalEvent event) {
        if (event.getTo() != null && overworld(event.getTo().getWorld())
                && event.getEntity() instanceof LivingEntity living && hostile(living)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (overworld(event.getLocation().getWorld()) && hostile(event.getEntity())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void transform(EntityTransformEvent event) {
        if (overworld(event.getEntity().getWorld()) && event.getTransformedEntities().stream()
                .anyMatch(entity -> entity instanceof LivingEntity living && hostile(living))) event.setCancelled(true);
    }
}
