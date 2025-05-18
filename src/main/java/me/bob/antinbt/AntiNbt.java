package me.bob.antinbt;

import me.bob.antinbt.listeners.*;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiNbt extends JavaPlugin {
	public static AntiNbt instance;

	@Override
	public void onEnable() {
		instance = this;

		Bukkit.getPluginManager().registerEvents(new InventoryEvents(), instance);
		info("Chat I'm back");
	}

	@Override
	public void onDisable() {
		warn(":gone:");
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
