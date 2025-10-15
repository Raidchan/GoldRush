package net.raiid.goldrush;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.ChatColor;
import net.raiid.util.TextUtil;

public class DirtBundle {

	private static String NBT_BUNDLE_MAX_WEIGHT = "bundle_max_weight";
	private static String NBT_BUNDLE_DIRT_WEIGHT = "bundle_dirt_weight";
	private static String NBT_BUNDLE_GOLD_WEIGHT = "bundle_gold_weight";

    public static ItemStack createDirtBundle(double maxWeight) {
        ItemStack bundle = new ItemStack(Material.BRUSH);
        ItemMeta meta = bundle.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setItemModel(new NamespacedKey("minecraft", "bundle"));
        meta.setItemName(ChatColor.GOLD + "土砂バンドル");
        meta.setMaxStackSize(1);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + String.format("%.1f", 0.0) + "/" + String.format("%.1f", maxWeight) + "kg");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "粘土を右クリックで回収できます");
        meta.setLore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(Main.instance, NBT_BUNDLE_MAX_WEIGHT), PersistentDataType.DOUBLE, maxWeight);
        pdc.set(new NamespacedKey(Main.instance, NBT_BUNDLE_DIRT_WEIGHT), PersistentDataType.DOUBLE, 0.0);
        pdc.set(new NamespacedKey(Main.instance, NBT_BUNDLE_GOLD_WEIGHT), PersistentDataType.DOUBLE, 0.0);
        bundle.setItemMeta(meta);
        return bundle;
    }

    public static boolean isDirtBundle(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Double weight = meta.getPersistentDataContainer().get(new NamespacedKey(Main.instance, NBT_BUNDLE_DIRT_WEIGHT), PersistentDataType.DOUBLE);
        return weight != null && weight >= 0.0;
    }

    public static boolean addDirt(ItemStack bundle, double weight, double goldContent) {
        if (!isDirtBundle(bundle)) return false;
        ItemMeta meta = bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(Main.instance, NBT_BUNDLE_MAX_WEIGHT);
        NamespacedKey goldKey = new NamespacedKey(Main.instance, NBT_BUNDLE_GOLD_WEIGHT);
        NamespacedKey dirtKey = new NamespacedKey(Main.instance, NBT_BUNDLE_DIRT_WEIGHT);
        double maxWeight = pdc.getOrDefault(maxKey, PersistentDataType.DOUBLE, 0.0);
        double currentDirt = pdc.getOrDefault(dirtKey, PersistentDataType.DOUBLE, 0.0);
        double currentGold = pdc.getOrDefault(goldKey, PersistentDataType.DOUBLE, 0.0);
        if (currentDirt >= maxWeight)
        	return false;
        double newWeight = currentDirt + weight + goldContent;
        if (newWeight > maxWeight)
        	newWeight = maxWeight;
        double newGold = currentGold + goldContent;
        pdc.set(dirtKey, PersistentDataType.DOUBLE, newWeight);
        pdc.set(goldKey, PersistentDataType.DOUBLE, newGold);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + String.format("%.1f", newWeight) + "/" + String.format("%.1f", maxWeight) + "kg");
        int progress = (int) ((newWeight / maxWeight) * 10);
        String bar = ChatColor.GREEN + TextUtil.repeat("■", progress) + ChatColor.GRAY + TextUtil.repeat("□", 10 - progress);
        lore.add(bar);
        meta.setLore(lore);
        bundle.setItemMeta(meta);
        return true;
    }

    public static double getDirtWeight(ItemStack bundle) {
        if (!isDirtBundle(bundle)) return 0.0;
        ItemMeta meta = bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey dirtKey = new NamespacedKey(Main.instance, NBT_BUNDLE_DIRT_WEIGHT);
        return pdc.getOrDefault(dirtKey, PersistentDataType.DOUBLE, 0.0);
    }

    public static double getGoldWeight(ItemStack bundle) {
        if (!isDirtBundle(bundle)) return 0.0;
        ItemMeta meta = bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey goldKey = new NamespacedKey(Main.instance, NBT_BUNDLE_GOLD_WEIGHT);
        return pdc.getOrDefault(goldKey, PersistentDataType.DOUBLE, 0.0);
    }

    public static double getMaxWeight(ItemStack bundle) {
        if (!isDirtBundle(bundle)) return 0.0;
        ItemMeta meta = bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(Main.instance, NBT_BUNDLE_MAX_WEIGHT);
        return pdc.getOrDefault(maxKey, PersistentDataType.DOUBLE, 0.0);
    }

    public static ItemStack clearBundle(ItemStack bundle) {
        if (!isDirtBundle(bundle)) return bundle;
        
        ItemMeta meta = bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey maxKey = new NamespacedKey(Main.instance, NBT_BUNDLE_MAX_WEIGHT);
        NamespacedKey dirtKey = new NamespacedKey(Main.instance, NBT_BUNDLE_DIRT_WEIGHT);
        NamespacedKey goldKey = new NamespacedKey(Main.instance, NBT_BUNDLE_GOLD_WEIGHT);
        double maxWeight = pdc.getOrDefault(maxKey, PersistentDataType.DOUBLE, 0.0);
        pdc.set(dirtKey, PersistentDataType.DOUBLE, 0.0);
        pdc.set(goldKey, PersistentDataType.DOUBLE, 0.0);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "容量: " + String.format("%.1f", 0.0) + "/" + String.format("%.1f", maxWeight) + "kg");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "粘土を右クリックで回収できます");
        meta.setLore(lore);

        bundle.setItemMeta(meta);
        return bundle;
    }

}
