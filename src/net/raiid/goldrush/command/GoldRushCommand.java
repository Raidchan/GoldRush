package net.raiid.goldrush.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.raiid.goldrush.DirtBundle;
import net.raiid.goldrush.GoldRushCore.TradingPostType;
import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.Main;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.goldrush.menu.GoldCheckMenu;
import net.raiid.goldrush.menu.GoldSellMenu;
import net.raiid.goldrush.menu.ShopMenu;
import net.raiid.goldrush.menu.SmeltMenu;
import net.raiid.util.TextUtil;

public class GoldRushCommand implements CommandExecutor, TabCompleter {

	public GoldRushCommand(Main plugin) {
		plugin.getCommand("goldrush").setExecutor(this);
		plugin.getCommand("goldrush").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("money")) {
				if (args.length > 2) {
					Player target = Bukkit.getPlayer(args[2]);
					if (target == null) return true;
					try {
						double value = Double.parseDouble(args[1]);
						PlayerDataManager.addMoney(target, value);
						sender.sendMessage(TextUtil.color("&a" + target.getName() + " に &e$" + value + "&a を追加しました"));
					} catch (NumberFormatException ex) {}
				}
			} else if (args[0].equalsIgnoreCase("menu")) {
				if (args.length > 2) {
					Player target = Bukkit.getPlayer(args[2]);
					if (target == null) {
						sender.sendMessage(TextUtil.color("&cプレイヤーが見つかりません: " + args[2]));
						return true;
					}
					
					if (args[1].equalsIgnoreCase("smelt")) {
						SmeltMenu.openMenu(target);
					} else if (args[1].equalsIgnoreCase("sell")) {
						// 売却メニュー
						if (args.length > 3) {
							// /goldrush menu sell <取引所名> <player>
							GoldSellMenu.openMenu(target, args[3]);
						} else {
							GoldSellMenu.openMenu(target, "商人ギルド");
						}
					} else if (args[1].equalsIgnoreCase("shop")) {
						// 購入メニュー
						if (args.length > 3) {
							// /goldrush menu shop <取引所名> <player>
							ShopMenu.openMenu(target, args[3]);
						} else {
							ShopMenu.openMenu(target, "商人ギルド");
						}
					} else if (args[1].equalsIgnoreCase("check")) {
						GoldCheckMenu.openMenu(target);
					}
				}
			} else if (args[0].equalsIgnoreCase("item")) {
				if (!(sender instanceof Player)) return true;
				Player player = (Player)sender;
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
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		
		if (args.length == 1) {
			// 第1引数: サブコマンド
			completions.addAll(Arrays.asList("money", "menu", "item"));
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("menu")) {
				// /goldrush menu <type>
				completions.addAll(Arrays.asList("smelt", "sell", "shop", "check"));
			} else if (args[0].equalsIgnoreCase("money")) {
				// /goldrush money <amount>
				completions.addAll(Arrays.asList("100", "500", "1000"));
			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("money")) {
				// プレイヤー名
				for (Player p : Bukkit.getOnlinePlayers()) {
					completions.add(p.getName());
				}
			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("menu") && 
			    (args[1].equalsIgnoreCase("shop") || args[1].equalsIgnoreCase("sell"))) {
				// /goldrush menu shop/sell <player> <取引所名>
				for (TradingPostType type : TradingPostType.values()) {
					completions.add(type.getName());
				}
			}
		}
		
		// 入力に応じてフィルタリング
		List<String> filtered = new ArrayList<>();
		String input = args[args.length - 1].toLowerCase();
		for (String completion : completions) {
			if (completion.toLowerCase().startsWith(input)) {
				filtered.add(completion);
			}
		}
		
		return filtered;
	}
}
