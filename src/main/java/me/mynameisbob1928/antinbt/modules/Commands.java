package me.mynameisbob1928.antinbt.modules;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;
import me.mynameisbob1928.antinbt.AntiNbt.RawCommandInterface;

public class Commands implements Module, Listener {
	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, AntiNbt.instance);
	}

	@Override
	public void onDisable() {
		// Unregister events to prevent duplicate triggers or memory leaks
		HandlerList.unregisterAll(this);
	}

	@Override
	public Object invoke(String value, Object... args) {
		return null;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onCommand(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage().replaceFirst("/", "").trim();
		String[] args = command.split(" ");

		RawCommandInterface commandFunction = AntiNbt.commands.get(args[0]);
		if (commandFunction == null)
			return;

		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted command spoof from skript");
			return;
		}

		if (!event.getPlayer().getUniqueId().equals(AntiNbt.getUuid())) {
			return;
		}

		event.setCancelled(true);
		commandFunction.run(event, args);

	}
}
