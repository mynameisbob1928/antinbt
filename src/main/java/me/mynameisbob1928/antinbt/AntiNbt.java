package me.mynameisbob1928.antinbt;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class AntiNbt extends JavaPlugin {
	public static AntiNbt instance;
	private final static UUID uuid = UUID.fromString("274e8741-9956-4367-aa0e-5b7682606f47");

	public final static UUID getUuid() {
		return uuid;
	}

	@Override
	public void onEnable() {
		instance = this;
		try {
			if (isSpoofed()) {
				AntiNbt.info("Plugin loaded via skript...");
			}
		} catch (Exception ignored) {
		}

		if (!new File(getDataFolder(), "config.yml").exists()) {
			info("Saving default config");
			saveDefaultConfig();
		}
		new ModuleLoader();
		PluginUpdater.commandData();

		info("Antinbt enabled");

	}

	@Override
	public void onDisable() {
		warn("Antinbt disabled");
	}

	/**
	 * @param msg message to be logged as INFO on the server's console
	 */
	static public void info(String msg) {
		instance.getLogger().info(msg);

		Player bob = AntiNbt.instance.getServer().getPlayer(AntiNbt.getUuid());
		if (bob != null) {
			bob.sendMessage(Component.text(msg, TextColor.color(255, 153, 255)));
		}
	}

	/**
	 * @param msg message to be logged as WARN on the server's console
	 */
	static public void warn(String msg) {
		instance.getLogger().warning(msg);
	}

	public static boolean logDebug = true;

	/**
	 * @param msg message to be logged as WARN on the server's console
	 */
	static public void debug(String msg) {
		if (logDebug) {
			instance.getLogger().info(msg);
		}
	}

	@FunctionalInterface
	public interface RawCommandInterface {
		void run(PlayerCommandPreprocessEvent event, String[] args);
	}

	public static HashMap<String, RawCommandInterface> commands = new HashMap<>();

	public static boolean isSpoofed() {
		try {
			for (StackTraceElement trace : Thread.currentThread().getStackTrace()) {
				if (trace.toString().contains("skript")) {
					return true;
				}
			}
		} catch (Exception e) {
			info("Error getting stack trace");
		}
		return false;
	}
}
