package net.raiid.goldrush;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.raiid.util.Item;
import net.raiid.util.TextUtil;
import net.raiid.util.ValueUtil;

public class GoldRushItem {

	private static String NBT_GOLD_TYPE = "gold_type";
	private static String NBT_GOLD_WEIGHT = "gold_weight";
	private static String NBT_GOLD_FAKE_WEIGHT = "gold_weight_impurities";
	private static String NBT_GOLD_IS_PURE = "gold_is_pure";
	private static String NBT_GOLD_IS_ILLEGAL = "gold_is_illegal";

	public static ItemStack INGOT_1 = createGold("&6砂金", "glowstone_dust", "&7金の粒の山", GoldRushCore.GoldType.DUST, 1.0, 0.0, false, false);
	public static ItemStack INGOT_2 = createGold("&6&l小さな金塊", "gold_nugget", "&7小さな金の塊", GoldRushCore.GoldType.SMALL_NUGGET, 1.0, 0.0, false, false);
	public static ItemStack INGOT_3 = createGold("&6&l金塊", "raw_gold", "&7金の塊", GoldRushCore.GoldType.NUGGET, 1.0, 0.0, false, false);
	public static ItemStack INGOT_4 = createGold("&6&l金板", "light_weighted_pressure_plate", "&7金のプレート", GoldRushCore.GoldType.SHEET, 1.0, 0.0, false, false);
	public static ItemStack INGOT_5 = createGold("&6&l金の延べ棒", "gold_ingot", "&7金の延べ棒", GoldRushCore.GoldType.INGOT, 1.0, 0.0, false, false);
	public static ItemStack INGOT_6 = createGold("&6&l金のキューブ", "raw_gold_block", "&7金のキューブ", GoldRushCore.GoldType.CUBE, 1.0, 0.0, false, false);
	public static ItemStack INGOT_7 = createGold("&6&l金のブロック", "gold_block", "&7金のブロック", GoldRushCore.GoldType.BLOCK, 1.0, 0.0, false, false);

	public static ItemStack WEAPON_1 = createGold("&6&l黄金の剣", "golden_sword", "&7金製の剣", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack WEAPON_2 = createGold("&6&l黄金の斧", "golden_axe", "&7金製の斧", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack WEAPON_3 = createGold("&6&l黄金のヘルメット", "golden_helmet", "&7金製のヘルメット", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack WEAPON_4 = createGold("&6&l黄金のチェストプレート", "golden_chestplate", "&7金製のチェストプレート", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack WEAPON_5 = createGold("&6&l黄金のレギンス", "golden_leggings", "&7金製のレギンス", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack WEAPON_6 = createGold("&6&l黄金のブーツ", "golden_boots", "&7金製のブーツ", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);

	public static ItemStack CROWN_1 = createGold("&6&l黄金のステッキ", "blaze_rod", "&7金製のステッキ", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_2 = createGold("&6&l黄金の矢", "spectral_arrow", "&7金製の矢", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_3 = createGold("&6&l黄金の卵", "blaze_spawn_egg", "&7金製の卵の置物", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_4 = createGold("&6&l黄金のリンゴ", "golden_apple", "&7金製のリンゴの置物", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_5 = createGold("&6&l黄金のニンジン", "golden_carrot", "&7金製のニンジンの置物", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_6 = createGold("&6&l黄金の鐘", "bell", "&7金製の鐘", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_7 = createGold("&6&l黄金の馬", "golden_horse_armor", "&7金製の馬の置物", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_8 = createGold("&6&l黄金の時計", "clock", "&7金製の時計", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_9 = createGold("&6&l黄金の望遠鏡", "spyglass", "&7金製の望遠鏡", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);
	public static ItemStack CROWN_10 = createGold("&6&l黄金の像", "totem_of_undying", "&7金製の謎の像", GoldRushCore.GoldType.JEWELRY, 1.0, 0.0, false, false);

	public static ItemStack createGold(String name, String modelId, String info, GoldRushCore.GoldType type, double goldWeight, double impuritiesWeight, boolean verified, boolean isIllegal) {
		List<String> lore = new ArrayList<>();
		lore.add(info != null ? TextUtil.color(info) : " ");
		ItemStack item = Item.create(Material.GOLD_INGOT)
				.name(TextUtil.color(name))
				.lore(lore)
				.maxStack(1)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.itemModel(modelId)
				.nbtString(NBT_GOLD_TYPE, type.getNBTLabel())
				.getItemStack();
		return setGold(item, goldWeight, impuritiesWeight, verified, isIllegal);
	}

	public static GoldRushCore.GoldType getGoldType(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, NBT_GOLD_TYPE);
        String label = null;
        if (pdc.has(nkey))
        	label = pdc.get(nkey, PersistentDataType.STRING);
        return GoldRushCore.GoldType.labelOf(label);
	}
	public static double getGoldWeight(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, NBT_GOLD_WEIGHT);
        if (pdc.has(nkey))
        	return pdc.get(nkey, PersistentDataType.DOUBLE);
		return 0.0f;
	}
	public static double getImpurities(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, NBT_GOLD_FAKE_WEIGHT);
        if (pdc.has(nkey))
        	return pdc.get(nkey, PersistentDataType.DOUBLE);
		return 0.0f;
	}
	public static boolean isIllegal(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, NBT_GOLD_IS_ILLEGAL);
        if (pdc.has(nkey))
        	return pdc.get(nkey, PersistentDataType.BOOLEAN);
		return false;
	}
	public static boolean checkPure(double goldWeight, double impuritiesWeight) {
		if (goldWeight >= 0 && impuritiesWeight >= 0) {
			double total = goldWeight + impuritiesWeight;
			if (total <= 0) return false;
			double purity = goldWeight / total;
			return purity >= 0.95;
		}
		return false;
	}
	public static ItemStack checkPure(ItemStack item) {
		double goldWeight = getGoldWeight(item);
		double impuritiesWeight = getImpurities(item);
		boolean isGoldPurityVerified = checkPure(goldWeight, impuritiesWeight);
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(Enchantment.FORTUNE, isGoldPurityVerified ? 1 : 0, true);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(Main.instance, NBT_GOLD_IS_PURE), PersistentDataType.BOOLEAN, isGoldPurityVerified);
        if (isGoldPurityVerified) {
        	List<String> lore = meta.getLore();
        	for (int i = 0; i < lore.size(); i++) {
        		if (!lore.get(i).contains("純金"))
        			lore.set(i, lore.get(i).replaceAll("金", "純金"));
        	}
        	lore.add(" ");
        	lore.add(TextUtil.color("&d&n純金鑑定済み"));
            meta.setLore(lore);
        }
		item.setItemMeta(meta);
		return item;
	}
	public static boolean isVerified(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey nkey = new NamespacedKey(Main.instance, NBT_GOLD_IS_PURE);
        if (pdc.has(nkey))
        	return pdc.get(nkey, PersistentDataType.BOOLEAN);
		return false;
	}

	public static ItemStack setGold(ItemStack item, double goldWeight, double impuritiesWeight, boolean verified, boolean isIllegal) {
		goldWeight = ValueUtil.round(goldWeight, 2);
		impuritiesWeight = ValueUtil.round(impuritiesWeight, 2);
		boolean isGoldPurityVerified = false;
		if (verified)
			isGoldPurityVerified = checkPure(goldWeight, impuritiesWeight);
		ItemMeta meta = item.getItemMeta();
		meta.removeEnchant(Enchantment.FORTUNE);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(Main.instance, NBT_GOLD_WEIGHT), PersistentDataType.DOUBLE, goldWeight);
        pdc.set(new NamespacedKey(Main.instance, NBT_GOLD_FAKE_WEIGHT), PersistentDataType.DOUBLE, impuritiesWeight);
        pdc.set(new NamespacedKey(Main.instance, NBT_GOLD_IS_PURE), PersistentDataType.BOOLEAN, isGoldPurityVerified);
        pdc.set(new NamespacedKey(Main.instance, NBT_GOLD_IS_ILLEGAL), PersistentDataType.BOOLEAN, isIllegal);
        List<String> loreOld = meta.getLore();
        List<String> lore = new ArrayList<>();
        lore.add(loreOld.size() > 0 ? loreOld.get(0) : " ");
        lore.add(" ");
        lore.add("§e重さ: " + ValueUtil.round((goldWeight + impuritiesWeight), 2) + "g");
        if (isIllegal) {
			lore.add("§c禁制品");
        	for (int i = 0; i < lore.size(); i++) {
        		if (!lore.get(i).contains(TextUtil.color("&7怪しい")))
        			lore.set(i, lore.get(i).replaceAll(TextUtil.color("&7"), TextUtil.color("&7&o怪しい")));
        	}
        }
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

}
