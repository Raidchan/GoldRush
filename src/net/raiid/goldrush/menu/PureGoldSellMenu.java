package net.raiid.goldrush.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.Main;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class PureGoldSellMenu implements Listener {

	public PureGoldSellMenu(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public static void openMenu(Player player) {
		Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, "金を売却する");
		ItemStack trigger = Item.create(Material.MINECART).name(TextUtil.color("&6&l売却を確定")).lore(TextUtil.color("&7左に売却するアイテムを入れて、ここをクリックして確定")).getItemStack();
		inv.setItem(4, trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金を売却する")) return;
	    Inventory inv = event.getInventory();
	    if (event.getRawSlot() != 4) return;//確定スロットかどうか
	    event.setCancelled(true);
		Player player = (Player)event.getWhoClicked();
	    double weight = 0;
	    double price = 0;
	    for (int slot = 0; slot < 4; slot++) {
	    	ItemStack item = inv.getItem(slot);
	    	if (item == null) continue;
	    	double w = GoldRushItem.getGoldWeight(item);
	    	weight += w;
	    	double p = w * Main.instance.getGoldRushCore().getSellPrice();
	    	price += p;
	    	if (p == 0) continue;
	    	inv.setItem(slot, null);
	    }
	    if (price <= 0) return;
	    PlayerDataManager.addMoney(player, price);
	    player.closeInventory();
	    player.sendMessage(TextUtil.color("&6&l" + weight + "g の金を売却しました"));
	    player.sendMessage(TextUtil.color("&6&l +$" + Math.round(price)));
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player)event.getPlayer();
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金を売却する")) return;
	    Inventory inv = event.getInventory();
	    for (int slot = 0; slot < 4; slot++)
	    	if (inv.getItem(slot) != null)
	    		player.getWorld().dropItem(player.getLocation(), inv.getItem(slot));
	}

}
