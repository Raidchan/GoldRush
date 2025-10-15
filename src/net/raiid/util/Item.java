package net.raiid.util;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.raiid.goldrush.Main;

public class Item {

	public static ItemStack create(Material material, int amount, String name, String lore, Enchantment ench, int level, boolean hide) {
		ItemStack item = new ItemStack(material);
		item.setAmount(amount);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(Arrays.asList(lore.split("\n")));
		if (ench != null) {
			meta.addEnchant(ench, level, true);
			if (hide)
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		item.setItemMeta(meta);
		return item;
	}




	public static Item create(Material material) {
		return new Item(new ItemStack(material));
	}

	private ItemStack item;

	public Item(ItemStack item) {
		this.item = item;
	}
	public ItemStack getItemStack() {
		return this.item;
	}
	public boolean equals(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) return false;
		ItemStack a1 = item.clone();
		a1.setAmount(1);
		ItemStack a2 = this.item.clone();
		a2.setAmount(1);
		return a1.equals(a2);
	}

	public Item amount(int amount) {
		this.item.setAmount(amount);
		return this;
	}
	public Item name(String name) {
		ItemMeta meta = this.item.getItemMeta();
		meta.setDisplayName(name);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item lore(List<String> lore) {
		ItemMeta meta = this.item.getItemMeta();
		meta.setLore(lore);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item lore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}
	public Item ench(Enchantment ench, int level) {
		if (level < 1) return this;
		ItemMeta meta = this.item.getItemMeta();
		meta.addEnchant(ench, level, true);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item unbreak() {
		ItemMeta meta = this.item.getItemMeta();
		meta.setUnbreakable(true);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item maxStack(int maxStack) {
		ItemMeta meta = this.item.getItemMeta();
		meta.setMaxStackSize(maxStack);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item hideToolTip() {
		ItemMeta meta = this.item.getItemMeta();
		meta.setHideTooltip(true);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item flag(ItemFlag flag) {
		ItemMeta meta = this.item.getItemMeta();
		meta.addItemFlags(flag);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item itemModel(String modelId) {
		ItemMeta meta = this.item.getItemMeta();
        meta.setItemModel(new NamespacedKey("minecraft", modelId));
		this.item.setItemMeta(meta);
		return this;
	}

	public Item nbtString(String key, String str) {
		ItemMeta meta = this.item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, key);
		pdc.set(nkey, PersistentDataType.STRING, str);
		item.setItemMeta(meta);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item nbtInt(String key, int value) {
		ItemMeta meta = this.item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, key);
		pdc.set(nkey, PersistentDataType.INTEGER, value);
		item.setItemMeta(meta);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item nbtDouble(String key, double value) {
		ItemMeta meta = this.item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, key);
		pdc.set(nkey, PersistentDataType.DOUBLE, value);
		item.setItemMeta(meta);
		this.item.setItemMeta(meta);
		return this;
	}
	public Item nbtBoolean(String key, boolean flag) {
		ItemMeta meta = this.item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, key);
		pdc.set(nkey, PersistentDataType.BOOLEAN, flag);
		item.setItemMeta(meta);
		this.item.setItemMeta(meta);
		return this;
	}

}
