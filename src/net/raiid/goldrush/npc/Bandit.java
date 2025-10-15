package net.raiid.goldrush.npc;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.util.ConfigLoc;

public class Bandit {
	// 闇商人エリアに定期的に巡回
	// プレイヤーが安全地帯（テント内、馬車内）にいなければ襲撃
	// NPCに/goldrush npc rob <player>を実行させる
	// 安全地帯判定はRegion.javaを活用

	public void onBanditCatch(Player player) {
	    // 所持金を50-80%奪う
	    double stolenMoney = PlayerDataManager.getMoney(player) * (0.5 + Math.random() * 0.3);
	    PlayerDataManager.removeMoney(player, stolenMoney);
	    
	    // 金アイテムをすべて奪う
	    for (ItemStack item : player.getInventory()) {
	        if (GoldRushItem.getGoldWeight(item) > 0) {
	            player.getInventory().remove(item);
	        }
	    }
	    
	    // 町に強制送還
	    player.teleport(ConfigLoc.get("town_spawn"));
	    player.sendMessage("§c盗賊に襲われた！全てを奪われてしまった...");
	}
}
