package net.raiid.util;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class SkullUtils {

	public static ItemStack byBase64(String base64) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		try {
		    String prepare = new String(Base64.getDecoder().decode(base64));
		    Matcher matcher = Pattern.compile("[\"{a-zA-Z:]+(http://[a-zA-Z0-9./]+)[}\"]+").matcher(prepare);
		    if (!matcher.matches()) return null;
			URL url = new URL(matcher.group(1));
			SkullMeta meta = (SkullMeta)item.getItemMeta();
			PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
			PlayerTextures texture = profile.getTextures();
			texture.setSkin(url);
			meta.setOwnerProfile(profile);
			item.setItemMeta(meta);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return item;
	}

}
