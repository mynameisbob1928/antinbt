package me.mynameisbob1928.antinbt.modules;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Version implements Module {
	@Override
	public void onEnable() {
		AntiNbt.commands.put("antinbtversion", this::handleCommand);
	}

	@Override
	public void onDisable() {
		AntiNbt.commands.remove("antinbtversion");
	}

	@Override
	public Object invoke(String value, Object... args) {
		return null;
	}

	private void handleCommand(PlayerCommandPreprocessEvent event, String[] args) {
		event.getPlayer()
				.sendMessage(Component.text("The current version is: " + AntiNbt.instance.getPluginMeta().getVersion(),
						TextColor.color(255, 153, 255)));
	}
}
