package jp.tsunamismp.manboucraft;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ExtraRuleExecutionTest {
    private ServerMock server;
    private ManbouCraftPlugin plugin;
    @BeforeEach void setup() { server = MockBukkit.mock(); plugin = MockBukkit.load(ManbouCraftPlugin.class); }
    @AfterEach void teardown() { MockBukkit.unmock(); }
    private void only(String id) {
        server.getPluginManager().disablePlugin(plugin);
        for (String key : plugin.getConfig().getConfigurationSection("rules").getKeys(false))
            plugin.getConfig().set("rules." + key + ".enabled", key.equals(id));
        plugin.getConfig().set("rules." + id + ".probability", 1);
        plugin.getConfig().set("respawn-protection-seconds", 0);
        plugin.saveConfig();
        server.getPluginManager().enablePlugin(plugin);
    }
    private long deaths(PlayerMock player, String id) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "statistics.yml"));
        return data.getLong("players." + player.getUniqueId() + ".causes." + id);
    }
    @Test void eatingBreadActuallyKillsAndRecordsBreadCause() {
        only("bread_choke");
        PlayerMock player = server.addPlayer("Manbou");
        AtomicReference<PlayerDeathEvent> death = new AtomicReference<>();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observe(PlayerDeathEvent event) { death.set(event); }
        }, plugin);
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.BREAD), EquipmentSlot.HAND));
        server.getScheduler().performOneTick();
        assertTrue(player.isDead());
        assertEquals(1, deaths(player, "bread_choke"));
        assertTrue(PlainTextComponentSerializer.plainText().serialize(death.get().deathMessage()).startsWith("Manbouはパン"));
    }
    @Test void enteringWaterActuallyTriggersSwimShock() {
        only("swim_shock");
        PlayerMock player = server.addPlayer();
        org.bukkit.Location from = player.getLocation();
        org.bukkit.Location to = from.clone().add(1, 0, 0);
        to.getBlock().setType(Material.WATER);
        server.getPluginManager().callEvent(new PlayerMoveEvent(player, from, to));
        server.getScheduler().performOneTick();
        assertTrue(player.isDead());
        assertEquals(1, deaths(player, "swim_shock"));
    }
    @Test void jumpingFromSolidGroundActuallyTriggersFear() {
        only("jump_fear");
        PlayerMock player = server.addPlayer();
        org.bukkit.Location from = player.getLocation();
        from.setY(65);
        from.clone().subtract(0, 1, 0).getBlock().setType(Material.STONE);
        server.getPluginManager().callEvent(new PlayerMoveEvent(player, from, from.clone().add(0, .42, 0)));
        server.getScheduler().performOneTick();
        assertTrue(player.isDead());
        assertEquals(1, deaths(player, "jump_fear"));
    }
    @Test void crouchingReliablyPreventsGuaranteedStressDeath() {
        only("diamond_joy");
        PlayerMock player = server.addPlayer();
        player.setSneaking(true);
        assertFalse(plugin.attempt(player, "diamond_joy"));
        assertEquals(20, player.getHealth());
        // Protection must not consume a roll/cooldown: releasing crouch permits the same condition.
        player.setSneaking(false);
        assertTrue(plugin.attempt(player, "diamond_joy"));
        assertTrue(player.isDead());
    }
    @Test void crouchingDoesNotPreventPhysicalBurnDeath() {
        only("burn_panic");
        PlayerMock player = server.addPlayer();
        player.setSneaking(true);
        assertTrue(plugin.attempt(player, "burn_panic"));
        assertTrue(player.isDead());
    }
    @Test void calmProtectionCanBeDisabled() {
        only("diamond_joy");
        PlayerMock player = server.addPlayer();
        player.setSneaking(true);
        plugin.getConfig().set("calm-protection-while-sneaking", false);
        assertTrue(plugin.attempt(player, "diamond_joy"));
        assertTrue(player.isDead());
    }
    @Test void disabledPatternsStayDisabledAfterReenable() {
        only("bread_choke");
        PlayerMock player = server.addPlayer();
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.ROTTEN_FLESH), EquipmentSlot.HAND));
        server.getScheduler().performOneTick();
        assertEquals(20, player.getHealth());
        assertEquals(0, deaths(player, "rotten_food"));
    }
}
