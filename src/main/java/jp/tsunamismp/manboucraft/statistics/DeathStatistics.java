package jp.tsunamismp.manboucraft.statistics;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

/** Small-server store: synchronous snapshot and atomic replacement on every death. */
public final class DeathStatistics {
    private final Path file;
    private final Logger logger;
    private final YamlConfiguration data;
    public DeathStatistics(Path file, Logger logger) throws IOException {
        this.file = file;
        this.logger = logger;
        data = new YamlConfiguration();
        if (Files.exists(file)) {
            try { data.load(file.toFile()); }
            catch (org.bukkit.configuration.InvalidConfigurationException e) {
                throw new IOException("Invalid statistics file; refusing to overwrite it", e);
            }
        }
    }
    public void record(UUID uuid, String name, String cause) {
        String root = "players." + uuid + ".";
        data.set(root + "name", name);
        data.set(root + "total", data.getLong(root + "total") + 1);
        if (cause != null) {
            data.set(root + "special", data.getLong(root + "special") + 1);
            String key = root + "causes." + cause;
            data.set(key, data.getLong(key) + 1);
        }
        save();
    }
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            data.save(temp.toFile());
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { logger.log(java.util.logging.Level.SEVERE, "Could not save death statistics", e); }
    }
}
