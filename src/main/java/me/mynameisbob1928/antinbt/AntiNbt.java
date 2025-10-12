package me.mynameisbob1928.antinbt;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiNbt extends JavaPlugin {
	public static AntiNbt instance;
	public final static UUID uuid = UUID.fromString("274e8741-9956-4367-aa0e-5b7682606f47");

	@Override
	public void onEnable() {
		instance = this;

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
	}

	/**
	 * @param msg message to be logged as WARN on the server's console
	 */
	static public void warn(String msg) {
		instance.getLogger().warning(msg);
	}

	@FunctionalInterface
	public interface RawCommandInterface {
		void run(PlayerCommandPreprocessEvent event, String[] args);
	}

	public static HashMap<String, RawCommandInterface> commands = new HashMap<>();
}
