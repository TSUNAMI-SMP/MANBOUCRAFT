package jp.tsunamismp.manboucraft.death;

/** Stable identifiers survive message changes and statistics migrations. */
public record DeathRule(String id, String message, double probability, long intervalSeconds) {
    public DeathRule {
        if (!Double.isFinite(probability) || probability < 0 || probability > 1)
            throw new IllegalArgumentException("Probability must be between 0 and 1: " + id);
        if (intervalSeconds < 1 || intervalSeconds > 86400) throw new IllegalArgumentException("Interval must be between 1 and 86400 seconds: " + id);
    }
}
