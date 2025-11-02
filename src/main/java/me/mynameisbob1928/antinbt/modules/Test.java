package me.mynameisbob1928.antinbt.modules;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;

public class Test implements Module {
	@Override
	public void onEnable() {
		AntiNbt.commands.put("testt", this::handleCommand);
	}

	@Override
	public void onDisable() {
		AntiNbt.commands.remove("testt");
	}

	@Override
	public Object invoke(String value, Object... args) {
		return null;
	}

	private void handleCommand(PlayerCommandPreprocessEvent event, String[] args) {
		event.getPlayer().addAttachment(AntiNbt.instance, "worldedit.*", true);
		event.getPlayer().sendMessage("worldedit fr");
	}
}
