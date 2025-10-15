package net.raiid.goldrush;

import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.customblock.CustomBlockListener;
import net.raiid.goldrush.command.GoldRushCommand;
import net.raiid.goldrush.listener.GoldRushProtectListener;
import net.raiid.goldrush.listener.DirtListener;
import net.raiid.goldrush.listener.Sidebar;
import net.raiid.goldrush.menu.GoldCheckMenu;
import net.raiid.goldrush.menu.GoldSellMenu;
import net.raiid.goldrush.menu.PureGoldSellMenu;
import net.raiid.goldrush.menu.ShopConfirmMenu;
import net.raiid.goldrush.menu.ShopMenu;
import net.raiid.goldrush.menu.SmeltMenu;

public class Main extends JavaPlugin {

	public static Main instance;

	private GoldRushCore core;

	public GoldRushCore getGoldRushCore() {
		return this.core;
	}

	@Override
	public void onEnable() {
		instance = this;

		this.core = new GoldRushCore(this);

		new GoldRushCommand(this);
		new DirtListener(this);
		new CustomBlockListener(this);
		new Sidebar(this);
		new GoldRushProtectListener(this);
		new PlayerDataManager(this);

		new SmeltMenu(this);
		new GoldSellMenu(this);
		new PureGoldSellMenu(this);
		new ShopMenu(this);
		new ShopConfirmMenu(this);
		new GoldCheckMenu(this);

	}

	@Override
	public void onDisable() {
		PlayerDataManager.save();
	}

}
