package me.mynameisbob1928.antinbt.modules;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;
import me.mynameisbob1928.antinbt.AntiNbt.RawCommandInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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

	private final String mainCommand = "antinbt";

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	private void onCommand(PlayerCommandPreprocessEvent event) {
		if (!event.getPlayer().getUniqueId().equals(AntiNbt.uuid)) {
			return;
		}

		String command = event.getMessage().replaceFirst("/", "").trim();
		String[] args = command.split(" ");

		if (args.length == 1 && args[0].equals(mainCommand)) {
			event.setCancelled(true);

			Player bob = event.getPlayer();

			InventoryView gui = MenuType.GENERIC_9X2.builder()
					.title(Component.text("Antinbt gui"))
					.build(bob);

			// Version
			ItemStack version = ItemStack.of(Material.PAPER);
			ItemMeta versionMeta = version.getItemMeta();
			versionMeta.itemName(Component.text("Plugin Version"));
			versionMeta.lore(List.of(Component.text("v" + AntiNbt.instance.getPluginMeta().getVersion(),
					TextColor.color(255, 153, 255))));
			version.setItemMeta(versionMeta);
			gui.setItem(3, version);

			// nbt debug
			Module inventoryEventsInstance = ModuleLoader.getInstance("InventoryEvents");
			if (inventoryEventsInstance != null) {
				ItemStack debug = ItemStack.of(Material.STICK);

				debug.editMeta(meta -> {
					meta.itemName(Component.text("Debug"));
					meta.setEnchantmentGlintOverride((Boolean) inventoryEventsInstance.invoke("logEvents"));
				});
				gui.setItem(4, debug);
			}

			// God
			Module godInstance = ModuleLoader.getInstance("God");
			if (godInstance != null) {
				ItemStack god = ItemStack.of(Material.TOTEM_OF_UNDYING);

				god.editMeta(meta -> {
					meta.itemName(Component.text("God"));

					@SuppressWarnings("unchecked")
					HashSet<UUID> gods = (HashSet<UUID>) godInstance.invoke("gods");

					meta.setEnchantmentGlintOverride(gods.contains(AntiNbt.uuid));
				});
				gui.setItem(5, god);
			}

			gui.open();

		} else {
			RawCommandInterface commandFunction = AntiNbt.commands.get(args[0]);
			if (commandFunction == null)
				return;

			event.setCancelled(true);
			commandFunction.run(event, args);
		}

	}
}
