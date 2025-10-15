package net.raiid.customblock.blocks;

import org.bukkit.Material;

import net.raiid.customblock.CustomBlock;

public class Null extends CustomBlock {

	public Null(Material material) {
		super(null, null, material, material.getHardness() * 30);
	}

}
