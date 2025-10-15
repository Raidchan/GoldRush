package net.raiid.util;

import org.bukkit.ChatColor;

public final class TextUtil {

	public static String repeat(String text, int repert) {
		String result = "";
		for (int i = 0; i < repert; i++)
			result += text;
		return result;
	}

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    public static String getLastColor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String coloredText = color(text);
        String lastColor = "";
        for (int i = coloredText.length() - 2; i >= 0; i--) {
            if (coloredText.charAt(i) == '§') {
                char code = Character.toLowerCase(coloredText.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    lastColor = "§" + code;
                    break;
                }
                if (code == 'r') {
                    lastColor = "";
                    break;
                }
            }
        }
        return lastColor;
    }

}