package me.mynameisbob1928.antinbt;

import java.util.Map;
import java.util.Set;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;

public class InventoryEvents implements Listener {
	@EventHandler
	public void ClickEvent(InventoryClickEvent event) {
		if (event.getWhoClicked().hasPermission("antinbt.bypass"))
			return;
		Bukkit.getScheduler().runTask(AntiNbt.instance, () -> checkInventory(event));
	}

	@EventHandler
	public void PickupEvent(EntityPickupItemEvent event) {
		if (event.getEntityType() != EntityType.PLAYER)
			return;
		if (event.getEntity().hasPermission("antinbt.bypass"))
			return;
		if (nbtPresent(event.getItem().getItemStack())) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}

	@EventHandler
	public void PlayerDropEvent(PlayerDropItemEvent event) {
		if (event.getPlayer().hasPermission("antinbt.bypass"))
			return;
		if (nbtPresent(event.getItemDrop().getItemStack())) {
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

			inv.getTopInventory().remove(item);
			inventoryEdited = true;
		}

		for (ItemStack item : inv.getBottomInventory().getStorageContents()) {
			if (item == null || item.isEmpty())
				continue;
			if (!nbtPresent(item))
				continue;

			player.getInventory().remove(item);
			inventoryEdited = true;
		}

		ItemStack[] armourContents = player.getInventory().getArmorContents();
		boolean armourEdited = false;

		for (int i = 0; i < armourContents.length; i++) {
			if (armourContents[i] == null)
				continue;
			if (nbtPresent(armourContents[i])) {
				armourContents[i] = null;
				armourEdited = true;
			}
		}
		if (armourEdited) {
			player.getInventory().setArmorContents(armourContents);
			inventoryEdited = true;
		}

		if (nbtPresent(player.getInventory().getItemInOffHand())) {
			player.getInventory().setItemInOffHand(null);
		}

		if (inventoryEdited) {
			player.updateInventory();
		}
	}

	private final Set<DataComponentType> nbtToIgnore = Set.of(
			DataComponentTypes.BANNER_PATTERNS, // Banner or shield patterns
			DataComponentTypes.BASE_COLOR, // Shield colour
			DataComponentTypes.CHARGED_PROJECTILES, // Crossbow projectiles
			DataComponentTypes.CONTAINER, // Container contents (this is manually checked)
			DataComponentTypes.CUSTOM_NAME, // Name set as if it were named by an anvil
			DataComponentTypes.DAMAGE, // Durability removed from an item
			DataComponentTypes.DYED_COLOR, // Dyed colour of a dyeable item (eg leather armour)
			DataComponentTypes.ENCHANTMENTS, // Item enchantments
			DataComponentTypes.INSTRUMENT, // Goat horn sound
			DataComponentTypes.ITEM_NAME, // Other way of naming item (is not italic)
			DataComponentTypes.LODESTONE_TRACKER,
			DataComponentTypes.LORE,
			DataComponentTypes.MAP_ID, // All (non-blank) maps have a map id
			DataComponentTypes.NOTE_BLOCK_SOUND, // Sound that plays when a player head on a noteblock (can be customised)
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

	private boolean nbtPresent(ItemStack item) {
		if (item == null)
			return false;

		if (Material.BUNDLE == item.getType()) { // completely block bundles
			return true;
		}

		ItemStack defaultItem = ItemStack.of(item.getType());

		if (!item.hasItemMeta()) {
			return !item.matchesWithoutData(defaultItem, nbtToIgnore, true); // Also put it here since perhaps some data might not be checked by item meta
		}

		ItemMeta meta = item.getItemMeta();

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

			if (state instanceof Container) { // Checks if containers (chests, barrels, dispensers, ect) have any items in them
				Inventory inv = ((Container) state).getInventory();
				for (ItemStack content : inv.getContents()) {
					if (content != null && !content.isEmpty()) {
						return true;
					}
				}
			}
		}

		for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) { // For any enchantments on an item, check if they are above their max set limit
			if (enchant.getKey().getMaxLevel() < enchant.getValue())
				return true;
		}

		return !item.matchesWithoutData(defaultItem, nbtToIgnore, true);
	}

		return false;
	}
}