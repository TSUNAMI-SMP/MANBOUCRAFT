package jp.tsunamismp.manboucraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class PluginIntegrationTest {
    private ServerMock server;
    private ManbouCraftPlugin plugin;
    @BeforeEach void setup() { server = MockBukkit.mock(); plugin = MockBukkit.load(ManbouCraftPlugin.class); }
    @AfterEach void teardown() { MockBukkit.unmock(); }
    @Test void pluginLoadsAllFiftyFiveConfiguredRulesWithoutCommands() {
        assertTrue(plugin.isEnabled());
        assertEquals(55, plugin.getConfig().getConfigurationSection("rules").getKeys(false).size());
        assertTrue(plugin.getDescription().getCommands().isEmpty());
    }
    @Test void deathScreenAndStatisticsDoNotConsumeGraveDrops() {
        PlayerMock player = server.addPlayer("Manbou");
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.DIAMOND, 3));
        PlayerDeathEvent event = new PlayerDeathEvent(player, null, drops, 7, Component.text("vanilla"), true);
        server.getPluginManager().callEvent(event);
        assertEquals(3, event.getDrops().getFirst().getAmount());
        assertEquals(7, event.getDroppedExp());
        assertFalse(event.getKeepInventory());
        assertEquals(Component.text("vanilla"), event.deathMessage());
        assertTrue(PlainTextComponentSerializer.plainText().serialize(event.deathScreenMessageOverride()).contains("突然の死"));
        YamlConfiguration statistics = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "statistics.yml"));
        assertEquals(1, statistics.getLong("players." + player.getUniqueId() + ".total"));
        assertEquals(0, statistics.getLong("players." + player.getUniqueId() + ".special"));
    }
    @Test void cancelledDeathDoesNotRecordOrReplaceMessages() {
        PlayerMock player = server.addPlayer();
        PlayerDeathEvent event = new PlayerDeathEvent(player, null, new ArrayList<>(), 0, Component.text("original"), true);
        event.setCancelled(true);
        server.getPluginManager().callEvent(event);
        assertNull(event.deathScreenMessageOverride());
        assertFalse(new java.io.File(plugin.getDataFolder(), "statistics.yml").exists());
    }
    @Test void specialDeathHasOneColorfulLogAndRecordsOnlyItsCause() {
        PlayerMock player = server.addPlayer("Manbou");
        jp.tsunamismp.manboucraft.death.DeathRule rule = jp.tsunamismp.manboucraft.death.RuleRegistry.defaults().get("diamond_joy");
        java.util.concurrent.atomic.AtomicReference<PlayerDeathEvent> death = new java.util.concurrent.atomic.AtomicReference<>();
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
            public void observe(PlayerDeathEvent event) { death.set(event); }
        }, plugin);
        plugin.executeDeath(player, rule);
        assertNotNull(death.get());
        String text = PlainTextComponentSerializer.plainText().serialize(death.get().deathMessage());
        assertEquals("Manbouはダイヤの輝きに心臓が追いつかなかった。", text);
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.YELLOW, death.get().deathMessage().color());
        YamlConfiguration statistics = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "statistics.yml"));
        assertEquals(1, statistics.getLong("players." + player.getUniqueId() + ".special"));
        assertEquals(1, statistics.getLong("players." + player.getUniqueId() + ".causes.diamond_joy"));
    }
    @Test void blockedSpecialDeathLeavesNoStaleCause() {
        PlayerMock player = server.addPlayer();
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
            public void prevent(PlayerDeathEvent event) { event.setCancelled(true); event.setReviveHealth(10); }
        }, plugin);
        plugin.executeDeath(player, jp.tsunamismp.manboucraft.death.RuleRegistry.defaults().get("diamond_joy"));
        assertFalse(new java.io.File(plugin.getDataFolder(), "statistics.yml").exists());
        PlayerDeathEvent normal = new PlayerDeathEvent(player, null, new ArrayList<>(), 0, Component.text("normal"), true);
        plugin.deathMessage(normal);
        assertEquals(Component.text("normal"), normal.deathMessage());
    }
    @Test void mobSuppressionOnlyChangesOverworldHostiles() {
        org.bukkit.World normal = server.addSimpleWorld("normal");
        org.bukkit.World nether = server.addSimpleWorld("nether");
        ((org.mockbukkit.mockbukkit.world.WorldMock) nether).setEnvironment(org.bukkit.World.Environment.NETHER);
        org.bukkit.entity.Zombie zombie = (org.bukkit.entity.Zombie) normal.spawnEntity(new org.bukkit.Location(normal, 0, 64, 0), org.bukkit.entity.EntityType.ZOMBIE);
        org.bukkit.entity.Cow cow = (org.bukkit.entity.Cow) normal.spawnEntity(new org.bukkit.Location(normal, 0, 64, 0), org.bukkit.entity.EntityType.COW);
        org.bukkit.entity.Zombie netherZombie = (org.bukkit.entity.Zombie) nether.spawnEntity(new org.bukkit.Location(nether, 0, 64, 0), org.bukkit.entity.EntityType.ZOMBIE);
        org.bukkit.event.entity.CreatureSpawnEvent denied = new org.bukkit.event.entity.CreatureSpawnEvent(zombie, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER);
        org.bukkit.event.entity.CreatureSpawnEvent allowed = new org.bukkit.event.entity.CreatureSpawnEvent(cow, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
        org.bukkit.event.entity.CreatureSpawnEvent netherAllowed = new org.bukkit.event.entity.CreatureSpawnEvent(netherZombie, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);
        server.getPluginManager().callEvent(denied);
        server.getPluginManager().callEvent(allowed);
        server.getPluginManager().callEvent(netherAllowed);
        assertTrue(denied.isCancelled());
        assertFalse(allowed.isCancelled());
        assertFalse(netherAllowed.isCancelled());
    }
    private org.bukkit.event.entity.EntityDamageEvent fallEvent(PlayerMock player, double damage) {
        return new org.bukkit.event.entity.EntityDamageEvent(player,
                org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.FALL).build(), damage);
    }
    @Test void onePointOfFallDamageKillsEvenDuringJoinProtection() {
        PlayerMock player = server.addPlayer();
        server.getPluginManager().callEvent(fallEvent(player, 1));
        server.getScheduler().performOneTick();
        assertTrue(player.isDead());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "statistics.yml"));
        assertEquals(1, data.getLong("players." + player.getUniqueId() + ".causes.fall_fragility"));
    }
    @Test void alreadyLethalVanillaFallAlsoGetsSpecialMessageAndStatistics() {
        PlayerMock player = server.addPlayer("Manbou");
        org.bukkit.damage.DamageSource source = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.FALL).build();
        PlayerDeathEvent death = new PlayerDeathEvent(player, source, new ArrayList<>(), 0, Component.text("vanilla fall"), true);
        server.getPluginManager().callEvent(death);
        assertEquals("Manbouはたった一段の油断で骨も心も折れた。",
                PlainTextComponentSerializer.plainText().serialize(death.deathMessage()));
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "statistics.yml"));
        assertEquals(1, data.getLong("players." + player.getUniqueId() + ".causes.fall_fragility"));
    }
    @Test void fractionalFallDamageAlsoKills() {
        PlayerMock player = server.addPlayer();
        server.getPluginManager().callEvent(fallEvent(player, .01));
        server.getScheduler().performOneTick();
        assertTrue(player.isDead());
    }
    @Test void zeroOrCancelledFallDamageDoesNotKill() {
        PlayerMock player = server.addPlayer();
        server.getPluginManager().callEvent(fallEvent(player, 0));
        org.bukkit.event.entity.EntityDamageEvent cancelled = fallEvent(player, 1);
        cancelled.setCancelled(true);
        server.getPluginManager().callEvent(cancelled);
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
    }
    @Test void laterCancellationPreventsQueuedFallDeath() {
        PlayerMock player = server.addPlayer();
        org.bukkit.event.entity.EntityDamageEvent event = fallEvent(player, 1);
        server.getPluginManager().callEvent(event);
        event.setCancelled(true);
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
    }
    @Test void creativeAndDisabledFatalFallsDoNotKill() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        server.getPluginManager().callEvent(fallEvent(player, 1));
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        plugin.getConfig().set("fatal-fall-damage", false);
        server.getPluginManager().callEvent(fallEvent(player, 1));
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
    }
    @Test void missingRequiredGravePreventsFallDeath() {
        PlayerMock player = server.addPlayer();
        plugin.getConfig().set("required-grave-plugin", "MissingGrave");
        server.getPluginManager().callEvent(fallEvent(player, 1));
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
    }
    @Test void joinProtectionPreventsRuleAttempts() {
        PlayerMock player = server.addPlayer();
        assertFalse(plugin.attempt(player, "diamond_joy"));
        assertEquals(20, player.getHealth());
    }
}
