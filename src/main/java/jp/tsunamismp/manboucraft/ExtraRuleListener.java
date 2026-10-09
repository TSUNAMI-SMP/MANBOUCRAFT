package jp.tsunamismp.manboucraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;

/** Additional event adapters keep new death patterns separate from the original rules. */
public final class ExtraRuleListener implements Listener {
    private final ManbouCraftPlugin plugin;
    private static final Map<Material, String> FOOD_RULES = Map.ofEntries(
            Map.entry(Material.COOKED_BEEF, "hot_food"),
            Map.entry(Material.COOKED_PORKCHOP, "hot_food"),
            Map.entry(Material.COOKED_CHICKEN, "hot_food"),
            Map.entry(Material.COOKED_MUTTON, "hot_food"),
            Map.entry(Material.COOKED_RABBIT, "hot_food"),
            Map.entry(Material.COOKED_COD, "hot_food"),
            Map.entry(Material.COOKED_SALMON, "hot_food"),
            Map.entry(Material.COOKIE, "sweet_overload"),
            Map.entry(Material.HONEY_BOTTLE, "sweet_overload"),
            Map.entry(Material.POTION, "potion_confusion"),
            Map.entry(Material.ROTTEN_FLESH, "rotten_food"),
            Map.entry(Material.SUSPICIOUS_STEW, "suspicious_stew"),
            Map.entry(Material.MILK_BUCKET, "milk_shock"),
            Map.entry(Material.BREAD, "bread_choke")
    );
    public ExtraRuleListener(ManbouCraftPlugin plugin) { this.plugin = plugin; }
    private void later(Player player, String rule, Cancellable event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event == null || !event.isCancelled()) plugin.attempt(player, rule);
        });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent event) {
        String rule = FOOD_RULES.get(event.getItem().getType());
        if (rule != null) later(event.getPlayer(), rule, event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void shoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player && event.getBow() != null
                && (event.getBow().getType() == Material.BOW || event.getBow().getType() == Material.CROSSBOW))
            later(player, "bow_recoil", event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void shield(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && player.isBlocking()) later(player, "shield_shock", event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFinalDamage() <= 0) return;
        switch (event.getCause()) {
            case FIRE, FIRE_TICK, HOT_FLOOR, LAVA -> later(player, "burn_panic", event);
            case CONTACT -> later(player, "cactus_prick", event);
            default -> { }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bucket(PlayerBucketEmptyEvent event) { later(event.getPlayer(), "bucket_panic", event); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void craft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null
                && !event.getCurrentItem().getType().isAir()
                && event.getAction() != org.bukkit.event.inventory.InventoryAction.NOTHING)
            later(player, "craft_confusion", event);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void furnace(FurnaceExtractEvent event) {
        if (event.getItemAmount() > 0) later(event.getPlayer(), "furnace_heat", null);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void anvil(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() != null
                && event.getClickedInventory().getType() == InventoryType.ANVIL
                && event.getSlotType() == InventoryType.SlotType.RESULT
                && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()
                && event.getAction() != org.bukkit.event.inventory.InventoryAction.NOTHING)
            later(player, "anvil_stress", event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void fish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) later(event.getPlayer(), "fishing_treasure", event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) { later(event.getPlayer(), "item_drop", event); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) { later(event.getPlayer(), "hand_swap", event); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void sneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) later(event.getPlayer(), "sneak_cramp", event);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || event.getTo() == null || !plugin.eligible(event.getPlayer())) return;
        Player player = event.getPlayer();
        Location from = event.getFrom(), to = event.getTo();
        if (from.getWorld() != to.getWorld() || player.isFlying() || player.isGliding() || player.isInsideVehicle()) return;
        if (to.getY() - from.getY() > .2 && from.clone().subtract(0, .05, 0).getBlock().getType().isSolid())
            later(player, "jump_fear", event);
        if (!water(from.getBlock().getType()) && water(to.getBlock().getType())) later(player, "swim_shock", event);
    }
    private boolean water(Material material) { return material == Material.WATER || material == Material.BUBBLE_COLUMN; }
    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.eligible(player)) continue;
            boolean bee = false, wolf = false, cat = false;
            for (Entity entity : player.getNearbyEntities(5, 4, 5)) {
                if (entity.getLocation().distanceSquared(player.getLocation()) > 25) continue;
                if (entity instanceof Bee nearby && nearby.getAnger() > 0) bee = true;
                if (entity instanceof Wolf nearby && nearby.isAngry()) wolf = true;
                if (entity instanceof Cat) cat = true;
            }
            if (bee) plugin.attempt(player, "bee_fear");
            if (wolf) plugin.attempt(player, "wolf_fear");
            if (cat) plugin.attempt(player, "cat_judgment");
            if (player.getLocation().getBlock().getType() == Material.BUBBLE_COLUMN) plugin.attempt(player, "bubble_panic");
        }
    }
}
