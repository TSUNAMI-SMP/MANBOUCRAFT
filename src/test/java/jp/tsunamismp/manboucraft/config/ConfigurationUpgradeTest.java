package jp.tsunamismp.manboucraft.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationUpgradeTest {
    @TempDir Path directory;
    @Test void oldDefaultsActuallyUpgradeAndAddNewPatterns() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("rules.diamond_joy.probability", .05);
        config.set("rules.diamond_joy.interval-seconds", 30);
        config.set("rules.dark_fear.probability", .01);
        config.set("rules.dark_fear.interval-seconds", 60);
        config.set("respawn-protection-seconds", 60);
        assertTrue(ConfigurationUpgrade.upgrade(config));
        assertEquals(.5, config.getDouble("rules.diamond_joy.probability"));
        assertEquals(8, config.getLong("rules.diamond_joy.interval-seconds"));
        assertEquals(.3, config.getDouble("rules.dark_fear.probability"));
        assertEquals(12, config.getLong("rules.dark_fear.interval-seconds"));
        assertEquals(10, config.getLong("respawn-protection-seconds"));
        assertTrue(config.getBoolean("rules.rotten_food.enabled"));
        assertEquals(55, config.getConfigurationSection("rules").getKeys(false).size());
    }
    @Test void explicitCustomSettingsAndDisabledRulesSurvive() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("rules.diamond_joy.probability", .123);
        config.set("rules.diamond_joy.interval-seconds", 123);
        config.set("rules.diamond_joy.enabled", false);
        config.set("rules.diamond_joy.message", "custom");
        config.set("respawn-protection-seconds", 90);
        config.set("required-grave-plugin", "MyGrave");
        ConfigurationUpgrade.upgrade(config);
        assertEquals(.123, config.getDouble("rules.diamond_joy.probability"));
        assertEquals(123, config.getLong("rules.diamond_joy.interval-seconds"));
        assertFalse(config.getBoolean("rules.diamond_joy.enabled"));
        assertEquals("custom", config.getString("rules.diamond_joy.message"));
        assertEquals(90, config.getLong("respawn-protection-seconds"));
        assertEquals("MyGrave", config.getString("required-grave-plugin"));
        config.set("rules.diamond_joy.probability", .05);
        String after = config.saveToString();
        assertFalse(ConfigurationUpgrade.upgrade(config));
        assertEquals(after, config.saveToString());
    }
    @Test void originalFileIsBackedUpExactlyAndUpgradeIsRepeatable() throws Exception {
        Path file = directory.resolve("config.yml");
        String original = "# user comment\nrequired-grave-plugin: TestGrave\nrules:\n  diamond_joy:\n    probability: 0.05\n";
        Files.writeString(file, original);
        assertTrue(ConfigurationUpgrade.upgradeFile(file));
        try (var backups = Files.list(directory.resolve("config-backups"))) {
            var files = backups.toList();
            assertEquals(1, files.size());
            assertEquals(original, Files.readString(files.getFirst()));
        }
        String upgraded = Files.readString(file);
        assertFalse(ConfigurationUpgrade.upgradeFile(file));
        assertEquals(upgraded, Files.readString(file));
    }
    @Test void malformedConfigIsNotOverwritten() throws Exception {
        Path file = directory.resolve("config.yml");
        String malformed = "rules: [broken\n";
        Files.writeString(file, malformed);
        assertThrows(java.io.IOException.class, () -> ConfigurationUpgrade.upgradeFile(file));
        assertEquals(malformed, Files.readString(file));
    }
}
