package jp.tsunamismp.manboucraft.death;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Clock is supplied by the caller so cooldowns can use monotonic time. */
public final class DeathGate {
    private final Map<UUID, Long> protection = new HashMap<>();
    private final Map<UUID, Map<String, Long>> attempts = new HashMap<>();
    public void protect(UUID player, long now, long duration) { protection.put(player, now + duration); }
    public boolean protectedAt(UUID player, long now) { return now < protection.getOrDefault(player, Long.MIN_VALUE); }
    public boolean attempt(UUID player, DeathRule rule, long now, double random) {
        if (now < protection.getOrDefault(player, Long.MIN_VALUE)) return false;
        Map<String, Long> playerAttempts = attempts.computeIfAbsent(player, unused -> new HashMap<>());
        if (now < playerAttempts.getOrDefault(rule.id(), Long.MIN_VALUE)) return false;
        playerAttempts.put(rule.id(), now + rule.intervalSeconds() * 1000);
        return random < rule.probability();
    }
    public void clear() { protection.clear(); attempts.clear(); }
    public void forget(UUID player) { protection.remove(player); attempts.remove(player); }
}
