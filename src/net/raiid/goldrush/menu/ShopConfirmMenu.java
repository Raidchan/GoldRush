package net.raiid.goldrush.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import net.raiid.goldrush.PlayerDataManager;
import net.raiid.goldrush.shop.ShopInventoryManager;
import net.raiid.goldrush.shop.ShopStock;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class ShopConfirmMenu implements Listener {

    // プレイヤーが確認中の商品を追跡
    private static Map<Player, UUID> confirming = new HashMap<>();
    
    public ShopConfirmMenu(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void openMenu(Player player, ShopStock stock) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, "購入を確定する");
        
        // フィラー
        ItemStack filler = Item.create(Material.BLACK_STAINED_GLASS_PANE).name(" ").getItemStack();
        inv.setItem(0, filler);
        inv.setItem(1, filler);
        inv.setItem(3, filler);
        
        // 商品表示
        ItemStack displayItem = stock.getItem();
        inv.setItem(2, displayItem);
        
        // 確定ボタン
        ItemStack confirm = Item.create(Material.LIME_STAINED_GLASS_PANE)
            .name(TextUtil.color("&a&l購入を確定"))
            .lore(
                TextUtil.color("&7クリックして購入を完了します"),
                "",
                TextUtil.color("&e価格: $" + String.format("%.2f", stock.getPrice())),
                TextUtil.color("&7所持金: $" + String.format("%.2f", PlayerDataManager.getMoney(player)))
            )
            .getItemStack();
        inv.setItem(4, confirm);
        
        // 追跡に追加
        confirming.put(player, stock.getStockId());
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (event.getInventory().getType() != InventoryType.HOPPER) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals("購入を確定する")) return;
        
        event.setCancelled(true);
        
        // 確定ボタン以外はスルー
        if (event.getRawSlot() != 4) return;
        
        // 購入処理
        UUID stockId = confirming.get(player);
        if (stockId == null) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c&lエラーが発生しました"));
            return;
        }
        
        // 在庫存在チェック
        if (!ShopInventoryManager.getInstance().stockExists(stockId)) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c&l申し訳ございません。"));
            player.sendMessage(TextUtil.color("&c&lこちらの商品は既に売り切れました。"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            confirming.remove(player);
            return;
        }
        
        ShopStock stock = ShopInventoryManager.getInstance().getStock(stockId);
        if (stock == null) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c&lエラーが発生しました"));
            confirming.remove(player);
            return;
        }
        
        // 所持金チェック
        double playerMoney = PlayerDataManager.getMoney(player);
        if (playerMoney < stock.getPrice()) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c&l所持金が不足しています！"));
            player.sendMessage(TextUtil.color("&7必要: $" + String.format("%.2f", stock.getPrice())));
            player.sendMessage(TextUtil.color("&7所持: $" + String.format("%.2f", playerMoney)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            confirming.remove(player);
            return;
        }
        
        // 購入試行（排他制御）
        if (ShopInventoryManager.getInstance().tryPurchase(stockId, player)) {
            // 購入成功
            PlayerDataManager.removeMoney(player, stock.getPrice());
            
            // アイテム付与
            ItemStack purchasedItem = stock.getItem();
            if (player.getInventory().firstEmpty() == -1) {
                // インベントリ満タンの場合はドロップ
                player.getWorld().dropItem(player.getLocation(), purchasedItem);
                player.sendMessage(TextUtil.color("&e※ インベントリが満タンのため、アイテムを足元にドロップしました"));
            } else {
                player.getInventory().addItem(purchasedItem);
            }
            
            player.closeInventory();
            player.sendMessage(TextUtil.color("&a&l━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(TextUtil.color("&a&l購入が完了しました！"));
            player.sendMessage(TextUtil.color("&7商品: &e" + purchasedItem.getItemMeta().getDisplayName()));
            player.sendMessage(TextUtil.color("&7支払額: &c-$" + String.format("%.2f", stock.getPrice())));
            player.sendMessage(TextUtil.color("&7残高: &e$" + String.format("%.2f", PlayerDataManager.getMoney(player))));
            player.sendMessage(TextUtil.color("&a&l━━━━━━━━━━━━━━━━━━━━"));
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            
        } else {
            // 購入失敗（同時購入で負けた）
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c&l申し訳ございません。"));
            player.sendMessage(TextUtil.color("&c&l他のお客様が先に購入されました。"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        
        confirming.remove(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        if (event.getInventory().getType() != InventoryType.HOPPER) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals("購入を確定する")) return;
        
        // 追跡から削除
        confirming.remove(player);
    }
}