package net.raiid.customblock;

import org.bukkit.Location;

import net.raiid.customblock.blocks.GoldenDirt;

public class Populartor {

	public static CustomBlock patch(Location loc) {
		if (!CustomBlockListener.placed.containsKey(loc)) {
			switch (loc.getBlock().getType()) {
			case BEDROCK:
			case INFESTED_COBBLESTONE:
			case DEAD_FIRE_CORAL_BLOCK:
				CustomBlock cb = new GoldenDirt();
				CustomBlockListener.placed.put(loc, cb);
				break;
			default:
				break;
			}
		}
		return null;
	}

}
