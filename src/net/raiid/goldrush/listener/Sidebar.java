package net.raiid.goldrush.listener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import net.raiid.goldrush.Main;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.util.SidebarUtil;

public class Sidebar implements Listener, CommandExecutor {

    private Map<UUID, SidebarUtil> sidebarMap = new HashMap<>();
    private Map<UUID, BukkitTask> sidebarTaskMap = new HashMap<>();

	private final Main plugin;

	public Sidebar(Main plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	    for (Player player : Bukkit.getOnlinePlayers())
	    	this.createSidebar(this.plugin, player);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg0, String[] args) {
		if (!(sender instanceof Player)) return true;
		Player player = (Player)sender;
		if (this.sidebarMap.containsKey(player.getUniqueId())) {
			this.removeSidebar(player);
		} else {
			this.createSidebar(this.plugin, player);
		}
		player.sendMessage("サイドバーの表示を切り替えました。");
		return false;
	}

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    	this.createSidebar(this.plugin, event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    	this.removeSidebar(event.getPlayer());
    }

    private void createSidebar(Main plugin, Player player) {
        SidebarUtil sidebar = new SidebarUtil(player, "&e&lGOLDRUSH");
        this.sidebarMap.put(player.getUniqueId(), sidebar);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                sidebar.setLines(Arrays.asList(
                        "&f" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                        "",
                        "&fOnlines: &e" + Bukkit.getOnlinePlayers().size(),
                        "",
                        "&fMoney: &e$" + String.format("%.2f", PlayerDataManager.getMoney(player)),
                        "",
                        "&ekusosaba.net"
                ));
            }
        }, 0L, 10L);
        this.sidebarTaskMap.put(player.getUniqueId(), task);
    }

    private void removeSidebar(Player player) {
        SidebarUtil sidebar = this.sidebarMap.remove(player.getUniqueId());
        if (sidebar != null) {
            sidebar.remove();
        }
        BukkitTask task = this.sidebarTaskMap.remove(player.getUniqueId());
        if (task != null) {
        	task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : this.sidebarTaskMap.values()) {
            task.cancel();
        }
        sidebarTaskMap.clear();
        for (UUID uuid : this.sidebarMap.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
            	this.sidebarMap.get(uuid).remove();
            }
        }
        this.sidebarMap.clear();
    }

}
