package me.mynameisbob1928.antinbt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiNbt extends JavaPlugin {
	public static AntiNbt instance;

	@Override
	public void onEnable() {
		instance = this;

		Bukkit.getPluginManager().registerEvents(new InventoryEvents(), instance);
		PluginUpdater.loadUpdateCommand(getLifecycleManager());

		info("Antinbt enabled");

		Bukkit.getScheduler().runTask(this, () -> {
			// Run on first server tick then wait a bit more (30 seconds, 600 ticks) to run the update
			Bukkit.getScheduler().runTaskLater(this, () -> {
				PluginUpdater.update("50xOnTop");
			}, 100);
		});
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
}
