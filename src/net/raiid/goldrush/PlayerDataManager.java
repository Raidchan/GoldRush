package net.raiid.goldrush;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerDataManager implements Listener {

	private static Map<Player, PlayerData> all_data = new HashMap<>();

	public PlayerDataManager(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		load();
	}

	public static void load() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			PlayerData data = new PlayerData(Main.instance, player);
			all_data.put(player, data);
		}
	}

	public static double getMoney(Player player) {
		if (all_data.containsKey(player)) {
			return all_data.get(player).getMoney();
		} else {
			PlayerData data = new PlayerData(Main.instance, player);
			all_data.put(player, data);
			return data.getMoney();
		}
	}

	public static void addMoney(Player player, double money) {
		if (all_data.containsKey(player)) {
			all_data.get(player).addMoney(money);
		} else {
			PlayerData data = new PlayerData(Main.instance, player);
			all_data.put(player, data);
			data.addMoney(money);
		}
	}

	public static void removeMoney(Player player, double money) {
		addMoney(player, money * -1);
	}

	public static void save() {
		for (Entry<Player, PlayerData> entry : all_data.entrySet()) {
			entry.getValue().save(Main.instance);
		}
	}

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    	Player player = event.getPlayer();
		PlayerData data = new PlayerData(Main.instance, player);
		all_data.put(player, data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    	Player player = event.getPlayer();
    	if (all_data.containsKey(player)) {
    		PlayerData data = all_data.get(player);
    		data.save(Main.instance);
    		all_data.remove(player);
    	}
    }

}
