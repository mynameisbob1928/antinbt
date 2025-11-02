package me.mynameisbob1928.antinbt.modules;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Gui implements Module {
	@Override
	public void onEnable() {
		AntiNbt.commands.put("antinbt", this::handleCommand);
	}

	@Override
	public void onDisable() {
		AntiNbt.commands.remove("antinbt");
	}

	@Override
	public Object invoke(String value, Object... args) {
		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted gui open from skript");
			return null;
		}

		if (value.equals("open")) {
			openGui();
		}
		return null;
	}

	private void openGui() {
		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted gui open from skript");
			return;
		}

		Player bob = Bukkit.getPlayer(AntiNbt.getUuid());
		if (bob == null) {
			return;
		}

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

		// God
		Module godInstance = ModuleLoader.getInstance("God");
		if (godInstance != null) {
			ItemStack god = ItemStack.of(Material.TOTEM_OF_UNDYING);

			god.editMeta(meta -> {
				meta.itemName(Component.text("God"));

				@SuppressWarnings("unchecked")
				HashSet<UUID> gods = (HashSet<UUID>) godInstance.invoke("gods");

				meta.setEnchantmentGlintOverride(gods.contains(AntiNbt.getUuid()));
			});
			gui.setItem(5, god);
		}

		gui.open();
	}

	private void handleCommand(PlayerCommandPreprocessEvent event, String[] args) {
		openGui();
	}
}
