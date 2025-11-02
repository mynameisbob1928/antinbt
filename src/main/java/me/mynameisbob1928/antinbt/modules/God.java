package me.mynameisbob1928.antinbt.modules;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;

public class God implements Module, Listener {
	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, AntiNbt.instance);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public Object invoke(String value, Object... args) {
		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted god access from skript");
			return null;
		}

		if (value.equals("gods")) {
			return gods;
		}

		return null;
	}

	private HashSet<UUID> gods = new HashSet<>();

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onDamage(EntityDamageEvent event) {
		if (!gods.contains(event.getEntity().getUniqueId())) {
			return;
		}

		Entity damageSource = event.getDamageSource().getCausingEntity();
		if (damageSource == null) {
			event.setCancelled(true);
			return;
		}
		if (damageSource.getUniqueId().equals(AntiNbt.getUuid())) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onDeath(PlayerDeathEvent event) {
		if (!gods.contains(event.getPlayer().getUniqueId())) {
			return;
		}

		Entity damageSource = event.getDamageSource().getCausingEntity();
		if (damageSource == null) {
			event.setCancelled(true);
			return;
		}
		if (damageSource.getUniqueId().equals(AntiNbt.getUuid())) {
			return;
		}

		event.setCancelled(true);
	}
}
