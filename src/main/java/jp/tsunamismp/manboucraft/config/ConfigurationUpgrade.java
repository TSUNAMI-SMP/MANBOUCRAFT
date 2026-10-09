package jp.tsunamismp.manboucraft.config;

import jp.tsunamismp.manboucraft.death.DeathRule;
import jp.tsunamismp.manboucraft.death.RuleRegistry;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Upgrade only values equal to the old shipped defaults, preserving explicit customizations. */
public final class ConfigurationUpgrade {
    private ConfigurationUpgrade() {}
    private record Legacy(double probability, long interval) {}
    private static final Map<String, Legacy> LEGACY = Map.ofEntries(
            Map.entry("water_impact", new Legacy(0.2, 30)),
            Map.entry("parasite", new Legacy(0.05, 30)),
            Map.entry("fishing_shock", new Legacy(0.05, 30)),
            Map.entry("temperature_shock", new Legacy(0.05, 30)),
            Map.entry("diamond_joy", new Legacy(0.05, 30)),
            Map.entry("bed_neck", new Legacy(0.03, 30)),
            Map.entry("ender_pearl", new Legacy(0.1, 30)),
            Map.entry("sprinting", new Legacy(0.05, 30)),
            Map.entry("egg_shock", new Legacy(0.05, 30)),
            Map.entry("snowball_shock", new Legacy(0.05, 30)),
            Map.entry("door_shock", new Legacy(0.02, 30)),
            Map.entry("chest_anxiety", new Legacy(0.02, 30)),
            Map.entry("tool_break", new Legacy(0.1, 30)),
            Map.entry("flower_emotion", new Legacy(0.02, 30)),
            Map.entry("bell_shock", new Legacy(0.05, 30)),
            Map.entry("boat_sickness", new Legacy(0.03, 30)),
            Map.entry("rain_stress", new Legacy(0.01, 60)),
            Map.entry("thunder_fear", new Legacy(0.02, 60)),
            Map.entry("player_contact", new Legacy(0.02, 60)),
            Map.entry("villager_stare", new Legacy(0.02, 60)),
            Map.entry("heavy_bag", new Legacy(0.01, 60)),
            Map.entry("dark_fear", new Legacy(0.01, 60)),
            Map.entry("height_fear", new Legacy(0.01, 60)),
            Map.entry("deep_water", new Legacy(0.02, 60)),
            Map.entry("lava_fear", new Legacy(0.02, 60)),
            Map.entry("loneliness", new Legacy(0.005, 60)),
            Map.entry("hunger_despair", new Legacy(0.01, 60)),
            Map.entry("nether_panic", new Legacy(0.01, 60)),
            Map.entry("end_anxiety", new Legacy(0.01, 60)),
            Map.entry("chicken_stare", new Legacy(0.01, 60))
    );
    public static boolean upgrade(YamlConfiguration config) {
        if (config.getInt("config-version", 0) >= 2) return false;
        for (DeathRule rule : RuleRegistry.defaults().values()) {
            String root = "rules." + rule.id() + ".";
            Legacy previous = LEGACY.get(rule.id());
            if (!config.isSet(root + "probability") || (previous != null
                    && Double.compare(config.getDouble(root + "probability"), previous.probability()) == 0)) {
                config.set(root + "probability", rule.probability());
            }
            if (!config.isSet(root + "interval-seconds") || (previous != null
                    && config.getLong(root + "interval-seconds") == previous.interval())) {
                config.set(root + "interval-seconds", rule.intervalSeconds());
            }
            if (!config.isSet(root + "enabled")) config.set(root + "enabled", true);
            if (!config.isSet(root + "message")) config.set(root + "message", rule.message());
        }
        if (!config.isSet("respawn-protection-seconds") || config.getLong("respawn-protection-seconds") == 60)
            config.set("respawn-protection-seconds", 10);
        if (!config.isSet("calm-protection-while-sneaking")) config.set("calm-protection-while-sneaking", true);
        config.set("config-version", 2);
        return true;
    }
    public static boolean upgradeFile(Path file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        try { config.load(file.toFile()); }
        catch (org.bukkit.configuration.InvalidConfigurationException e) { throw new IOException("Invalid config.yml; original file preserved", e); }
        if (!upgrade(config)) return false;
        Path backups = file.getParent().resolve("config-backups");
        Files.createDirectories(backups);
        Path backup = Files.createTempFile(backups, "config-before-0.2.0-", ".yml");
        Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        Path temporary = file.resolveSibling("config.yml.upgrade.tmp");
        config.save(temporary.toFile());
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        return true;
    }
}
