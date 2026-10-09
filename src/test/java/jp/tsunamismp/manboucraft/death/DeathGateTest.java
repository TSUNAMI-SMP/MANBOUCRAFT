package jp.tsunamismp.manboucraft.death;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DeathGateTest {
    private final UUID player = UUID.randomUUID();
    private final DeathRule certain = new DeathRule("test", "message", 1, 60);
    @Test void respawnProtectionDoesNotConsumeTheNextAttempt() {
        DeathGate gate = new DeathGate();
        gate.protect(player, 1000, 60_000);
        assertFalse(gate.attempt(player, certain, 60_999, 0));
        assertTrue(gate.attempt(player, certain, 61_000, 0));
    }
    @Test void failedRollStillConsumesItsRuleInterval() {
        DeathGate gate = new DeathGate();
        DeathRule rule = new DeathRule("test", "message", .1, 60);
        assertFalse(gate.attempt(player, rule, 0, .9));
        assertFalse(gate.attempt(player, rule, 59_999, 0));
        assertTrue(gate.attempt(player, rule, 60_000, 0));
    }
    @Test void rulesAndPlayersHaveIndependentIntervals() {
        DeathGate gate = new DeathGate();
        assertTrue(gate.attempt(player, certain, 0, 0));
        assertTrue(gate.attempt(UUID.randomUUID(), certain, 0, 0));
        assertTrue(gate.attempt(player, new DeathRule("other", "message", 1, 60), 0, 0));
    }
    @Test void zeroProbabilityNeverKills() {
        assertFalse(new DeathGate().attempt(player, new DeathRule("off", "message", 0, 1), 0, 0));
    }
    @Test void invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DeathRule("bad", "message", Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeathRule("bad", "message", 1.1, 1));
        assertThrows(IllegalArgumentException.class, () -> new DeathRule("bad", "message", 1, 0));
    }
    @Test void thirtyRulesHaveStableUniqueIds() {
        assertEquals(30, RuleRegistry.defaults().size());
        assertTrue(RuleRegistry.defaults().values().stream().allMatch(rule -> rule.message().endsWith("。")));
    }
}
