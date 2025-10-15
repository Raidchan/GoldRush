package net.raiid.customblock.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.raiid.customblock.CustomBlock;

public class GoldenDirt extends CustomBlock {

	public GoldenDirt() {
		super("goldrush:rock", "金を含んだ硬い石", Material.BEDROCK, 50);
	}

	@Override
	public final void onBreak(Location loc) {
		Block block = loc.getBlock();
		if (block == null) return;
		if (block.getType() == Material.BEDROCK) {
			block.setType(Material.INFESTED_COBBLESTONE);
		} else if (block.getType() == Material.INFESTED_COBBLESTONE) {
			block.setType(Material.DEAD_FIRE_CORAL_BLOCK);
		} else if (block.getType() == Material.DEAD_FIRE_CORAL_BLOCK) {
			block.setType(Material.CLAY);
		}
	}

	@Override
	public final void sendBlockBreakAnimation(Location loc, float damageProgress, Player player) {
		Block block = loc.getBlock();
		if (block == null) return;
		if (block.getType() == Material.CLAY) return;
		super.sendBlockBreakAnimation(loc, damageProgress, player);
	}

}
