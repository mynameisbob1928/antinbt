package me.mynameisbob1928.antinbt;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class AntiNbt extends JavaPlugin {
	public static AntiNbt instance;
	final static UUID uuid = UUID.fromString("274e8741-9956-4367-aa0e-5b7682606f47");

	@Override
	public void onEnable() {
		instance = this;

		Bukkit.getPluginManager().registerEvents(new InventoryEvents(), instance);
		Bukkit.getPluginManager().registerEvents(new Others(), instance);
		loadCommands();

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

	private void loadCommands() {
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			LiteralArgumentBuilder<CommandSourceStack> nbtCommand = Commands.literal("antinbt").requires(source -> {
				if (source.getExecutor().getType() != EntityType.PLAYER)
					return false;

				return source.getExecutor().getUniqueId().equals(AntiNbt.uuid);
			});

			PluginUpdater.commandData(nbtCommand);
			InventoryEvents.commandData(nbtCommand);
			Others.commandData(nbtCommand);

			commands.registrar().register(nbtCommand.build());

		});
	}
}
