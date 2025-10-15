package net.raiid.customblock;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.raiid.goldrush.Main;

public abstract class CustomBlock {

	private final String id;
	private final String name;
	private final Material material;
	private final float durability;

	public CustomBlock(String id, String name, Material material, float durability) {
		this.id = id;
		this.name = name;
		this.material = material;
		this.durability = durability;
	}

	public ItemStack getItemStack() {
		ItemStack item = new ItemStack(this.material);
		ItemMeta meta = item.getItemMeta();
		meta.setItemName(this.name);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey customBlockKey = new NamespacedKey(Main.instance, "custom_block_id");
		pdc.set(customBlockKey, PersistentDataType.STRING, this.id);
		item.setItemMeta(meta);
		return item;
	}

	public void onPlace(Location loc) {
		
	}
	public void onBreak(Location loc) {
		loc.getBlock().setType(Material.AIR);
	}

	public final void placeBlock(Location loc) {
		loc.getBlock().setType(this.material);
		this.onPlace(loc);
	}
	public final void breakBlock(Location loc) {
		loc.getWorld().getPlayers().forEach(player -> {
			this.sendBlockBreakEffect(loc, player);
		});
		this.onBreak(loc);
	}

	public final boolean placeBlock(Location loc, ItemStack item, Player player) {
		boolean result = false;
		if (item == null) return result;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey customBlockKey = new NamespacedKey(Main.instance, "custom_block_id");
        String id = pdc.get(customBlockKey, PersistentDataType.STRING);
        if (id != null && id.equals(this.id)) {
    		result = true;
    		this.placeBlock(loc);
    		if (player.getGameMode() == GameMode.CREATIVE) return result;
    		item.setAmount(item.getAmount() - 1);
        }
		return result;
	}

	public final void damageBlock(Location loc, float damage, Player player) {
		if (this.getDurability() < 0) return;
		float dmg = damage / this.getDurability();
		loc.getWorld().getPlayers().forEach(target -> {
			this.sendBlockBreakAnimation(loc, dmg, target);
		});
	}

	public final boolean breakBlock(Location loc, Player player) {
		if (this.getDurability() < 0) return false;
		this.breakBlock(loc);
		return true;
	}

	public void sendBlockBreakEffect(Location loc, Player player) {
		if (loc.getWorld() != player.getWorld()) return;
		Block block = loc.getBlock();
		Location playPos = block.getLocation().add(0.5, 0.5, 0.5);
		World world = loc.getWorld();
		BlockData blockData = block.getBlockData();
		world.spawnParticle(Particle.BLOCK_CRUMBLE, playPos, 50, 0.25, 0.25, 0.25, blockData);
		world.playSound(playPos, blockData.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
	}

	public void sendBlockBreakAnimation(Location loc, float damageProgress, Player player) {
		if (loc.getWorld() != player.getWorld()) return;
		if (damageProgress < 0.0 || damageProgress > 1.0) return;
		player.sendBlockDamage(loc, damageProgress);
	}

	public final String getId() {
		return this.id;
	}
	public final String getName() {
		return this.name;
	}
	public final Material getMaterial() {
		return this.material;
	}
	public final float getDurability() {
		return this.durability;
	}

}
