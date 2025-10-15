package net.raiid.goldrush.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CraftJewelryMenu {
	// 金塊 → 装飾品への加工
	// 重さは変わらないが、GoldTypeがJEWELRYに変化
	// 加工費として少額の料金を徴収

	public static void openMenu(Player player, ItemStack goldIngot) {
	    // GoldRushItem.WEAPON_1 ~ CROWN_10 のリストを表示
	    // 選択すると、goldIngotの重さを引き継いで装飾品を生成
	}
}
