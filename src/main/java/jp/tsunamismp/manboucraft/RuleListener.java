package jp.tsunamismp.manboucraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Event adapters and inexpensive, once-per-second environmental sampling. */
public final class RuleListener implements Listener {
    private final ManbouCraftPlugin plugin;
    private final Map<UUID, Double> runDistance = new HashMap<>();
    private final Map<UUID, Double> fallStart = new HashMap<>();
    private final Map<UUID, Integer> parasites = new HashMap<>();
    private int sampleCount;
    private final Map<UUID, Boolean> cold = new HashMap<>();
    private final Map<UUID, Long> contactSince = new HashMap<>();
    private final Map<UUID, Long> villagerSince = new HashMap<>();
    private final Map<UUID, Long> rainSince = new HashMap<>();
    // Only naturally generated ores can trigger joy; placed silk-touch ores do not.
    private final java.util.Set<String> placedOres = new java.util.HashSet<>();
    public RuleListener(ManbouCraftPlugin plugin) throws IOException {
        this.plugin = plugin;
        Path path = plugin.getDataFolder().toPath().resolve("rule-state.yml");
        if (Files.exists(path)) {
            YamlConfiguration state = new YamlConfiguration();
            try { state.load(path.toFile()); }
            catch (org.bukkit.configuration.InvalidConfigurationException e) { throw new IOException("Invalid rule state", e); }
            placedOres.addAll(state.getStringList("placed-diamond-ores"));
            org.bukkit.configuration.ConfigurationSection exposures = state.getConfigurationSection("parasites");
            if (exposures != null) for (String id : exposures.getKeys(false)) {
                try { parasites.put(UUID.fromString(id), Math.max(1, Math.min(90, exposures.getInt(id)))); }
                catch (IllegalArgumentException e) { throw new IOException("Invalid player UUID in rule state", e); }
            }
        }
    }
    public void saveState() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("placed-diamond-ores", new java.util.ArrayList<>(placedOres));
        parasites.forEach((id, seconds) -> state.set("parasites." + id, seconds));
        Path path = plugin.getDataFolder().toPath().resolve("rule-state.yml");
        Path temp = path.resolveSibling("rule-state.yml.tmp");
        try {
            Files.createDirectories(path.getParent());
            state.save(temp.toFile());
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save rule state", e); }
    }
    public void leave(UUID id) { clearMovement(id); saveState(); }
    private void clearMovement(UUID id) {
        runDistance.remove(id); fallStart.remove(id); cold.remove(id);
        contactSince.remove(id); villagerSince.remove(id); rainSince.remove(id);
    }
    public void clear(UUID id) {
        clearMovement(id);
        parasites.remove(id);
        saveState();
    }
    private void later(Player player, String rule) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.attempt(player, rule));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent event) {
        Material type = event.getItem().getType();
        if (type != Material.COD && type != Material.SALMON) return;
        Player player = event.getPlayer();
        if (!plugin.eligible(player) || parasites.containsKey(player.getUniqueId())) return;
        parasites.put(player.getUniqueId(), java.util.concurrent.ThreadLocalRandom.current().nextInt(30, 91));
        saveState();
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void fish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) return;
        for (Player player : event.getPlayer().getWorld().getPlayers()) {
            if (!player.equals(event.getPlayer()) && player.getLocation().distanceSquared(event.getPlayer().getLocation()) <= 64)
                later(player, "fishing_shock");
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        fallStart.remove(event.getPlayer().getUniqueId());
        runDistance.remove(event.getPlayer().getUniqueId());
        cold.remove(event.getPlayer().getUniqueId());
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) later(event.getPlayer(), "ender_pearl");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bed(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) later(event.getPlayer(), "bed_neck");
    }
    private static String key(Block block) { return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DIAMOND_ORE || event.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE) {
            placedOres.add(key(event.getBlock()));
            saveState();
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreak(BlockBreakEvent event) {
        boolean placed = placedOres.remove(key(event.getBlock()));
        if (placed) saveState();
        if (!placed && (event.getBlock().getType() == Material.DIAMOND_ORE || event.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE))
            later(event.getPlayer(), "diamond_joy");
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void tool(PlayerItemBreakEvent event) { later(event.getPlayer(), "tool_break"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) return;
        Block block = event.getClickedBlock();
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            if (block.getBlockData() instanceof Openable && block.getType().name().endsWith("_DOOR")) later(event.getPlayer(), "door_shock");
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void inventory(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && (event.getInventory().getHolder() instanceof org.bukkit.block.Chest || event.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest))
            later(player, "chest_anxiety");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void flower(BlockBreakEvent event) {
        if (org.bukkit.Tag.SMALL_FLOWERS.isTagged(event.getBlock().getType())) later(event.getPlayer(), "flower_emotion");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bell(BellRingEvent event) {
        for (Player player : event.getBlock().getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(event.getBlock().getLocation()) <= 100) later(player, "bell_shock");
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void vehicle(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player && event.getVehicle() instanceof Boat) later(player, "boat_sickness");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamager() instanceof Egg) later(player, "egg_shock");
        if (event.getDamager() instanceof Snowball) later(player, "snowball_shock");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event instanceof PlayerTeleportEvent || event.getTo() == null || !plugin.eligible(player)) return;
        Location from = event.getFrom(), to = event.getTo();
        if (from.getWorld() != to.getWorld()) return;
        UUID id = player.getUniqueId();
        double distance = from.distance(to);
        if (player.isSprinting() && !player.isInsideVehicle() && distance < 2) {
            double total = runDistance.getOrDefault(id, 0.0) + distance;
            if (total >= 300) { total = 0; later(player, "sprinting"); }
            runDistance.put(id, total);
        }
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) { fallStart.remove(id); return; }
        if (to.getY() < from.getY() && !player.isInWater()) fallStart.putIfAbsent(id, from.getY());
        if (player.isInWater()) {
            Double start = fallStart.remove(id);
            if (start != null && start - to.getY() >= 6) later(player, "water_impact");
        } else if (player.isOnGround()) fallStart.remove(id);
    }
    private boolean sustained(Map<UUID, Long> map, UUID id, boolean condition, long time, long duration) {
        if (!condition) { map.remove(id); return false; }
        return time - map.computeIfAbsent(id, unused -> time) >= duration;
    }
    public void tick() {
        long time = System.nanoTime() / 1_000_000;
        if (++sampleCount % 60 == 0) saveState();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.eligible(player)) continue;
            UUID exposureId = player.getUniqueId();
            if (!plugin.protectedNow(player) && parasites.containsKey(exposureId)) {
                int seconds = parasites.get(exposureId) - 1;
                if (seconds <= 0) { parasites.remove(exposureId); plugin.attempt(player, "parasite"); saveState(); }
                else parasites.put(exposureId, seconds);
            }
            Location loc = player.getLocation();
            World world = player.getWorld();
            UUID id = player.getUniqueId();
            boolean exposed = world.getHighestBlockYAt(loc) <= loc.getBlockY();
            boolean freezing = loc.getBlock().getTemperature() < .2;
            Boolean previousCold = cold.put(id, freezing);
            if (previousCold != null && previousCold != freezing) plugin.attempt(player, "temperature_shock");
            if (sustained(rainSince, id, world.hasStorm() && exposed && !freezing && world.getEnvironment() == World.Environment.NORMAL, time, 60_000))
                plugin.attempt(player, "rain_stress");
            if (world.isThundering() && exposed) plugin.attempt(player, "thunder_fear");
            boolean contact = false, villager = false, chicken = false;
            for (Entity nearby : player.getNearbyEntities(8, 4, 8)) {
                double squared = nearby.getLocation().distanceSquared(loc);
                if (nearby instanceof Player other && plugin.eligible(other) && squared < 1.44) contact = true;
                if (nearby instanceof Villager && squared < 16 && nearby.getLocation().getDirection().dot(loc.toVector().subtract(nearby.getLocation().toVector()).normalize()) > .8
                        && player.hasLineOfSight(nearby)) villager = true;
                if (nearby instanceof Chicken && squared < 9) chicken = true;
            }
            if (sustained(contactSince, id, contact, time, 2_000)) plugin.attempt(player, "player_contact");
            if (sustained(villagerSince, id, villager, time, 5_000)) plugin.attempt(player, "villager_stare");
            if (chicken) plugin.attempt(player, "chicken_stare");
            int occupied = 0;
            for (ItemStack item : player.getInventory().getStorageContents()) if (item != null && !item.getType().isAir()) occupied++;
            if (occupied >= 32) plugin.attempt(player, "heavy_bag");
            if (loc.getBlock().getLightLevel() <= 3) plugin.attempt(player, "dark_fear");
            if (loc.getY() >= 150 && exposed) plugin.attempt(player, "height_fear");
            if (player.isInWater() && loc.clone().subtract(0, 3, 0).getBlock().getType() == Material.WATER) plugin.attempt(player, "deep_water");
            if (nearLava(loc)) plugin.attempt(player, "lava_fear");
            boolean companion = world.getPlayers().stream().anyMatch(other -> !other.equals(player) && plugin.eligible(other) && other.getLocation().distanceSquared(loc) <= 32 * 32);
            if (!companion) plugin.attempt(player, "loneliness");
            if (player.getFoodLevel() <= 6) plugin.attempt(player, "hunger_despair");
            if (world.getEnvironment() == World.Environment.NETHER) plugin.attempt(player, "nether_panic");
            if (world.getEnvironment() == World.Environment.THE_END) plugin.attempt(player, "end_anxiety");
        }
    }
    private boolean nearLava(Location loc) {
        // 27 blocks, once per second; never force-load neighboring chunks.
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            int bx = loc.getBlockX() + x, bz = loc.getBlockZ() + z;
            if (loc.getWorld().isChunkLoaded(bx >> 4, bz >> 4)
                    && loc.getWorld().getBlockAt(bx, loc.getBlockY() + y, bz).getType() == Material.LAVA) return true;
        }
        return false;
    }
}
