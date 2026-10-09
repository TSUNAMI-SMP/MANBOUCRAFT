package jp.tsunamismp.manboucraft;

import jp.tsunamismp.manboucraft.death.DeathGate;
import jp.tsunamismp.manboucraft.death.DeathRule;
import jp.tsunamismp.manboucraft.death.RuleRegistry;
import jp.tsunamismp.manboucraft.statistics.DeathStatistics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ManbouCraftPlugin extends JavaPlugin implements Listener {
    private final Map<String, DeathRule> rules = new LinkedHashMap<>();
    private final Map<UUID, DeathRule> pending = new HashMap<>();
    private final DeathGate gate = new DeathGate();
    private DeathStatistics statistics;
    private RuleListener listener;
    private long protectionMillis;

    @Override public void onEnable() {
        saveDefaultConfig();
        long seconds = getConfig().getLong("respawn-protection-seconds", 60);
        if (seconds < 0 || seconds > 86400) throw new IllegalArgumentException("Invalid respawn protection");
        protectionMillis = seconds * 1000;
        for (DeathRule original : RuleRegistry.defaults().values()) {
            ConfigurationSection section = getConfig().getConfigurationSection("rules." + original.id());
            if (section == null || section.getBoolean("enabled", true)) {
                rules.put(original.id(), section == null ? original : new DeathRule(original.id(),
                        section.getString("message", original.message()),
                        section.getDouble("probability", original.probability()),
                        section.getLong("interval-seconds", original.intervalSeconds())));
            }
        }
        try { statistics = new DeathStatistics(getDataFolder().toPath().resolve("statistics.yml"), getLogger()); }
        catch (IOException e) { getLogger().log(java.util.logging.Level.SEVERE, "Statistics unavailable", e); Bukkit.getPluginManager().disablePlugin(this); return; }
        try { listener = new RuleListener(this); }
        catch (IOException e) { getLogger().log(java.util.logging.Level.SEVERE, "Rule state unavailable", e); Bukkit.getPluginManager().disablePlugin(this); return; }
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        MobListener mobs = new MobListener(this);
        Bukkit.getPluginManager().registerEvents(mobs, this);
        mobs.sweepLoaded();
        for (Player player : Bukkit.getOnlinePlayers()) protect(player);
        Bukkit.getScheduler().runTaskTimer(this, listener::tick, 20, 20);
        String grave = getConfig().getString("required-grave-plugin", "");
        if (grave.isBlank()) getLogger().warning("No grave provider name configured. ManbouCraft does not store items; configure required-grave-plugin to enforce its presence.");
        else if (!Bukkit.getPluginManager().isPluginEnabled(grave)) getLogger().warning("Special deaths paused until grave provider is enabled: " + grave);
        getLogger().info("Enabled " + rules.size() + " death rules; inventory and experience remain owned by your grave plugin.");
    }
    @Override public void onDisable() {
        if (statistics != null) statistics.save();
        if (listener != null) listener.saveState();
        pending.clear();
    }
    public boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && player.getHealth() > 0
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                && !pending.containsKey(player.getUniqueId());
    }
    private boolean graveAvailable() {
        String grave = getConfig().getString("required-grave-plugin", "");
        return grave.isBlank() || Bukkit.getPluginManager().isPluginEnabled(grave);
    }
    void fatalFall(Player player) {
        if (!eligible(player) || !graveAvailable() || !getConfig().getBoolean("fatal-fall-damage", true)) return;
        // This is physical fragility, not a random rule: no respawn shield or probability roll.
        executeDeath(player, fallRule());
    }
    private DeathRule fallRule() {
        return new DeathRule("fall_fragility",
                getConfig().getString("fatal-fall-message", "たった一段の油断で骨も心も折れた。"), 1, 1);
    }
    private DeathRule ruleForDeath(PlayerDeathEvent event) {
        DeathRule special = pending.get(event.getEntity().getUniqueId());
        if (special != null) return special;
        GameMode mode = event.getEntity().getGameMode();
        // A lethal vanilla fall finishes before the queued task. Attribute that death too.
        if ((mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE)
                && getConfig().getBoolean("fatal-fall-damage", true) && graveAvailable()
                && event.getDamageSource() != null
                && event.getDamageSource().getDamageType() == org.bukkit.damage.DamageType.FALL) return fallRule();
        return null;
    }
    public boolean attempt(Player player, String id) {
        DeathRule rule = rules.get(id);
        if (!graveAvailable()) return false;
        if (rule == null || !eligible(player)) return false;
        if (!gate.attempt(player.getUniqueId(), rule, now(), ThreadLocalRandom.current().nextDouble())) return false;
        executeDeath(player, rule);
        return true;
    }
    boolean protectedNow(Player player) { return gate.protectedAt(player.getUniqueId(), now()); }
    void executeDeath(Player player, DeathRule rule) {
        // Setting health to zero gives an ordinary PlayerDeathEvent. Intentional sudden deaths bypass totems.
        pending.put(player.getUniqueId(), rule);
        try { player.setHealth(0); }
        finally { pending.remove(player.getUniqueId()); }
        // A plugin cancelling the death must not leave stale cause information.
        if (!player.isDead()) gate.protect(player.getUniqueId(), now(), 10_000);
    }
    private static long now() { return System.nanoTime() / 1_000_000; }
    private void protect(Player player) { gate.protect(player.getUniqueId(), now(), protectionMillis); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void deathMessage(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        DeathRule rule = ruleForDeath(event);
        Component message = event.deathMessage();
        if (rule != null) {
            NamedTextColor[] colors = {NamedTextColor.LIGHT_PURPLE, NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.GOLD};
            NamedTextColor color = NamedTextColor.NAMES.value(getConfig().getString("message-color", "auto"));
            if (color == null) color = colors[Math.floorMod(rule.id().hashCode(), colors.length)];
            message = Component.text(event.getEntity().getName(), NamedTextColor.YELLOW)
                    .append(Component.text("は", NamedTextColor.WHITE))
                    .append(Component.text(rule.message(), color));
            event.deathMessage(message);
        }
        if (message == null) message = Component.text(event.getEntity().getName() + "は命の電池が切れた。", NamedTextColor.GRAY);
        event.deathScreenMessageOverride(Component.text("＿人人人人人人＿\n＞　突然の死　＜\n￣Y^Y^Y^Y^Y^Y^￣", NamedTextColor.RED)
                .append(Component.newline()).append(Component.newline()).append(message));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordDeath(PlayerDeathEvent event) {
        DeathRule rule = ruleForDeath(event);
        statistics.record(event.getEntity().getUniqueId(), event.getEntity().getName(), rule == null ? null : rule.id());
        if (listener != null) listener.clear(event.getEntity().getUniqueId());
        // Never touch drops, inventory, keepInventory or experience here.
    }
    @EventHandler public void join(PlayerJoinEvent event) { protect(event.getPlayer()); }
    @EventHandler public void respawn(PlayerRespawnEvent event) { protect(event.getPlayer()); listener.clear(event.getPlayer().getUniqueId()); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        gate.forget(id); pending.remove(id); listener.leave(id);
    }
}
