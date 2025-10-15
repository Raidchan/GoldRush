package net.raiid.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigLoc {

	private static JavaPlugin instance;
	private static String FILE = "config_location.yml";
	private static FileConfiguration data;
	private static Map<String, Location> ALL;

	public static void load(JavaPlugin plugin) {
		instance = plugin;
		try {
			ALL = new HashMap<>();
			if (!plugin.getDataFolder().exists())
				plugin.getDataFolder().mkdirs();
			File dataFile = new File(plugin.getDataFolder(), FILE);
			if (!dataFile.exists())
				dataFile.createNewFile();
			data = YamlConfiguration.loadConfiguration(dataFile);
			ConfigurationSection section = data.getDefaultSection();
			if (section != null) {
				for (String key : section.getKeys(false)) {
					World world = Bukkit.getWorld(data.getString(key + ".World"));
					double x = data.getDouble(key + ".X");
					double y = data.getDouble(key + ".Y");
					double z = data.getDouble(key + ".Z");
					Location loc = new Location(world, x, y, z);
					ALL.put(key, loc);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Location get(String key) {
		return ALL.get(key);
	}

	public static void set(String key, Location loc) {
		ALL.put(key, loc);
		data.set(key + ".World", loc.getWorld().getName());
		data.set(key + ".X", loc.getX());
		data.set(key + ".Y", loc.getY());
		data.set(key + ".Z", loc.getZ());
		save(instance);
	}

	public static void save(JavaPlugin plugin) {
		try {
			if (!plugin.getDataFolder().exists())
				plugin.getDataFolder().mkdirs();
			File dataFile = new File(plugin.getDataFolder(), FILE);
			if (!dataFile.exists())
				dataFile.createNewFile();
			data.save(dataFile);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
