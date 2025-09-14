package me.mynameisbob1928.antinbt;

import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

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

	private boolean nbtPresent(ItemStack item) {
		if (item == null)
			return false;
		if (!item.hasItemMeta()) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();

		if (meta instanceof BlockStateMeta) {
			BlockState state = ((BlockStateMeta) meta).getBlockState();

			if (state instanceof Container) {
				Inventory inv = ((Container) state).getInventory();
				for (ItemStack content : inv.getContents()) {
					if (content != null && !content.isEmpty()) {
						return true;
					}
				}
			}
		}

		if (meta instanceof BundleMeta) {
			BundleMeta bundleMeta = (BundleMeta) meta;
			if (!bundleMeta.getItems().isEmpty())
				return true;
		}

		for (Map.Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet()) {
			if (enchant.getKey().getMaxLevel() < enchant.getValue())
				return true;
		}

		if (meta.hasAttributeModifiers()
				|| meta.hasEquippable()
				|| meta.hasFood()
				|| meta.hasItemName()
				|| meta.hasItemModel()
				|| meta.hasJukeboxPlayable()
				|| meta.hasMaxStackSize()
				|| meta.hasRarity()
				|| meta.hasTool()
				|| meta.hasTooltipStyle()
				|| meta.hasUseCooldown()
				|| meta.hasUseRemainder()
				|| meta.hasDamageResistant()
				|| meta.isUnbreakable()

		)
			return true;

		return false;
	}
}