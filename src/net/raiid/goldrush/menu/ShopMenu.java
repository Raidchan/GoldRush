package net.raiid.goldrush.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import net.raiid.goldrush.GoldRushCore.TradingPostType;
import net.raiid.goldrush.PlayerDataManager;
import net.raiid.goldrush.shop.ShopInventoryManager;
import net.raiid.goldrush.shop.ShopStock;
import net.raiid.util.Item;
import net.raiid.util.TextUtil;

public class ShopMenu implements Listener {

    private static final int ITEMS_PER_PAGE = 45; // 1ページ45アイテム（最下段は操作用）
    private static final boolean DYNAMIC_SIZE = true; // false にすると常に54スロット固定
    
    public ShopMenu(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void openMenu(Player player, String shopName) {
        TradingPostType type = TradingPostType.nameOf(shopName);
        if (type == null) return;
        openMenu(player, type, 0);
    }

    public static void openMenu(Player player, TradingPostType shopType, int page) {
        Inventory inv = createShopGUI(player, shopType, page);
        
        // プレイヤーをトラッキング
        ShopInventoryManager.getInstance().addViewer(player, shopType, page);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }
    
    /**
     * GUI作成
     */
    private static Inventory createShopGUI(Player player, TradingPostType shopType, int page) {
        String title = shopType.getName() + " - ショップ";
        
        // 在庫取得
        List<ShopStock> stocks = ShopInventoryManager.getInstance().getStocks(shopType);
        
        // ページング計算
        int totalPages = (int) Math.ceil((double) stocks.size() / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, stocks.size());
        
        int itemCount = endIndex - startIndex;
        
        // GUIサイズを動的に決定
        int inventorySize = calculateInventorySize(itemCount, totalPages > 1);
        
        Inventory inv = Bukkit.createInventory(null, inventorySize, title);
        
        // 商品配置
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopStock stock = stocks.get(i);
            ItemStack displayItem = createDisplayItem(player, stock);
            inv.setItem(slot, displayItem);
            slot++;
        }
        
        // 最下段の操作ボタン（常に表示）
        int bottomRowStart = inventorySize - 9;
        
        ItemStack filler = Item.create(Material.GRAY_STAINED_GLASS_PANE).hideToolTip().getItemStack();
        for (int i = bottomRowStart; i < inventorySize; i++) {
            inv.setItem(i, filler);
        }
        
        // 前ページボタン（2ページ目以降）
        if (page > 0) {
            ItemStack prevPage = Item.create(Material.ARROW)
                .name(TextUtil.color("&e← 前のページ"))
                .lore(TextUtil.color("&7ページ " + page + "/" + totalPages))
                .getItemStack();
            inv.setItem(bottomRowStart + 3, prevPage);
        }
        
        // 店舗情報（常に表示）
        ItemStack info = Item.create(Material.FILLED_MAP)
            .name(TextUtil.color("&6&l" + shopType.getName()))
            .lore(
                TextUtil.color("&7商品数: &e" + stocks.size() + "個"),
                TextUtil.color("&7所持金: &e$" + String.format("%.2f", PlayerDataManager.getMoney(player))),
                "",
                TextUtil.color("&7ページ: &e" + (page + 1) + "/" + Math.max(1, totalPages))
            )
            .flag(ItemFlag.HIDE_MAP_ID)
            .getItemStack();
        inv.setItem(bottomRowStart + 4, info);
        
        // 次ページボタン
        if (endIndex < stocks.size()) {
            ItemStack nextPage = Item.create(Material.ARROW)
                .name(TextUtil.color("&e次のページ →"))
                .lore(TextUtil.color("&7ページ " + (page + 2) + "/" + totalPages))
                .getItemStack();
            inv.setItem(bottomRowStart + 5, nextPage);
        }
        
        return inv;
    }
    
    /**
     * 商品数とページ状況に応じて最適なインベントリサイズを計算
     */
    private static int calculateInventorySize(int itemCount, boolean hasMultiplePages) {
        // 動的サイズ無効の場合は常に54
        if (!DYNAMIC_SIZE) {
            return 54;
        }
        
        // 複数ページある場合は常に54（6行）
        if (hasMultiplePages) {
            return 54;
        }
        
        // 1ページのみの場合でも、最下段の操作行は常に表示
        // 必要な商品行数 + 1行（操作用）
        int itemRows = (int) Math.ceil((double) itemCount / 9.0);
        int totalRows = itemRows + 1; // 商品行 + 操作行
        
        // 最小2行（商品1行 + 操作1行）、最大6行
        totalRows = Math.max(2, Math.min(totalRows, 6));
        
        return totalRows * 9;
    }
    
    /**
     * 表示用アイテム作成
     */
    private static ItemStack createDisplayItem(Player player, ShopStock stock) {
        ItemStack displayItem = stock.getItem();
        ItemMeta meta = displayItem.getItemMeta();
        
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // 価格表示追加
        lore.add("");
        lore.add(TextUtil.color("&e価格: $" + String.format("%.2f", stock.getPrice())));
        
        // 動的商品の場合、追加情報
        if (stock.isDynamic()) {
            lore.add(TextUtil.color("&7在庫: 1個限り"));
            long minutes = stock.getElapsedMinutes();
            if (minutes < 60) {
                lore.add(TextUtil.color("&7入荷: " + minutes + "分前"));
            } else {
                lore.add(TextUtil.color("&7入荷: " + (minutes / 60) + "時間前"));
            }
        }
        
        // 所持金不足チェック
        double playerMoney = PlayerDataManager.getMoney(player);
        if (playerMoney < stock.getPrice()) {
            lore.add("");
            lore.add(TextUtil.color("&c&l所持金が不足しています"));
        }
        
        // StockID を NBT に埋め込み
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
    }
    
    /**
     * GUI更新（リアルタイム同期用）
     */
    public static void refreshGUI(Player player, TradingPostType shopType, int page) {
        if (!player.getOpenInventory().getTitle().contains(shopType.getName())) {
            return; // 別の画面を開いている
        }
        
        Inventory currentInv = player.getOpenInventory().getTopInventory();
        Inventory newInv = createShopGUI(player, shopType, page);
        
        // サイズが変わった場合は再オープン
        if (currentInv.getSize() != newInv.getSize()) {
            // インベントリサイズが変わったので、一旦閉じて再度開く
            player.closeInventory();
            openMenu(player, shopType, page);
            player.sendMessage(TextUtil.color("&7※ 商品数が変動したため、画面を更新しました"));
            return;
        }
        
        // サイズが同じ場合は内容だけ更新
        currentInv.setContents(newInv.getContents());
        
        // 更新通知音
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().contains("ショップ")) return;
        
        // ショップGUI内では全てのクリックをキャンセル
        event.setCancelled(true);
        
        // 下部インベントリ（プレイヤーのインベントリ）のクリックは無視
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            return;
        }
        
        // 上部インベントリ以外は処理しない
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        int slot = event.getRawSlot();
        int invSize = event.getInventory().getSize();
        
        // 店舗タイプ取得
        TradingPostType shopType = getShopTypeFromTitle(event.getView().getTitle());
        if (shopType == null) return;
        
        // 現在のページ取得
        int currentPage = getCurrentPage(player);
        
        // 在庫取得
        List<ShopStock> stocks = ShopInventoryManager.getInstance().getStocks(shopType);
        int totalPages = (int) Math.ceil((double) stocks.size() / ITEMS_PER_PAGE);
        
        // 操作ボタンの行は常にある
        int bottomRowStart = invSize - 9;
        
        // 操作ボタンのクリック判定
        if (slot >= bottomRowStart) {
            int relativeSlot = slot - bottomRowStart;
            
            if (relativeSlot == 3 && currentPage > 0) {
                // 前ページ
                openMenu(player, shopType, currentPage - 1);
                return;
            } else if (relativeSlot == 5 && currentPage < totalPages - 1) {
                // 次ページ
                openMenu(player, shopType, currentPage + 1);
                return;
            } else if (relativeSlot == 4) {
                // 店舗情報（何もしない）
                return;
            }
            // その他の操作スロット
            return;
        }
        
        // 商品クリック
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // 在庫から対応する商品を検索
        int itemIndex = currentPage * ITEMS_PER_PAGE + slot;
        
        if (itemIndex >= stocks.size()) return;
        
        ShopStock stock = stocks.get(itemIndex);
        
        // 所持金チェック
        double playerMoney = PlayerDataManager.getMoney(player);
        if (playerMoney < stock.getPrice()) {
            player.sendMessage(TextUtil.color("&c&l所持金が不足しています！"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // 購入確認画面へ
        ShopConfirmMenu.openMenu(player, stock);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().contains("ショップ")) return;
        
        // トラッキングから削除
        ShopInventoryManager.getInstance().removeViewer(player);
    }
    
    /**
     * タイトルから店舗タイプ取得
     */
    private TradingPostType getShopTypeFromTitle(String title) {
        for (TradingPostType type : TradingPostType.values()) {
            if (title.contains(type.getName())) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * プレイヤーの現在のページ取得
     */
    private int getCurrentPage(Player player) {
        // ShopInventoryManagerから現在のページ情報を取得
        return ShopInventoryManager.getInstance().getCurrentPage(player);
    }
}