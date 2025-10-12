package me.mynameisbob1928.antinbt.modules;

import me.mynameisbob1928.antinbt.PluginUpdater;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;

public class ApplyUpdate implements Module {
	@Override
	public void onEnable() {
		PluginUpdater.commandData();
	}

	@Override
	public void onDisable() {
	}

	@Override
	public Object invoke(String value, Object... args) {
		return null;
	}
}
