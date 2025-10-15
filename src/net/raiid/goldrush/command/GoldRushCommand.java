package net.raiid.goldrush.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.raiid.goldrush.DirtBundle;
import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.Main;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.goldrush.menu.GoldCheckMenu;
import net.raiid.goldrush.menu.GoldSellMenu;
import net.raiid.goldrush.menu.ShopConfirmMenu;
import net.raiid.goldrush.menu.ShopMenu;
import net.raiid.goldrush.menu.SmeltMenu;
import net.raiid.util.TextUtil;

public class GoldRushCommand implements CommandExecutor {

	public GoldRushCommand(Main plugin) {
		plugin.getCommand("goldrush").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg0, String[] args) {
		if (!(sender instanceof Player)) return true;
		Player player = (Player)sender;
		if (args.length > 0) {
			if (args[0].equals("money")) {
				if (args.length > 2) {
					Player target = Bukkit.getPlayer(args[2]);
					if (target == null) return true;
					try {
						double value = Double.parseDouble(args[1]);
						PlayerDataManager.addMoney(target, value);
						player.sendMessage(TextUtil.color("&a" + target.getName() + " に &e$" + value + "&a を追加しました"));
					} catch (NumberFormatException ex) {}
				}
			} else if (args[0].equals("menu")) {
				if (args.length > 2) {
					Player target = Bukkit.getPlayer(args[2]);
					if (target == null) return true;
					if (args[1].equals("smelt")) {
						SmeltMenu.openMenu(target);
					} else if (args[1].equals("sell")) {
						GoldSellMenu.openMenu(target, "商人ギルド");
					} else if (args[1].equals("shop")) {
						ShopMenu.openMenu(target);
					} else if (args[1].equals("sellconfirm")) {
						ShopConfirmMenu.openMenu(target, new ItemStack(Material.APPLE));
					} else if (args[1].equals("check")) {
						GoldCheckMenu.openMenu(target);
					}
				}
			} else if (args[0].equals("item")) {
				Inventory inv = Bukkit.createInventory(null, 54, "GoldRush Debug");
				inv.addItem(DirtBundle.createDirtBundle(500));
				ItemStack bundle = DirtBundle.createDirtBundle(500);
				if (DirtBundle.addDirt(bundle, 0.0, 500.0))
					inv.addItem(bundle);
				ItemStack test = GoldRushItem.INGOT_5;
				inv.addItem(GoldRushItem.setGold(test, 100.0, 0.0, false, false));

				inv.addItem(GoldRushItem.INGOT_1);
				inv.addItem(GoldRushItem.INGOT_2);
				inv.addItem(GoldRushItem.INGOT_3);
				inv.addItem(GoldRushItem.INGOT_4);
				inv.addItem(GoldRushItem.INGOT_5);
				inv.addItem(GoldRushItem.INGOT_6);
				inv.addItem(GoldRushItem.INGOT_7);
				inv.addItem(GoldRushItem.WEAPON_1);
				inv.addItem(GoldRushItem.WEAPON_2);
				inv.addItem(GoldRushItem.WEAPON_3);
				inv.addItem(GoldRushItem.WEAPON_4);
				inv.addItem(GoldRushItem.WEAPON_5);
				inv.addItem(GoldRushItem.WEAPON_6);
				inv.addItem(GoldRushItem.CROWN_2);
				inv.addItem(GoldRushItem.CROWN_1);
				inv.addItem(GoldRushItem.CROWN_3);
				inv.addItem(GoldRushItem.CROWN_4);
				inv.addItem(GoldRushItem.CROWN_5);
				inv.addItem(GoldRushItem.CROWN_6);
				inv.addItem(GoldRushItem.CROWN_7);
				inv.addItem(GoldRushItem.CROWN_8);
				inv.addItem(GoldRushItem.CROWN_9);
				inv.addItem(GoldRushItem.CROWN_10);
				player.openInventory(inv);
			}
		}
		return false;
	}

}
