package jp.tsunamismp.manboucraft;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExtraRuleListenerTest {
    public static class RecordingPlugin extends ManbouCraftPlugin {
        final List<String> attempts = new ArrayList<>();
        @Override public boolean attempt(org.bukkit.entity.Player player, String id) { attempts.add(id); return false; }
    }
    private ServerMock server;
    private RecordingPlugin plugin;
    private PlayerMock player;
    @BeforeEach void setup() { server = MockBukkit.mock(); plugin = MockBukkit.load(RecordingPlugin.class); player = server.addPlayer(); }
    @AfterEach void teardown() { MockBukkit.unmock(); }
    @Test void cookedFoodAndRottenFoodHaveDifferentCauses() {
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.COOKED_BEEF), org.bukkit.inventory.EquipmentSlot.HAND));
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.ROTTEN_FLESH), org.bukkit.inventory.EquipmentSlot.HAND));
        server.getScheduler().performOneTick();
        assertEquals(List.of("hot_food", "rotten_food"), plugin.attempts);
    }
    @Test void milkAndBreadTriggerTheirOwnPatterns() {
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.MILK_BUCKET), org.bukkit.inventory.EquipmentSlot.HAND));
        server.getPluginManager().callEvent(new PlayerItemConsumeEvent(player, new ItemStack(Material.BREAD), org.bukkit.inventory.EquipmentSlot.HAND));
        server.getScheduler().performOneTick();
        assertEquals(List.of("milk_shock", "bread_choke"), plugin.attempts);
    }
    @Test void cancelledConsumptionDoesNotTriggerAndLateCancellationAlsoWorks() {
        PlayerItemConsumeEvent cancelled = new PlayerItemConsumeEvent(player, new ItemStack(Material.COOKIE), org.bukkit.inventory.EquipmentSlot.HAND);
        cancelled.setCancelled(true);
        server.getPluginManager().callEvent(cancelled);
        PlayerItemConsumeEvent late = new PlayerItemConsumeEvent(player, new ItemStack(Material.POTION), org.bukkit.inventory.EquipmentSlot.HAND);
        server.getPluginManager().callEvent(late);
        late.setCancelled(true);
        server.getScheduler().performOneTick();
        assertTrue(plugin.attempts.isEmpty());
    }
    @Test void handSwapAndEnteringSneakTriggerButLeavingSneakDoesNot() {
        server.getPluginManager().callEvent(new PlayerSwapHandItemsEvent(player, new ItemStack(Material.STONE), new ItemStack(Material.DIRT)));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(player, true));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(player, false));
        server.getScheduler().performOneTick();
        assertEquals(List.of("hand_swap", "sneak_cramp"), plugin.attempts);
    }
}
