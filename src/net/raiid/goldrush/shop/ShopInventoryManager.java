package net.raiid.goldrush.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.raiid.goldrush.DirtBundle;
import net.raiid.goldrush.GoldRushCore;
import net.raiid.goldrush.GoldRushCore.GoldType;
import net.raiid.goldrush.GoldRushCore.TradingPostType;
import net.raiid.goldrush.GoldRushItem;
import net.raiid.goldrush.Main;
import net.raiid.goldrush.menu.ShopMenu;

/**
 * Shop在庫管理システム
 */
public class ShopInventoryManager {
    
    private static ShopInventoryManager instance;
    private final JavaPlugin plugin;
    
    // 在庫データ
    private Map<TradingPostType, List<ShopStock>> allStocks = new HashMap<>();
    
    // 現在Shopを見ているプレイヤー
    private Map<Player, ShopViewData> activeViewers = new HashMap<>();
    
    // 更新タスク管理
    private Map<TradingPostType, BukkitTask> pendingUpdates = new HashMap<>();
    private BukkitTask restockTask;
    
    // データファイル
    private File dataFile;
    private FileConfiguration dataConfig;
    
    // 在庫設定
    private static final int MAX_DYNAMIC_SLOTS = 10; // 動的商品の最大スロット数
    private static final long RESTOCK_INTERVAL = 5 * 60 * 20L; // 5分ごと補充チェック
    
    public ShopInventoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        
        // 初期化
        for (TradingPostType type : TradingPostType.values()) {
            allStocks.put(type, new ArrayList<>());
        }
        
        // データファイル初期化
        initDataFile();
        
        // データ読み込み
        loadData();
        
        // 静的商品の登録
        registerStaticProducts();
        
        // 動的商品の初期生成
        generateInitialDynamicStocks();
        
        // 定期補充タスク
        startRestockTask();
    }
    
    public static ShopInventoryManager getInstance() {
        return instance;
    }
    
    /**
     * データファイル初期化
     */
    private void initDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        dataFile = new File(plugin.getDataFolder(), "shop_inventory.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Shop在庫データファイルの作成に失敗しました");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    /**
     * 静的商品の登録（無限在庫）
     */
    private void registerStaticProducts() {
        // 作業員（採掘場）- ツール類を安価で販売
        addStaticStock(TradingPostType.OFFICIAL, new ItemStack(Material.WOODEN_PICKAXE), 5.0);
        addStaticStock(TradingPostType.OFFICIAL, new ItemStack(Material.STONE_PICKAXE), 15.0);
        addStaticStock(TradingPostType.OFFICIAL, new ItemStack(Material.IRON_PICKAXE), 50.0);
        addStaticStock(TradingPostType.OFFICIAL, DirtBundle.createDirtBundle(500), 10.0);
        addStaticStock(TradingPostType.OFFICIAL, new ItemStack(Material.BOWL), 3.0);
        
        // 商人ギルド - 高級ツール
        addStaticStock(TradingPostType.MERCHANT_GUILD, DirtBundle.createDirtBundle(1000), 80.0);
        addStaticStock(TradingPostType.MERCHANT_GUILD, new ItemStack(Material.IRON_PICKAXE), 100.0);
        addStaticStock(TradingPostType.MERCHANT_GUILD, new ItemStack(Material.DIAMOND_PICKAXE), 500.0);
        
        // 王立銀行
        addStaticStock(TradingPostType.BANK, new ItemStack(Material.DIAMOND_PICKAXE), 500.0);
    }
    
    /**
     * 静的商品追加
     */
    private void addStaticStock(TradingPostType shopType, ItemStack item, double price) {
        ShopStock stock = new ShopStock(shopType, item, price, true);
        allStocks.get(shopType).add(stock);
    }
    
    /**
     * 動的商品の初期生成
     */
    private void generateInitialDynamicStocks() {
        // 各店舗に初期在庫を生成
        for (TradingPostType type : TradingPostType.values()) {
            int initialCount;
            
            // 店舗ごとの初期在庫数
            switch (type) {
                case OFFICIAL:
                    // 作業員: 静的商品のみなので動的在庫なし
                    initialCount = 0;
                    break;
                case MERCHANT_GUILD:
                    // 商人ギルド: 5-8個
                    initialCount = 5 + (int)(Math.random() * 4);
                    break;
                case BLACK_MARKET:
                    // 闇市場: 3-7個
                    initialCount = 3 + (int)(Math.random() * 5);
                    break;
                default:
                    // その他: 3-7個
                    initialCount = 3 + (int)(Math.random() * 5);
                    break;
            }
            
            for (int i = 0; i < initialCount; i++) {
                generateDynamicStock(type);
            }
        }
        
        saveData();
    }
    
    /**
     * 動的商品を1つ生成
     */
    private ShopStock generateDynamicStock(TradingPostType shopType) {
        GoldRushCore core = Main.instance.getGoldRushCore();
        
        // 店舗に応じた商品生成
        switch (shopType) {
            case OFFICIAL:
                // 作業員（採掘場）: 動的商品なし（ツールのみ）
                return null;
                
            case MERCHANT_GUILD:
                // 商人ギルド: 誠実な金塊・金板（純度75-90%）
                return generateMerchantGuildGold(shopType, core);
                
            case BANK:
                // 王立銀行: 装飾品
                return generateJewelryStock(shopType, core);
                
            case BLACK_MARKET:
                // 闇市場: 大型で不純物多い金塊（純度40-95%、ギャンブル性）
                return generateBlackMarketGold(shopType, core);
                
            case SUSPICIOUS_DEALER:
                // 怪しい商人: 超割安の怪しい金塊
                return generateSuspiciousGoldStock(shopType, core);
                
            default:
                return null;
        }
    }
    
    /**
     * 商人ギルド用金塊生成（誠実・中サイズ・純度75-90%）
     */
    private ShopStock generateMerchantGuildGold(TradingPostType shopType, GoldRushCore core) {
        // 重量: 50-200g（中サイズ）
        double pureWeight = 50 + Math.random() * 150;
        
        // 純度: 75-90%（誠実だが純金には届かない）
        double purity = 0.75 + Math.random() * 0.15;
        
        double totalWeight = pureWeight / purity;
        double impurities = totalWeight - pureWeight;
        
        // 金塊、小塊、金板から選択
        GoldType type;
        if (totalWeight < 100) {
            type = GoldType.SMALL_NUGGET;
        } else if (totalWeight < 200) {
            type = GoldType.NUGGET;
        } else {
            type = GoldType.SHEET;
        }
        
        ItemStack item = createGoldItem(type, pureWeight, impurities, false);
        double price = core.getSellPrice(shopType, type) * totalWeight;
        
        ShopStock stock = new ShopStock(shopType, item, price);
        allStocks.get(shopType).add(stock);
        
        return stock;
    }
    
    /**
     * 闇市場用金塊生成（大型・不純物多め・ギャンブル性）
     */
    private ShopStock generateBlackMarketGold(TradingPostType shopType, GoldRushCore core) {
        // 重量: 100-500g（大型）
        double pureWeight = 100 + Math.random() * 400;
        
        // 純度: 基本は40-70%、稀に80-95%の大当たり
        double purity;
        if (Math.random() < 0.15) {
            // 15%の確率で高純度（80-95%）
            purity = 0.80 + Math.random() * 0.15;
        } else {
            // 85%の確率で低純度（40-70%）
            purity = 0.40 + Math.random() * 0.30;
        }
        
        double totalWeight = pureWeight / purity;
        double impurities = totalWeight - pureWeight;
        
        // インゴット、キューブから選択
        GoldType type;
        if (totalWeight < 250) {
            type = GoldType.INGOT;
        } else {
            type = GoldType.CUBE;
        }
        
        ItemStack item = createGoldItem(type, pureWeight, impurities, true); // 違法フラグ
        
        // 価格は時価 × 1.2（闇レートで高め）
        double price = core.getSellPrice(shopType, type) * totalWeight * 1.2;
        
        ShopStock stock = new ShopStock(shopType, item, price);
        allStocks.get(shopType).add(stock);
        
        return stock;
    }
    
    /**
     * 純金在庫生成
     */
    @SuppressWarnings("unused")
	private ShopStock generatePureGoldStock(TradingPostType shopType, GoldRushCore core) {
        double weight = 50 + Math.random() * 200; // 50-250g
        GoldType type = randomGoldType(false);
        
        ItemStack item = createGoldItem(type, weight, 0.0, false);
        double price = core.getSellPrice(shopType, type) * weight;
        
        ShopStock stock = new ShopStock(shopType, item, price);
        allStocks.get(shopType).add(stock);
        
        return stock;
    }
        
    /**
     * 装飾品生成
     */
    private ShopStock generateJewelryStock(TradingPostType shopType, GoldRushCore core) {
        double weight = 50 + Math.random() * 150; // 50-200g
        
        ItemStack item = randomJewelry(weight);
        double price = core.getSellPrice(shopType, GoldType.JEWELRY) * weight;
        
        ShopStock stock = new ShopStock(shopType, item, price);
        allStocks.get(shopType).add(stock);
        
        return stock;
    }

    /**
     * 怪しい金塊生成
     */
    private ShopStock generateSuspiciousGoldStock(TradingPostType shopType, GoldRushCore core) {
        double pureWeight = 30 + Math.random() * 200; // 30-230g
        double purity = 0.3 + Math.random() * 0.5; // 30-80%
        double totalWeight = pureWeight / purity;
        double impurities = totalWeight - pureWeight;
        GoldType type = randomGoldType(true);
        
        ItemStack item = createGoldItem(type, pureWeight, impurities, true);
        double price = core.getSellPrice(shopType, type) * totalWeight * 0.5; // 50%割引
        
        ShopStock stock = new ShopStock(shopType, item, price);
        allStocks.get(shopType).add(stock);
        
        return stock;
    }
    
    /**
     * ランダムな金タイプ取得
     */
    private GoldType randomGoldType(boolean includeDust) {
        GoldType[] types;
        if (includeDust) {
            types = new GoldType[]{GoldType.DUST, GoldType.SMALL_NUGGET, GoldType.NUGGET};
        } else {
            types = new GoldType[]{GoldType.SMALL_NUGGET, GoldType.NUGGET, GoldType.SHEET, GoldType.INGOT};
        }
        return types[(int)(Math.random() * types.length)];
    }
    
    /**
     * ランダムな装飾品取得
     */
    private ItemStack randomJewelry(double weight) {
        ItemStack[] jewelry = {
            GoldRushItem.WEAPON_1, GoldRushItem.WEAPON_2, GoldRushItem.WEAPON_3,
            GoldRushItem.CROWN_1, GoldRushItem.CROWN_2, GoldRushItem.CROWN_3,
            GoldRushItem.CROWN_4, GoldRushItem.CROWN_5, GoldRushItem.CROWN_6
        };
        ItemStack base = jewelry[(int)(Math.random() * jewelry.length)].clone();
        return GoldRushItem.setGold(base, weight, 0.0, false, false);
    }
    
    /**
     * 金アイテム作成
     */
    private ItemStack createGoldItem(GoldType type, double goldWeight, double impurities, boolean isIllegal) {
        ItemStack base;
        switch (type) {
            case DUST: base = GoldRushItem.INGOT_1.clone(); break;
            case SMALL_NUGGET: base = GoldRushItem.INGOT_2.clone(); break;
            case NUGGET: base = GoldRushItem.INGOT_3.clone(); break;
            case SHEET: base = GoldRushItem.INGOT_4.clone(); break;
            case INGOT: base = GoldRushItem.INGOT_5.clone(); break;
            default: base = GoldRushItem.INGOT_3.clone();
        }
        return GoldRushItem.setGold(base, goldWeight, impurities, false, isIllegal);
    }
    
    /**
     * 定期補充タスク開始
     */
    private void startRestockTask() {
        restockTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TradingPostType type : TradingPostType.values()) {
                restockShop(type);
            }
            saveData();
        }, RESTOCK_INTERVAL, RESTOCK_INTERVAL);
    }
    
    /**
     * 店舗の補充処理
     */
    private void restockShop(TradingPostType shopType) {
        List<ShopStock> stocks = allStocks.get(shopType);
        
        // 売れ残りを削除
        stocks.removeIf(stock -> stock.isStale());
        
        // 動的商品の数をカウント
        long dynamicCount = stocks.stream().filter(ShopStock::isDynamic).count();
        
        // 作業員は補充しない（静的商品のみ）
        if (shopType == TradingPostType.OFFICIAL) {
            return;
        }
        
        // 空き枠があれば補充
        int emptySlots = MAX_DYNAMIC_SLOTS - (int)dynamicCount;
        if (emptySlots > 0) {
            int restockCount;
            
            // 店舗ごとの補充確率と数
            switch (shopType) {
                case MERCHANT_GUILD:
                    // 商人ギルド: 積極的に補充（80%確率、1-3個）
                    restockCount = Math.min(emptySlots, 1 + (int)(Math.random() * 3));
                    for (int i = 0; i < restockCount; i++) {
                        if (Math.random() < 0.8) {
                            generateDynamicStock(shopType);
                        }
                    }
                    break;
                    
                case BLACK_MARKET:
                    // 闇市場: 不定期補充（40%確率、1-2個）
                    restockCount = Math.min(emptySlots, 1 + (int)(Math.random() * 2));
                    for (int i = 0; i < restockCount; i++) {
                        if (Math.random() < 0.4) {
                            generateDynamicStock(shopType);
                        }
                    }
                    break;
                    
                default:
                    // その他: 標準補充（60%確率、1-3個）
                    restockCount = Math.min(emptySlots, 1 + (int)(Math.random() * 3));
                    for (int i = 0; i < restockCount; i++) {
                        if (Math.random() < 0.6) {
                            generateDynamicStock(shopType);
                        }
                    }
                    break;
            }
            
            // 補充があれば通知
            if (restockCount > 0) {
                notifyStockChanged(shopType);
            }
        }
    }
    
    /**
     * 在庫一覧取得
     */
    public List<ShopStock> getStocks(TradingPostType shopType) {
        return new ArrayList<>(allStocks.get(shopType));
    }
    
    /**
     * 在庫存在チェック
     */
    public boolean stockExists(UUID stockId) {
        for (List<ShopStock> stocks : allStocks.values()) {
            for (ShopStock stock : stocks) {
                if (stock.getStockId().equals(stockId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 在庫取得
     */
    public ShopStock getStock(UUID stockId) {
        for (List<ShopStock> stocks : allStocks.values()) {
            for (ShopStock stock : stocks) {
                if (stock.getStockId().equals(stockId)) {
                    return stock;
                }
            }
        }
        return null;
    }
    
    /**
     * 購入試行（排他制御）
     */
    public synchronized boolean tryPurchase(UUID stockId, Player buyer) {
        ShopStock stock = getStock(stockId);
        
        if (stock == null) {
            return false; // 既に売り切れ
        }
        
        // 静的商品は削除しない（無限在庫）
        if (!stock.isDynamic()) {
            return true;
        }
        
        // 動的商品は在庫削除
        List<ShopStock> stocks = allStocks.get(stock.getShopType());
        stocks.remove(stock);
        
        // 在庫変更通知
        notifyStockChanged(stock.getShopType());
        
        // データ保存
        saveData();
        
        plugin.getLogger().info(buyer.getName() + " が " + stock.getShopType().getName() + 
                               " で商品を購入: $" + String.format("%.2f", stock.getPrice()));
        
        return true;
    }
    
    /**
     * ビューアー追加
     */
    public void addViewer(Player player, TradingPostType shopType, int page) {
        activeViewers.put(player, new ShopViewData(shopType, page));
    }
    
    /**
     * ビューアー削除
     */
    public void removeViewer(Player player) {
        activeViewers.remove(player);
    }
    
    /**
     * プレイヤーの現在のページ取得
     */
    public int getCurrentPage(Player player) {
        ShopViewData data = activeViewers.get(player);
        if (data == null) return 0;
        return data.currentPage;
    }
    
    /**
     * 在庫変更通知（遅延付き一括更新）
     */
    public void notifyStockChanged(TradingPostType shopType) {
        // 既に更新予約があればキャンセル
        if (pendingUpdates.containsKey(shopType)) {
            pendingUpdates.get(shopType).cancel();
        }
        
        // 0.5秒後に一括更新
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateAllViewers(shopType);
            pendingUpdates.remove(shopType);
        }, 10L);
        
        pendingUpdates.put(shopType, task);
    }
    
    /**
     * 全ビューアーのGUI更新
     */
    private void updateAllViewers(TradingPostType shopType) {
        for (Map.Entry<Player, ShopViewData> entry : new HashMap<>(activeViewers).entrySet()) {
            Player player = entry.getKey();
            ShopViewData data = entry.getValue();
            
            if (!player.isOnline()) {
                activeViewers.remove(player);
                continue;
            }
            
            if (data.shopType == shopType) {
                // 同じ店舗を見ているプレイヤーのGUIを更新
                ShopMenu.refreshGUI(player, shopType, data.currentPage);
            }
        }
    }
    
    /**
     * データ保存
     */
    private void saveData() {
        dataConfig = new YamlConfiguration();
        
        for (Map.Entry<TradingPostType, List<ShopStock>> entry : allStocks.entrySet()) {
            TradingPostType type = entry.getKey();
            List<ShopStock> stocks = entry.getValue();
            
            int index = 0;
            for (ShopStock stock : stocks) {
                // 静的商品は保存しない
                if (!stock.isDynamic()) continue;
                
                String path = type.name() + "." + index;
                dataConfig.set(path + ".stockId", stock.getStockId().toString());
                dataConfig.set(path + ".item", stock.getItem());
                dataConfig.set(path + ".price", stock.getPrice());
                dataConfig.set(path + ".addedTime", stock.getAddedTime());
                dataConfig.set(path + ".slotIndex", stock.getSlotIndex());
                
                index++;
            }
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Shop在庫データの保存に失敗しました");
            e.printStackTrace();
        }
    }
    
    /**
     * データ読み込み
     */
    private void loadData() {
        for (TradingPostType type : TradingPostType.values()) {
            ConfigurationSection section = dataConfig.getConfigurationSection(type.name());
            if (section == null) continue;
            
            for (String key : section.getKeys(false)) {
                String path = type.name() + "." + key;
                
                UUID stockId = UUID.fromString(dataConfig.getString(path + ".stockId"));
                ItemStack item = dataConfig.getItemStack(path + ".item");
                double price = dataConfig.getDouble(path + ".price");
                long addedTime = dataConfig.getLong(path + ".addedTime");
                int slotIndex = dataConfig.getInt(path + ".slotIndex");
                
                ShopStock stock = new ShopStock(stockId, type, item, price, addedTime, slotIndex, true);
                allStocks.get(type).add(stock);
            }
        }
        
        plugin.getLogger().info("Shop在庫データを読み込みました");
    }
    
    /**
     * アンロード
     */
    public void unload() {
        // タスクキャンセル
        if (restockTask != null) {
            restockTask.cancel();
        }
        
        for (BukkitTask task : pendingUpdates.values()) {
            task.cancel();
        }
        
        // データ保存
        saveData();
        
        instance = null;
    }
    
    /**
     * ビューアー情報
     */
    public static class ShopViewData {
        public TradingPostType shopType;
        public int currentPage;
        
        public ShopViewData(TradingPostType shopType, int currentPage) {
            this.shopType = shopType;
            this.currentPage = currentPage;
        }
    }
}