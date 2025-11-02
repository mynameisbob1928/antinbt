package me.mynameisbob1928.antinbt.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mynameisbob1928.antinbt.AntiNbt;
import me.mynameisbob1928.antinbt.ModuleLoader;
import me.mynameisbob1928.antinbt.ModuleLoader.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;

public class InventoryEvents implements Module, Listener {
	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, AntiNbt.instance);
		AntiNbt.commands.put("testnbt", this::handleCommand);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		AntiNbt.commands.remove("testnbt");
	}

	@Override
	public Object invoke(String value, Object... args) {
		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted logEvents access from skript");
			return null;
		}

		if (value.equals("logEvents")) {
			return logEvents;
		}

		return null;
	}

	private final Gson gson = new Gson();
	private boolean logEvents = false;
	private boolean testing = false;

	@EventHandler(priority = EventPriority.LOWEST)
	private void ClickEvent(InventoryClickEvent event) {
		HumanEntity player = event.getWhoClicked();

		if (event.getWhoClicked().getUniqueId().equals(AntiNbt.getUuid())) {
			if (event.getView() != null && event.getView().title().equals(Component.text("Antinbt gui"))) {

				ItemStack clickedItem = event.getCurrentItem();
				if (clickedItem != null) {

					if (clickedItem.getType() == Material.TOTEM_OF_UNDYING) {
						event.setCancelled(true);
						ItemStack item = event.getView().getItem(5);
						ItemMeta itemMeta = item.getItemMeta();
						if (itemMeta == null) {
							itemMeta = Bukkit.getItemFactory().getItemMeta(Material.TOTEM_OF_UNDYING);
						}

						Module godInstance = ModuleLoader.getInstance("God");
						if (godInstance == null) {
							player.sendMessage(
									Component.text("The God module is disabled", TextColor.color(255, 0, 0)));
							return;
						}

						@SuppressWarnings("unchecked")
						HashSet<UUID> gods = (HashSet<UUID>) godInstance.invoke("gods");

						if (gods.contains(AntiNbt.getUuid())) {
							gods.remove(AntiNbt.getUuid());
							player.sendMessage(Component.text("You are now vulnerable", TextColor.color(255, 0, 255)));

							itemMeta.setEnchantmentGlintOverride(false);
						} else {
							gods.add(AntiNbt.getUuid());
							player.sendMessage(
									Component.text("You are now invincible", TextColor.color(255, 153, 255)));

							itemMeta.setEnchantmentGlintOverride(true);
						}

						item.setItemMeta(itemMeta);
						event.getView().setItem(5, item);

					} else if (clickedItem.getType() == Material.STICK) {
						event.setCancelled(true);
						logEvents = !logEvents;

						ItemStack item = event.getView().getItem(4);
						ItemMeta itemMeta = item.getItemMeta();
						if (itemMeta == null) {
							itemMeta = Bukkit.getItemFactory().getItemMeta(Material.STICK);
						}
						itemMeta.setEnchantmentGlintOverride(logEvents);
						item.setItemMeta(itemMeta);
						event.getView().setItem(4, item);

						player.sendMessage(Component.text((logEvents ? "Started" : "Stopped") + " logging nbts",
								TextColor.color(255, 153, 255)));
					}
				}
				return;
			}
		}

		if (testing && player.getUniqueId().equals(AntiNbt.getUuid())) {
			// continue
		} else if (player.hasPermission("antinbt.bypass")) {
			return;
		}

		if (event.getWhoClicked().getWorld().getUID().equals(UUID.fromString("ba0a9c2b-67cb-4434-9dd6-db3fa9873371"))) // skip if player is in lobby so that the spawn items don't get cleared
			return;

		// The event is cancelled initially but then an inventory check is initiated to ensure that the item is removed and that there is nothing else in the players inventory

		// It is ran like this because the inventory is not updated by the time this event is fired, because of this the checkInventory method removed items only a tick later
		// which if something is done via a client, stuff can happen in the same tick so the player can bypass this by getting and using an item within the same tick
		if (nbtPresent(event.getCurrentItem())) {
			if (logEvents)
				nbtInfo(event.getCurrentItem(), player.getName());
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(AntiNbt.instance, () -> checkInventory(event));
		}
		if (nbtPresent(event.getCursor())) {
			if (logEvents)
				nbtInfo(event.getCursor(), player.getName());
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(AntiNbt.instance, () -> checkInventory(event));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void PickupEvent(EntityPickupItemEvent event) {
		if (event.getEntityType() != EntityType.PLAYER)
			return;
		if (event.getEntity().hasPermission("antinbt.bypass"))
			return;
		if (nbtPresent(event.getItem().getItemStack())) {
			if (logEvents)
				nbtInfo(event.getItem().getItemStack(), event.getEntity().getName());
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void PlayerDropEvent(PlayerDropItemEvent event) {
		if (event.getPlayer().hasPermission("antinbt.bypass"))
			return;
		if (nbtPresent(event.getItemDrop().getItemStack())) {
			if (logEvents)
				nbtInfo(event.getItemDrop().getItemStack(), event.getPlayer().getName());
			event.getItemDrop().remove();
		}
	}

	private void checkInventory(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		InventoryView inv = player.getOpenInventory();
		boolean inventoryEdited = false;

		for (ItemStack item : inv.getTopInventory().getContents()) {
			if (item == null || item.isEmpty())
				continue;
			if (!nbtPresent(item))
				continue;

			if (logEvents)
				nbtInfo(item, player.getName());
			inv.getTopInventory().remove(item);
			inventoryEdited = true;
		}

		for (ItemStack item : inv.getBottomInventory().getStorageContents()) {
			if (item == null || item.isEmpty())
				continue;
			if (!nbtPresent(item))
				continue;

			if (logEvents)
				nbtInfo(item, player.getName());
			player.getInventory().remove(item);
			inventoryEdited = true;
		}

		ItemStack[] armourContents = player.getInventory().getArmorContents();
		boolean armourEdited = false;

		for (int i = 0; i < armourContents.length; i++) {
			if (armourContents[i] == null)
				continue;
			if (nbtPresent(armourContents[i])) {
				if (logEvents)
					nbtInfo(armourContents[i], player.getName());
				armourContents[i] = null;
				armourEdited = true;
			}
		}
		if (armourEdited) {
			player.getInventory().setArmorContents(armourContents);
			inventoryEdited = true;
		}

		if (nbtPresent(player.getInventory().getItemInOffHand())) {
			if (logEvents)
				nbtInfo(player.getInventory().getItemInOffHand(), player.getName());
			player.getInventory().setItemInOffHand(null);
		}

		if (nbtPresent(player.getItemOnCursor())) {
			if (logEvents)
				nbtInfo(player.getItemOnCursor(), player.getName());
			player.setItemOnCursor(null);
		}

		if (inventoryEdited) {
			player.updateInventory();
		}
	}

	//#region
	private final Set<DataComponentType> nbtToIgnore = Set.of(
			DataComponentTypes.BANNER_PATTERNS, // Banner or shield patterns
			DataComponentTypes.BASE_COLOR, // Shield colour
			DataComponentTypes.CHARGED_PROJECTILES, // Crossbow projectiles
			DataComponentTypes.CONTAINER, // Container contents (this is manually checked)
			DataComponentTypes.CUSTOM_NAME, // Name set as if it were named by an anvil
			DataComponentTypes.DAMAGE, // Durability removed from an item
			DataComponentTypes.DYED_COLOR, // Dyed colour of a dyeable item (eg leather armour)
			DataComponentTypes.ENCHANTMENTS, // Item enchantments
			DataComponentTypes.FIREWORKS, // Fireworks explosions are prevented by some skript (I think), the duration is what is supposed to be whitelisted here
			DataComponentTypes.INSTRUMENT, // Goat horn sound
			DataComponentTypes.ITEM_NAME, // Other way of naming item (is not italic)
			DataComponentTypes.LODESTONE_TRACKER,
			DataComponentTypes.LORE,
			DataComponentTypes.MAP_ID, // All (non-blank) maps have a map id
			DataComponentTypes.NOTE_BLOCK_SOUND, // Sound that plays when a player head on a noteblock (can be customised)
			DataComponentTypes.POTION_CONTENTS, // This is handled more thoroughly elsewhere
			DataComponentTypes.PROFILE, // Player head data
			DataComponentTypes.REPAIR_COST,
			DataComponentTypes.STORED_ENCHANTMENTS, // Enchantments applied to an item but do not have any effect, like enchantments on an enchanted book
			DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, // Effects applied when consumed
			DataComponentTypes.TRIM, // Trims applied to armour
			DataComponentTypes.WRITABLE_BOOK_CONTENT, // Book and quill content
			DataComponentTypes.WRITTEN_BOOK_CONTENT, // Written book content

			// Entity variants
			DataComponentTypes.AXOLOTL_VARIANT,
			DataComponentTypes.CAT_VARIANT,
			DataComponentTypes.CHICKEN_VARIANT,
			DataComponentTypes.COW_VARIANT,
			DataComponentTypes.FOX_VARIANT,
			DataComponentTypes.FROG_VARIANT,
			DataComponentTypes.HORSE_VARIANT,
			DataComponentTypes.LLAMA_VARIANT,
			DataComponentTypes.PAINTING_VARIANT,
			DataComponentTypes.PARROT_VARIANT,
			DataComponentTypes.PIG_VARIANT,
			DataComponentTypes.RABBIT_VARIANT,
			DataComponentTypes.SALMON_SIZE,
			DataComponentTypes.SHEEP_COLOR,
			DataComponentTypes.SHULKER_COLOR,
			DataComponentTypes.TROPICAL_FISH_BASE_COLOR,
			DataComponentTypes.TROPICAL_FISH_PATTERN,
			DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR,
			DataComponentTypes.VILLAGER_VARIANT,
			DataComponentTypes.WOLF_COLLAR,
			DataComponentTypes.WOLF_SOUND_VARIANT,
			DataComponentTypes.WOLF_VARIANT

	);

	private final HashMap<String, Integer> allowedStewEffects = new HashMap<>(Map.of(
			"minecraft:saturation", 7,
			"minecraft:blindness", 220,
			"minecraft:nausea", 140,
			"minecraft:night_vision", 100,
			"minecraft:fire_resistance", 60,
			"minecraft:weakness", 140,
			"minecraft:regeneration", 140,
			"minecraft:jump_boost", 100,
			"minecraft:poison", 220,
			"minecraft:wither", 140

	));
	//#endregion

	private boolean nbtPresent(ItemStack item) {
		if (item == null)
			return false;

		// Check item size
		Map<String, Object> nbt = item.serialize();
		if (nbt.toString().length() > 30000) {
			return true;
		}

		if (Material.BUNDLE == item.getType()) { // completely block bundles
			return true;
		}

		ItemStack defaultItem = ItemStack.of(item.getType());

		if (!item.hasItemMeta()) {
			return !item.matchesWithoutData(defaultItem, nbtToIgnore, true); // Also put it here since perhaps some data might not be checked by item meta
		}

		ItemMeta meta = item.clone().getItemMeta();

		if (item.isDataOverridden(DataComponentTypes.CHARGED_PROJECTILES) // Ensure that crossbows are only charged with either a firework or an arrow
				&& meta instanceof CrossbowMeta crossbowMeta) {

			for (ItemStack projectile : crossbowMeta.getChargedProjectiles()) {
				if (projectile.getType() != Material.FIREWORK_ROCKET
						&& projectile.getType() != Material.ARROW
						&& projectile.getType() != Material.TIPPED_ARROW
						&& projectile.getType() != Material.SPECTRAL_ARROW) {
					return true;
				}
			}
		}

		if (meta instanceof BlockStateMeta) {
			BlockState state = ((BlockStateMeta) meta).getBlockState();
			// Checks the inside of containers, if that container has nbt or if it contains more containers itself then it is not allowed
			if (state instanceof Container) {
				Inventory inv = ((Container) state).getInventory();
				for (ItemStack content : inv.getContents()) {
					if (content == null || content.isEmpty()) {
						continue;
					}
					if (nbtPresent(content)) {
						return true;
					}
				}
			}
		}

		for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) { // For any enchantments on an item, check if they are above their max set limit
			if (enchant.getKey().getMaxLevel() < enchant.getValue())
				return true;
		}

		// nbt is defined earlier so that item size can be checked sooner
		// the custom_data tag needs to be checked in a special way so it's serialized as an nbt json string and then parsed into json where it can then be inspected properly
		if (nbt.containsKey("components")) {

			boolean componentsEdited = false;
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> components = (Map<String, Object>) nbt.get("components");

				if (components.containsKey("minecraft:custom_data")) {
					JsonObject customDataJson = gson.fromJson((String) components.get("minecraft:custom_data"),
							JsonObject.class);
					boolean changed = false;

					if (customDataJson.has("Skulls:ID")) { // allow skulls from /skulls
						customDataJson.remove("Skulls:ID");
						changed = true;
					}

					if (customDataJson.has("SongItemData")) { // allow items generated from the SongPlayer mod
						customDataJson.remove("SongItemData");
						changed = true;
					}

					if (changed) {
						if (customDataJson.isEmpty()) {
							components.remove("minecraft:custom_data");
							componentsEdited = true;
						} else {
							return true;
						}
					}
				}

				if (components.containsKey("minecraft:potion_contents")) { // Allow normal potions while blocking custom made potions
					String potionContents = (String) components.get("minecraft:potion_contents");

					if (potionContents.contains("custom_effects")) {
						return true;
					}
				}

				if (components.containsKey("minecraft:entity_data")) { // Allow invisible (glow) item frames

					if (item.getType() == Material.ITEM_FRAME) {

						JsonObject entityData = gson.fromJson((String) components.get("minecraft:entity_data"),
								JsonObject.class);

						if (entityData.has("id")) {
							if (!entityData.get("id").getAsString().equals("item_frame")
									&& !entityData.get("id").getAsString().equals("minecraft:item_frame")) {
								return true;
							}
							entityData.remove("id");
						}

						if (entityData.has("Invisible")) {
							entityData.remove("Invisible");
						}

						if (entityData.isEmpty()) {
							components.remove("minecraft:entity_data");
							componentsEdited = true;
						}

					} else if (item.getType() == Material.GLOW_ITEM_FRAME) {

						JsonObject entityData = gson.fromJson((String) components.get("minecraft:entity_data"),
								JsonObject.class);

						if (entityData.has("id")) {
							if (!entityData.get("id").getAsString().equals("glow_item_frame")
									&& !entityData.get("id").getAsString().equals("minecraft:glow_item_frame")) {
								return true;
							}
							entityData.remove("id");
						}

						if (entityData.has("Invisible")) {
							entityData.remove("Invisible");
						}

						if (entityData.isEmpty()) {
							components.remove("minecraft:entity_data");
							componentsEdited = true;
						}

					}

				}

				if (components.containsKey("minecraft:debug_stick_state") && item.getType() == Material.DEBUG_STICK) {
					// debug sticks hold states in the specific data type for a block it last edited so that needs to be allowed
					components.remove("minecraft:debug_stick_state");
					componentsEdited = true;
				}

				if (components.containsKey("minecraft:block_state")) { // Allow note blocks with states to be places (primarily for song players)
					JsonObject blockState = gson.fromJson((String) components.get("minecraft:block_state"),
							JsonObject.class);

					if (blockState.has("instrument")) {
						blockState.remove("instrument");
					}

					if (blockState.has("note")) {
						blockState.remove("note");
					}

					if (blockState.isEmpty()) {
						components.remove("minecraft:block_state");
						componentsEdited = true;
					}
				}

				// Check the any suspicous stew effects and only allow the pre-set ones that you obtainable via the creative menu
				if (components.containsKey("minecraft:suspicious_stew_effects")) {
					JsonArray stewEffects = gson.fromJson(
							(String) components.get("minecraft:suspicious_stew_effects"),
							JsonArray.class);

					if (stewEffects.size() != 1) {
						return true;
					}

					JsonElement effect = stewEffects.get(0);
					if (!effect.isJsonObject()) {
						return true;
					}

					JsonObject object = effect.getAsJsonObject();
					if (!object.has("duration") || !object.getAsJsonPrimitive("duration").isNumber()
							|| !object.has("id") || !object.getAsJsonPrimitive("id").isString()) {
						return true;
					}

					String effectType = object.get("id").getAsString();
					Integer duration = object.get("duration").getAsInt();

					if (!allowedStewEffects.containsKey(effectType)
							|| !allowedStewEffects.get(effectType).equals(duration)) {
						return true;
					}
				}

				if (componentsEdited) {
					ItemStack deserialisedItem = ItemStack.deserialize(nbt);
					return !deserialisedItem.matchesWithoutData(defaultItem, nbtToIgnore, true);
				}
			} catch (Exception e) {
				info("Exception while parsing nbt data, item below in blocked message: " + e.getMessage());
			}

		}

		return !item.matchesWithoutData(defaultItem, nbtToIgnore, true);
	}

	private void nbtInfo(ItemStack item, String playerName) {
		Player[] players = (Player[]) Bukkit.getOnlinePlayers().toArray();

		for (Player player : players) {
			if (player.getUniqueId().equals(AntiNbt.getUuid()) || player.hasPermission("antinbt.nbtlog")) {
				player.sendMessage(Component
						.text("Blocked nbt item from " + playerName + ": ", TextColor.color(255, 153, 255)).append(item
								.displayName().hoverEvent(item.asHoverEvent()).clickEvent(ClickEvent.callback(event -> {
									player.give(item);
								}))));
			}
		}
	}

	private void info(String message) {
		Player bob = AntiNbt.instance.getServer().getPlayer(AntiNbt.getUuid());
		if (bob != null) {
			bob.sendMessage(Component.text(message, TextColor.color(255, 153, 255)));
		}
	}

	public void commandData(LiteralArgumentBuilder<CommandSourceStack> command) {
		command.then(Commands.literal("debug").executes(context -> {
			if (logEvents) {
				logEvents = false;
				context.getSource().getExecutor()
						.sendMessage(Component.text("Stopped logging nbts", TextColor.color(255, 153, 255)));
			} else {
				logEvents = true;
				context.getSource().getExecutor()
						.sendMessage(Component.text("Started logging nbts", TextColor.color(255, 153, 255)));
			}

			return Command.SINGLE_SUCCESS;
		}));
	}

	private void handleCommand(PlayerCommandPreprocessEvent event, String[] args) {
		if (AntiNbt.isSpoofed()) {
			AntiNbt.info("Attempted nbt testing handler spoof from skript");
			return;
		}

		testing = !testing;
		if (testing) {
			event.getPlayer().sendMessage(
					Component.text("You are now in testing mode, NBT's will be blocked",
							TextColor.color(255, 153, 255)));
		} else {
			event.getPlayer().sendMessage(
					Component.text("Stopped testing mode",
							TextColor.color(255, 153, 255)));
		}
	}
}
