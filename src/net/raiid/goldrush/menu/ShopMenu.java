package net.raiid.goldrush.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class ShopMenu implements Listener {

	public ShopMenu(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static void openMenu(Player player) {
		Inventory inv = Bukkit.createInventory(null, 54, "購入する");
		ItemStack trigger = Item.create(Material.MINECART).name(TextUtil.color("&6&l売却を確定")).lore(TextUtil.color("&7左に売却するアイテムを入れて、ここをクリックして確定")).getItemStack();
		inv.setItem(4, trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getSize() != 54) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("購入する")) return;
	    Inventory inv = event.getInventory();
	    if (event.getRawSlot() != 4) return;//確定スロットかどうか
	    event.setCancelled(true);
	    ItemStack item = new ItemStack(Material.APPLE);
		Player player = (Player)event.getWhoClicked();
		ShopConfirmMenu.openMenu(player, item);
	}

}
