package net.raiid.goldrush.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;
import net.raiid.util.ValueUtil;

public class GoldCheckMenu implements Listener {

	public GoldCheckMenu(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	private static double CHECK_PRICE = 2000.0;

	public static void openMenu(Player player) {
		Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, "金の鑑定を行う");
		ItemStack none = Item.create(Material.BLACK_STAINED_GLASS_PANE).hideToolTip().getItemStack();
		inv.setItem(1, none);
		inv.setItem(2, none);
		inv.setItem(3, none);
		ItemStack trigger = Item.create(Material.WRITABLE_BOOK).name(TextUtil.color("&6&l鑑定結果")).lore(TextUtil.color("&7クリックして左のアイテムの鑑定結果を表示"), TextUtil.color("&b&n鑑定料: $" + CHECK_PRICE)).getItemStack();
		inv.setItem(4, trigger);
		player.openInventory(inv);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金の鑑定を行う")) return;
	    if (event.getClickedInventory() != event.getView().getTopInventory()) return;
	    if (event.getRawSlot() == 0) return;
    	event.setCancelled(true);
	    if (event.getRawSlot() != 4) return;
	    Inventory inv = event.getInventory();
	    ItemStack check = inv.getItem(0);
	    if (check != null && check.getType() != Material.AIR) {
	    	ItemStack info = inv.getItem(4);
	    	if (info.getType() != Material.WRITABLE_BOOK) return;
			Player player = (Player)event.getWhoClicked();
			if (PlayerDataManager.getMoney(player) < CHECK_PRICE) {
	    		info = Item.create(Material.WRITABLE_BOOK).name(TextUtil.color("&6&l鑑定結果")).lore(TextUtil.color("&7クリックして左のアイテムの鑑定結果を表示"), TextUtil.color("&b&n鑑定料: $" + CHECK_PRICE), TextUtil.color("&c&l所持金が不足しています！")).getItemStack();
				player.sendMessage(TextUtil.color("&c&l所持金が不足しています！"));
			} else {
		    	double goldWeight = GoldRushItem.getGoldWeight(check);
		    	double impuritiesWeight = GoldRushItem.getImpurities(check);
		    	if (goldWeight + impuritiesWeight > 0) {
					PlayerDataManager.removeMoney(player, CHECK_PRICE);
					if (GoldRushItem.isVerified(check)) {
			    		info = Item.create(Material.WRITABLE_BOOK).name(TextUtil.color("&6&l鑑定結果")).lore(TextUtil.color("&7クリックして左のアイテムの鑑定結果を表示"), TextUtil.color("&b&n鑑定料: $" + CHECK_PRICE), TextUtil.color("&d&lこのアイテムは純金鑑定済みです！")).getItemStack();
						player.sendMessage(TextUtil.color("&d&lこのアイテムは純金鑑定済みです！"));
					} else {
						if (!GoldRushItem.isIllegal(check)) {
							check = GoldRushItem.checkPure(check);
							inv.setItem(0, check);
						}
						player.sendMessage(TextUtil.color("&a&lアイテムの鑑定に $" + CHECK_PRICE + " を支払いました。"));
				    	double weight = goldWeight + GoldRushItem.getImpurities(check);
				    	double purity = ValueUtil.round(goldWeight / weight * 100, 2);
			    		info = Item.create(Material.ENCHANTED_BOOK).name(TextUtil.color("&6&l鑑定結果")).lore(TextUtil.color("&e純金の配合率: &n" + purity + "%")).getItemStack();
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
		                player.sendMessage(TextUtil.color("&6&l━━━━━━━━━━━━━━━━━━━━"));
		                player.sendMessage(TextUtil.color("&6&l鑑定が完了しました！"));
		                player.sendMessage(TextUtil.color("&7鑑定料: &c-$" + CHECK_PRICE));
		                player.sendMessage(TextUtil.color("&7純金の配合率: &e" + purity + "%"));
		                if (purity >= 95.0) {
		                    player.sendMessage(TextUtil.color("&d&l✦ 純金認定 ✦"));
		                }
		                player.sendMessage(TextUtil.color("&7残高: &e$" + String.format("%.2f", PlayerDataManager.getMoney(player))));
		                player.sendMessage(TextUtil.color("&6&l━━━━━━━━━━━━━━━━━━━━"));
					}
		    	} else {
		    		info = Item.create(Material.WRITABLE_BOOK).name(TextUtil.color("&6&l鑑定結果")).lore(TextUtil.color("&7クリックして左のアイテムの鑑定結果を表示"), TextUtil.color("&b&n鑑定料: $" + CHECK_PRICE), TextUtil.color("&c&lこのアイテムは鑑定対象ではありません！")).getItemStack();
					player.sendMessage(TextUtil.color("&c&lこのアイテムは鑑定対象ではありません！"));
		    	}
			}
			inv.setItem(4, info);
	    }
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player)event.getPlayer();
	    if (event.getInventory().getType() != InventoryType.HOPPER) return;
	    if (event.getView().getTitle() == null) return;
	    if (!event.getView().getTitle().equals("金の鑑定を行う")) return;
	    Inventory inv = event.getInventory();
	    if (inv.getItem(0) != null && inv.getItem(0).getType() != Material.AIR)
	    	player.getWorld().dropItem(player.getLocation(), inv.getItem(0));
	}

}
