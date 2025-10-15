package net.raiid.goldrush;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerData {

	private JavaPlugin plugin;
	private Player player;
	private File dataFile;
	private YamlConfiguration data;

	private double money;

	public PlayerData(JavaPlugin plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
		try {
			if (!this.plugin.getDataFolder().exists())
				this.plugin.getDataFolder().mkdirs();
			File folder = new File(this.plugin.getDataFolder(), "players");
			if (!folder.exists())
				folder.mkdirs();
			this.dataFile = new File(folder, player.getUniqueId() + ".yml");
			if (!this.dataFile.exists())
				this.dataFile.createNewFile();
			this.data = YamlConfiguration.loadConfiguration(this.dataFile);
			if (!this.data.contains("money"))
			    this.data.set("money", 0);
			this.money = this.data.getDouble("money", 0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public Player getPlayer() {
		return this.player;
	}

	public double getMoney() {
		return this.money;
	}
	public void setMoney(double money) {
		this.money = money;
		this.save(this.plugin);
	}
	public void addMoney(double money) {
		this.money += money;
		this.save(this.plugin);
	}

	public void save(JavaPlugin plugin) {
		try {
			if (!plugin.getDataFolder().exists())
				plugin.getDataFolder().mkdirs();
			if (!this.dataFile.exists())
				this.dataFile.createNewFile();
			this.data.set("money", this.money);
			this.data.save(this.dataFile);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
