package net.raiid.goldrush.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class ShopConfirmMenu implements Listener {

	public ShopConfirmMenu(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static void openMenu(Player player, ItemStack item) {
		Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, "購入を確定する");
		ItemStack none = Item.create(Material.BLACK_STAINED_GLASS_PANE).name(" ").getItemStack();
		inv.setItem(0, none);
		inv.setItem(1, none);
		inv.setItem(2, item);//商品
		inv.setItem(3, none);
		ItemStack trigger = Item.create(Material.WRITABLE_BOOK).name(TextUtil.color("&6&l購入を確定")).lore(TextUtil.color("&7ここをクリックして確定")).getItemStack();
		inv.setItem(4, trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("購入を確定する")) return;
	    Inventory inv = event.getInventory();
	    if (event.getRawSlot() != 4) return;//確定スロットかどうか
	    event.setCancelled(true);
		Player player = (Player)event.getWhoClicked();
		double price = -1;

	    player.closeInventory();
	    player.sendMessage(TextUtil.color("&a&l<item>を売却しました"));
	    player.sendMessage(TextUtil.color("&c&l -$" + price));
	}

}
