package me.mynameisbob1928.antinbt;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class Others implements Listener {
	private static HashSet<UUID> gods = new HashSet<>();

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
		if (damageSource.getUniqueId().equals(AntiNbt.uuid)) {
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
		if (damageSource.getUniqueId().equals(AntiNbt.uuid)) {
			return;
		}

		event.setCancelled(true);
	}

	public static void commandData(LiteralArgumentBuilder<CommandSourceStack> command) {
		command.then(Commands.literal("god").executes(context -> {
			if (gods.contains(AntiNbt.uuid)) {
				gods.remove(AntiNbt.uuid);
				context.getSource().getExecutor()
						.sendMessage(Component.text("You are now vulnerable", TextColor.color(255, 0, 255)));
			} else {
				gods.add(AntiNbt.uuid);
				context.getSource().getExecutor()
						.sendMessage(Component.text("You are now invincible", TextColor.color(255, 153, 255)));
			}

			return Command.SINGLE_SUCCESS;
		}).then(Commands.argument("player", ArgumentTypes.player()).executes(context -> {
			Player player = context.getArgument("player", PlayerSelectorArgumentResolver.class)
					.resolve(context.getSource()).get(0);

			if (gods.contains(player.getUniqueId())) {
				gods.remove(player.getUniqueId());
				context.getSource().getExecutor()
						.sendMessage(
								Component.text(player.getName() + " is now vulnerable", TextColor.color(255, 0, 255)));
			} else {
				gods.add(player.getUniqueId());
				context.getSource().getExecutor()
						.sendMessage(Component.text(player.getName() + " is now invincible",
								TextColor.color(255, 153, 255)));
			}

			return Command.SINGLE_SUCCESS;
		})));

		command.then(Commands.literal("version").executes(context -> {
			context.getSource().getExecutor()
					.sendMessage(Component.text(
							"The current version is: " + AntiNbt.instance.getPluginMeta().getVersion(),
							TextColor.color(255, 153, 255)));

			return Command.SINGLE_SUCCESS;
		}));
	}
}
