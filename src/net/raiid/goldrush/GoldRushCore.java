package net.raiid.goldrush;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GoldRushCore {
    
    private final JavaPlugin plugin;
    private static GoldRushCore instance;
    
    // 価格関連（ドル表示）
    private static final double BASE_PRICE = 1.0; // 基準価格 $1.0/g
    private static final double MIN_PRICE = 0.8;  // 最低価格 $0.8/g
    private static final double MAX_PRICE = 1.2;  // 最高価格 $1.2/g
    
    private double currentRate = 1.0;
    private double buySpread = 1.15;
    
    // イベント関連
    private PriceEvent currentEvent = null;
    private long eventEndTime = 0;
    
    // 需給関連
    private double totalSoldToday = 0.0;
    private long lastDayResetTime = System.currentTimeMillis();
    
    // 価格更新管理
    private long lastPriceUpdateTime = System.currentTimeMillis();
    private static final long PRICE_UPDATE_INTERVAL = 5 * 60 * 1000; // 5分
    
    // イベント発生管理
    private long lastEventCheckTime = System.currentTimeMillis();
    private static final long EVENT_CHECK_INTERVAL = 60 * 60 * 1000; // 1時間
    
    // スケジューラ
    private BukkitTask mainTask;
    
    private Random random = new Random();
    
    // データファイル
    private File dataFile;
    private FileConfiguration dataConfig;
    
    // コンストラクタ
    public GoldRushCore(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        
        // データファイル初期化
        initDataFile();
        
        // データ読み込み
        loadData();
        
        // 1秒ごとにチェック(軽量)
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // 5分ごとの価格更新チェック
            if (currentTime - lastPriceUpdateTime >= PRICE_UPDATE_INTERVAL) {
                updatePrice();
                lastPriceUpdateTime = currentTime;
                saveData();
            }
            
            // 1時間ごとのイベントチェック
            if (currentTime - lastEventCheckTime >= EVENT_CHECK_INTERVAL) {
                checkRandomEvent();
                lastEventCheckTime = currentTime;
                saveData();
            }
            
            // 1日ごとのリセット(24時間)
            if (currentTime - lastDayResetTime >= 24 * 60 * 60 * 1000) {
                resetDailyStats();
                lastDayResetTime = currentTime;
                saveData();
            }
            
            // イベント終了チェック
            if (currentEvent != null && currentTime >= eventEndTime) {
                broadcastEventEnd();
                currentEvent = null;
                saveData();
            }
        }, 0L, 20L); // 1秒ごと
    }
    
    // データファイル初期化
    private void initDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        dataFile = new File(plugin.getDataFolder(), "goldmarket.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("データファイルの作成に失敗しました");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    // データ読み込み
    private void loadData() {
        currentRate = dataConfig.getDouble("currentRate", 1.0);
        lastPriceUpdateTime = dataConfig.getLong("lastPriceUpdateTime", System.currentTimeMillis());
        lastEventCheckTime = dataConfig.getLong("lastEventCheckTime", System.currentTimeMillis());
        lastDayResetTime = dataConfig.getLong("lastDayResetTime", System.currentTimeMillis());
        totalSoldToday = dataConfig.getDouble("totalSoldToday", 0.0);
        
        // イベント復元
        if (dataConfig.contains("currentEvent")) {
            String eventName = dataConfig.getString("currentEvent");
            eventEndTime = dataConfig.getLong("eventEndTime", 0);
            
            try {
                currentEvent = PriceEvent.valueOf(eventName);
                
                // イベントがすでに終了していたらクリア
                if (System.currentTimeMillis() >= eventEndTime) {
                    currentEvent = null;
                    eventEndTime = 0;
                } else {
                    plugin.getLogger().info("イベント復元: " + currentEvent.getName() + 
                        " (残り" + ((eventEndTime - System.currentTimeMillis()) / 60000) + "分)");
                }
            } catch (IllegalArgumentException e) {
                currentEvent = null;
                eventEndTime = 0;
            }
        }
        
        plugin.getLogger().info("金市場データを読み込みました");
        plugin.getLogger().info("現在の価格: $" + String.format("%.2f", getBuyPrice()) + "/g");
    }
    
    // データ保存
    private void saveData() {
        dataConfig.set("currentRate", currentRate);
        dataConfig.set("lastPriceUpdateTime", lastPriceUpdateTime);
        dataConfig.set("lastEventCheckTime", lastEventCheckTime);
        dataConfig.set("lastDayResetTime", lastDayResetTime);
        dataConfig.set("totalSoldToday", totalSoldToday);
        
        if (currentEvent != null) {
            dataConfig.set("currentEvent", currentEvent.name());
            dataConfig.set("eventEndTime", eventEndTime);
        } else {
            dataConfig.set("currentEvent", null);
            dataConfig.set("eventEndTime", 0);
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("データの保存に失敗しました");
            e.printStackTrace();
        }
    }
    
    // インスタンス取得
    public static GoldRushCore getInstance() {
        return instance;
    }
    
    // アンロード
    public static void unload() {
        if (instance != null) {
            // データ保存
            instance.saveData();
            
            // タスクキャンセル
            if (instance.mainTask != null) {
                instance.mainTask.cancel();
            }
            
            instance = null;
        }
    }
    
    // === 価格更新 ===
    private void updatePrice() {
        double oldRate = currentRate;
        
        // 1. 時間経過による変動(±3-8%)
        double timeFluctuation = (0.03 + (random.nextDouble() * 0.05)) * (random.nextBoolean() ? 1 : -1);
        
        // 2. 需給による変動
        double demandFluctuation = calculateDemandFluctuation();
        
        // 3. イベントによる変動
        double eventFluctuation = 0.0;
        if (currentEvent != null && System.currentTimeMillis() < eventEndTime) {
            eventFluctuation = currentEvent.getEffect();
        }
        
        // 合計変動
        double totalChange = timeFluctuation + demandFluctuation + eventFluctuation;
        currentRate += totalChange;
        
        // 価格範囲の制限
        if (currentRate < (MIN_PRICE / BASE_PRICE)) {
            currentRate = MIN_PRICE / BASE_PRICE;
        } else if (currentRate > (MAX_PRICE / BASE_PRICE)) {
            currentRate = MAX_PRICE / BASE_PRICE;
        }
        
        // ログ
        String arrow = (currentRate > oldRate) ? "↑" : (currentRate < oldRate) ? "↓" : "→";
        plugin.getLogger().info("価格更新: $" + String.format("%.2f", oldRate * BASE_PRICE) + " " + arrow + " $" + 
                                String.format("%.2f", currentRate * BASE_PRICE));
    }
    
    // 需給による変動計算
    private double calculateDemandFluctuation() {
        if (totalSoldToday > 1000) {
            return -0.05;
        } else if (totalSoldToday > 500) {
            return -0.02;
        } else if (totalSoldToday < 100) {
            return 0.03;
        } else if (totalSoldToday < 300) {
            return 0.01;
        }
        
        return 0.0;
    }
    
    // === イベント処理 ===
    private void checkRandomEvent() {
        // すでにイベント中なら何もしない
        if (currentEvent != null && System.currentTimeMillis() < eventEndTime) {
            return;
        }
        
        // 20%の確率でイベント発生
        if (random.nextDouble() < 0.2) {
            triggerRandomEvent();
        }
    }
    
    private void triggerRandomEvent() {
        PriceEvent[] events = PriceEvent.values();
        currentEvent = events[random.nextInt(events.length)];
        eventEndTime = System.currentTimeMillis() + currentEvent.getDuration();
        
        broadcastEvent();
        
        plugin.getLogger().info("イベント発生: " + currentEvent.getName() + 
            " (期限: " + new java.util.Date(eventEndTime) + ")");
    }
    
    private void broadcastEvent() {
        String message = currentEvent.getMessage();
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "⚠ " + ChatColor.BOLD + "緊急速報!");
        Bukkit.broadcastMessage(ChatColor.WHITE + message);
        
        long remainingMinutes = (eventEndTime - System.currentTimeMillis()) / 60000;
        
        if (currentEvent.getEffect() > 0) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "→ 金価格が上昇します! (残り" + remainingMinutes + "分)");
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "→ 金価格が下落します! (残り" + remainingMinutes + "分)");
        }
        
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
    }
    
    private void broadcastEventEnd() {
        if (currentEvent != null) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "[速報] " + currentEvent.getName() + "の影響が収まりました");
        }
    }
    
    // === 統計リセット ===
    private void resetDailyStats() {
        plugin.getLogger().info("1日の売却統計をリセット: " + String.format("%.2f", totalSoldToday) + "g");
        totalSoldToday = 0.0;
    }
    
    // === 金のタイプ ===
    public enum GoldType {
        DUST("砂金", "dust", 1.0),
        SMALL_NUGGET("小塊", "small_nugget", 1.05),
        NUGGET("金塊", "nugget", 1.10),
        SHEET("金板", "sheet", 1.15),
        INGOT("インゴット", "ingot", 1.20),
        CUBE("立方体", "cube", 1.25),
        BLOCK("ブロック", "block", 1.30),
        JEWELRY("装飾品", "jewelry", 1.50);

    	public static GoldType labelOf(String nbtLabel) {
    		if (nbtLabel == null) return null;
    		for (GoldType type : values()) {
    			if (type.getNBTLabel().equalsIgnoreCase(nbtLabel))
    				return type;
    		}
    		return null;
    	}
        
        private final String displayName;
        private final String nbtLabel;
        private final double multiplier; // 価格倍率
        
        GoldType(String displayName, String nbtLabel, double multiplier) {
            this.displayName = displayName;
            this.nbtLabel = nbtLabel;
            this.multiplier = multiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public String getNBTLabel() { return nbtLabel; }
        public double getMultiplier() { return multiplier; }
    }
    
    // === 取引所の種類 ===
    public enum TradingPostType {
        OFFICIAL("公認取引所", 1.00, 1.15),
        MERCHANT_GUILD("商人ギルド", 1.03, 1.18),
        BANK("王立銀行", 0.98, 1.13),
        BLACK_MARKET("闇市場", 1.15, 1.05),
        SUSPICIOUS_DEALER("怪しい商人", 1.10, 0.90);
        
    	public static TradingPostType nameOf(String name) {
    		for (TradingPostType type : values()) {
    			if (type.getName().equals(name))
    				return type;
    		}
    		return null;
    	}

        private final String name;
        private final double buyMultiplier;   // 買取価格の倍率
        private final double sellMultiplier;  // 販売価格の倍率
        
        TradingPostType(String name, double buyMultiplier, double sellMultiplier) {
            this.name = name;
            this.buyMultiplier = buyMultiplier;
            this.sellMultiplier = sellMultiplier;
        }
        
        public String getName() { return name; }
        public double getBuyMultiplier() { return buyMultiplier; }
        public double getSellMultiplier() { return sellMultiplier; }
    }
    
    // === 取引所ごとの価格取得 ===
    
    /**
     * 指定した取引所での買取価格(プレイヤーが金を売る時)
     * @param postType 取引所の種類
     * @param goldType 金のタイプ
     * @param totalWeight 全体の重量(g)
     * @param pureGoldWeight 純金の重量(g)
     * @return 1gあたりの買取価格
     */
    public double getBuyPrice(TradingPostType postType, GoldType goldType, 
                             double totalWeight, double pureGoldWeight) {
        // 基準価格
        double basePrice = BASE_PRICE * currentRate;
        
        // 純度を計算
        double purity = calculatePurity(totalWeight, pureGoldWeight);
        
        // 純度95%以上なら純金扱い、それ以下なら不純物ありとして減額
        double purityMultiplier;
        if (purity >= 0.95) {
            // 純金: タイプボーナスそのまま
            purityMultiplier = goldType.getMultiplier();
        } else {
            // 不純物あり: タイプボーナス × 0.8
            purityMultiplier = goldType.getMultiplier() * 0.8;
        }
        
        // 取引所の倍率
        double finalPrice = basePrice * purityMultiplier * postType.getBuyMultiplier();
        
        return finalPrice;
    }
    
    /**
     * 指定した取引所での販売価格(プレイヤーが金を買う時)
     * @param postType 取引所の種類
     * @param goldType 金のタイプ
     * @return 1gあたりの販売価格
     */
    public double getSellPrice(TradingPostType postType, GoldType goldType) {
        // 基準価格
        double basePrice = BASE_PRICE * currentRate;
        
        // タイプボーナス
        double typeMultiplier = goldType.getMultiplier();
        
        // 取引所の倍率
        double finalPrice = basePrice * typeMultiplier * postType.getSellMultiplier();
        
        return finalPrice;
    }
    
    /**
     * 指定した取引所で取り扱える金かどうか判定
     * @param postType 取引所の種類
     * @param totalWeight 全体の重量
     * @param pureGoldWeight 純金の重量
     * @return 取り扱い可能ならtrue
     */
    public boolean canTrade(TradingPostType postType, double totalWeight, double pureGoldWeight) {
        int purity = calculatePurityPercent(totalWeight, pureGoldWeight);
        
        switch (postType) {
            case OFFICIAL:
            case MERCHANT_GUILD:
            case BANK:
                // 正規の取引所は純度95%以上のみ
                return purity >= 95;
                
            case BLACK_MARKET:
            case SUSPICIOUS_DEALER:
                // 闇市は何でも買い取る
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * 取引できない理由を取得
     * @param postType 取引所の種類
     * @param totalWeight 全体の重量
     * @param pureGoldWeight 純金の重量
     * @return 取引できない理由(取引可能ならnull)
     */
    public String getTradeRefusalReason(TradingPostType postType, double totalWeight, double pureGoldWeight) {
        if (canTrade(postType, totalWeight, pureGoldWeight)) {
            return null;
        }
        
        int purity = calculatePurityPercent(totalWeight, pureGoldWeight);
        
        switch (postType) {
            case OFFICIAL:
                return "純度が低すぎます。王国公認取引所では純度95%以上の金のみ取り扱います。";
                
            case MERCHANT_GUILD:
                return "この品質では買い取れません。商人ギルドは純度95%以上の金のみ扱います。";
                
            case BANK:
                return "申し訳ございません。当銀行では純度95%以上の金のみお取り扱いしております。";
                
            default:
                return "この金は取り扱えません。";
        }
    }
    
    /**
     * 取引所ごとの最低純度要求を取得
     * @param postType 取引所の種類
     * @return 最低純度(%)
     */
    public int getMinimumPurity(TradingPostType postType) {
        switch (postType) {
            case OFFICIAL:
            case MERCHANT_GUILD:
            case BANK:
                return 95;
                
            case BLACK_MARKET:
            case SUSPICIOUS_DEALER:
                return 0; // 何でもOK
                
            default:
                return 100;
        }
    }
    
    /**
     * 不純物が混じった金の買取価格を計算
     * @param postType 取引所の種類
     * @param goldType 金のタイプ
     * @param totalWeight 金全体の重量(g)
     * @param pureGoldWeight 純金の重量(g)
     * @return 買取価格
     */
    public double getImpureGoldBuyPrice(TradingPostType postType, GoldType goldType,
                                        double totalWeight, double pureGoldWeight) {
        // 1gあたりの価格
        double pricePerGram = getBuyPrice(postType, goldType, totalWeight, pureGoldWeight);
        
        // 純金の重量だけを価値として計算
        return pureGoldWeight * pricePerGram;
    }
    
    /**
     * 純度を取得(0.0〜1.0)
     * @param totalWeight 全体の重量
     * @param pureGoldWeight 純金の重量
     * @return 純度(0.0〜1.0)
     */
    public static double calculatePurity(double totalWeight, double pureGoldWeight) {
        if (totalWeight <= 0) return 0.0;
        return Math.min(1.0, pureGoldWeight / totalWeight);
    }
    
    /**
     * 純度をパーセントで取得
     * @param totalWeight 全体の重量
     * @param pureGoldWeight 純金の重量
     * @return 純度(0〜100)
     */
    public static int calculatePurityPercent(double totalWeight, double pureGoldWeight) {
        return (int) (calculatePurity(totalWeight, pureGoldWeight) * 100);
    }
    
    // === 既存のメソッドは互換性のため残す ===
    
    /**
     * 標準の買取価格(王国公認 + DUST)
     */
    public double getBuyPrice() {
        return BASE_PRICE * currentRate;
    }
    
    /**
     * 標準の販売価格(王国公認 + DUST)
     */
    public double getSellPrice() {
        return BASE_PRICE * currentRate * buySpread;
    }
    
    /**
     * 現在のレート取得
     */
    public double getCurrentRate() {
        return currentRate;
    }
    
    /**
     * 現在のイベント
     */
    public PriceEvent getCurrentEvent() {
        return currentEvent;
    }
    
    /**
     * イベントが有効か
     */
    public boolean isEventActive() {
        return currentEvent != null && System.currentTimeMillis() < eventEndTime;
    }
    
    /**
     * イベントの残り時間(ミリ秒)
     */
    public long getEventRemainingTime() {
        if (!isEventActive()) return 0;
        return eventEndTime - System.currentTimeMillis();
    }
    
    /**
     * 売却記録(需給計算用)
     */
    public void recordSale(double goldAmount) {
        totalSoldToday += goldAmount;
        saveData(); // 即座に保存
        
        plugin.getLogger().info("金売却記録: +" + String.format("%.2f", goldAmount) + "g (合計: " + 
                               String.format("%.2f", totalSoldToday) + "g)");
    }
    
    /**
     * 今日の総売却量
     */
    public double getTotalSoldToday() {
        return totalSoldToday;
    }
    
    /**
     * 管理者用: 価格を強制設定
     */
    public void setCurrentRate(double rate) {
        this.currentRate = Math.max(MIN_PRICE / BASE_PRICE, Math.min(MAX_PRICE / BASE_PRICE, rate));
        saveData();
    }
    
    /**
     * 管理者用: イベント強制発動
     */
    public void triggerEvent(PriceEvent event) {
        currentEvent = event;
        eventEndTime = System.currentTimeMillis() + event.getDuration();
        broadcastEvent();
        saveData();
    }
    
    // === イベントEnum ===
    public enum PriceEvent {
        VEIN_DISCOVERY(
            "金鉱脈発見",
            "大規模な金鉱脈が発見されました!",
            -0.30,
            30 * 60 * 1000
        ),
        KINGDOM_PURCHASE(
            "王国買取強化",
            "王国が金の大量買取を開始しました!",
            0.40,
            20 * 60 * 1000
        ),
        MERCHANT_VISIT(
            "大商人来訪",
            "富豪の商人が高値で金を買い取っています!",
            0.25,
            15 * 60 * 1000
        ),
        BANDIT_RAID(
            "盗賊団襲撃",
            "盗賊団が金を奪って逃走!市場が混乱しています",
            -0.20,
            25 * 60 * 1000
        );
        
        private final String name;
        private final String message;
        private final double effect;
        private final long duration;
        
        PriceEvent(String name, String message, double effect, long duration) {
            this.name = name;
            this.message = message;
            this.effect = effect;
            this.duration = duration;
        }
        
        public String getName() { return name; }
        public String getMessage() { return message; }
        public double getEffect() { return effect; }
        public long getDuration() { return duration; }
    }
}