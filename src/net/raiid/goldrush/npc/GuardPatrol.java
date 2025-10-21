package net.raiid.goldrush.npc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.goldrush.GoldRushItem;

public class GuardPatrol {
	// 昼: 5-7人、夜: 2-3人
	// 路地裏は巡回しない設定

	public GuardPatrol(JavaPlugin plugin) {
		//昼夜制御:
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
		    long time = Bukkit.getWorld("world").getTime();
		    boolean isDay = (time >= 0 && time < 13000);
		    
		    // NPCのスポーン制御
		    if (isDay) {
		        //spawnGuards(5);
		    } else {
		        //spawnGuards(2);
		    }
		}, 0L, 6000L); // 5分ごとにチェック
	}
	public void onGuardCatch(Player player) {
	    // 違法アイテムを全て没収
	    int confiscatedCount = 0;
	    for (ItemStack item : player.getInventory()) {
	        if (GoldRushItem.isIllegal(item)) {
	            player.getInventory().remove(item);
	            confiscatedCount++;
	        }
	    }
	    
	    // 信用度ペナルティ
	    //PlayerReputation rep = PlayerDataManager.getReputation(player);
	    //rep.onConfiscated();
	    
	    player.sendMessage("§c警備兵に捕まった！違法アイテム" + confiscatedCount + "個が没収された");
	}
}
