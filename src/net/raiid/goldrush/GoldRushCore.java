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
        
        //int purity = calculatePurityPercent(totalWeight, pureGoldWeight);
        
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
        // === 供給増加（価格下落） ===
        NEARBY_STRIKE(
            "近隣鉱山での大当たり",
            "隣の谷で大規模な金脈が見つかったらしい。町に金が溢れています",
            -0.25,
            30 * 60 * 1000
        ),
        CLAIM_JUMPERS(
            "鉱区荒らしの横行",
            "鉱区荒らしが増え、盗まれた金が闇市場に流れています",
            -0.15,
            20 * 60 * 1000
        ),
        NORTHERN_DISCOVERY(
            "北部での金脈発見",
            "遥か北の山脈で新たな金脈が発見されたとの報せが届きました",
            -0.20,
            25 * 60 * 1000
        ),
        DOWNSTREAM_GOLD(
            "下流での砂金ラッシュ",
            "数日の道のりの下流で大量の砂金が見つかったそうです",
            -0.18,
            20 * 60 * 1000
        ),
        SOUTHERN_SMUGGLING(
            "南部からの密輸",
            "国境の向こうから大量の金が密輸されているようです",
            -0.22,
            25 * 60 * 1000
        ),
        CALIFORNIA_BOOM(
            "カリフォルニアの好景気",
            "西海岸で金が溢れ、その影響がこちらにも及んでいます",
            -0.28,
            30 * 60 * 1000
        ),
        CLAIM_SALE_RUSH(
            "採掘権の投げ売り",
            "遠方の採掘者たちが権利を手放し、金が市場に流れ込んでいます",
            -0.16,
            20 * 60 * 1000
        ),
        
        // === 需要増加（価格高騰） ===
        RAILROAD_AGENT(
            "鉄道会社の買付人",
            "大陸横断鉄道の買付人が町に到着。資金調達のため金を買い漁っています",
            0.35,
            20 * 60 * 1000
        ),
        EASTERN_BUYER(
            "東部の大富豪",
            "東海岸の富豪が装飾品を求めて高値で買い取っています",
            0.30,
            15 * 60 * 1000
        ),
        BANK_PANIC(
            "銀行取り付け騒ぎ",
            "東部で銀行が破綻したという噂。現金の代わりに金が求められています",
            0.40,
            25 * 60 * 1000
        ),
        WEDDING_SEASON(
            "結婚シーズン",
            "町に裕福な家族が集まり、結婚指輪の需要が高まっています",
            0.20,
            15 * 60 * 1000
        ),
        EUROPEAN_DELEGATION(
            "欧州使節団の訪問",
            "遥か海を越えてきた使節団が贈答品として金製品を買い求めています",
            0.28,
            20 * 60 * 1000
        ),
        FEDERAL_RESERVE_RUMOR(
            "政府の金買い上げ",
            "連邦政府が金の備蓄を増やすという噂が広まっています",
            0.25,
            30 * 60 * 1000
        ),
        MILITARY_CONTRACT(
            "軍からの調達",
            "陸軍が勲章用の金を大量発注しているとの情報です",
            0.32,
            20 * 60 * 1000
        ),
        EXHIBITION_DEMAND(
            "万国博覧会の準備",
            "遠方で開催される博覧会向けに金製品の注文が殺到しています",
            0.26,
            25 * 60 * 1000
        ),
        
        // === 供給減少（価格上昇） ===
        DISTANT_COLLAPSE(
            "他州での鉱山事故",
            "他州の主要鉱山で事故が発生。全体の供給が減少しています",
            0.20,
            30 * 60 * 1000
        ),
        BANDIT_RAID(
            "盗賊団の襲撃",
            "盗賊団が輸送馬車を襲撃！流通が滞っています",
            0.25,
            25 * 60 * 1000
        ),
        MOUNTAIN_FLOOD(
            "山間部の洪水",
            "山奥の採掘場が洪水被害を受け、作業が中断しています",
            0.22,
            25 * 60 * 1000
        ),
        UNION_STRIKE(
            "鉱山労働者のストライキ",
            "遠方の鉱山で待遇改善を求めるストライキが発生しています",
            0.18,
            20 * 60 * 1000
        ),
        NATIVE_CONFLICT(
            "先住民との衝突",
            "辺境地域で先住民との衝突が激化。採掘が危険になっています",
            0.24,
            30 * 60 * 1000
        ),
        EQUIPMENT_SHORTAGE(
            "採掘道具の不足",
            "東部の工場火災で採掘道具が不足。作業効率が低下しています",
            0.16,
            25 * 60 * 1000
        ),
        
        // === 法執行・治安 ===
        MARSHAL_PATROL(
            "保安官の巡回強化",
            "連邦保安官が管轄地域を巡回中。取り締まりが強化されています",
            0.10,
            20 * 60 * 1000
        ),
        GANG_WARFARE(
            "ギャング抗争",
            "近隣地域で無法者同士の抗争が激化しています",
            0.15,
            25 * 60 * 1000
        ),
        VIGILANTE_JUSTICE(
            "自警団の活動",
            "町の自警団が活動を強化。怪しい取引が監視されています",
            0.08,
            20 * 60 * 1000
        ),
        
        // === 社交・娯楽 ===
        HIGH_STAKES_POKER(
            "高額ポーカー大会",
            "酒場で高額賞金のポーカー大会。勝者が金を求めています",
            0.15,
            15 * 60 * 1000
        ),
        SALOON_INCIDENT(
            "酒場での騒動",
            "酒場で大乱闘が発生。一部の商人が取引を見合わせています",
            -0.10,
            10 * 60 * 1000
        ),
        CARNIVAL_ARRIVAL(
            "興行一座の来訪",
            "遠方から興行一座が訪れ、興行収入の金を両替しています",
            0.12,
            15 * 60 * 1000
        ),
        HORSE_RACE(
            "競馬大会",
            "近隣の町で大規模な競馬大会。賭けに勝った者が金を求めています",
            0.14,
            15 * 60 * 1000
        ),
        
        // === その他 ===
        COUNTERFEIT_PANIC(
            "偽金騒動",
            "偽造金貨が出回っているとの噂。取引が慎重になっています",
            -0.12,
            20 * 60 * 1000
        ),
        ASSAYER_SHORTAGE(
            "鑑定士不足",
            "熟練の鑑定士が引退し、純金の検査が滞っています",
            -0.08,
            15 * 60 * 1000
        ),
        MINING_CONVENTION(
            "採掘者会議",
            "地域の採掘者たちが集まり、金の売買が活発化しています",
            0.08,
            20 * 60 * 1000
        ),
        BLIZZARD_BLOCKADE(
            "猛吹雪による封鎖",
            "記録的な吹雪で山道が封鎖。物資の輸送が困難です",
            0.18,
            30 * 60 * 1000
        ),
        DROUGHT_IMPACT(
            "干ばつの影響",
            "長期の干ばつで川の水位が下がり、水を使う精錬作業が困難です",
            0.14,
            25 * 60 * 1000
        ),
        SPECULATOR_FRENZY(
            "投機筋の暴走",
            "東部の投機家たちが金に殺到。価格が不安定になっています",
            0.30,
            20 * 60 * 1000
        ),
        NEWSPAPER_SCANDAL(
            "新聞スキャンダル",
            "有力紙が金市場の不正を報道。取引が一時的に冷え込んでいます",
            -0.14,
            15 * 60 * 1000
        ),
        TELEGRAPH_NEWS(
            "電信による好報",
            "電信で届いた東部の好景気の報せで、金への期待が高まっています",
            0.22,
            20 * 60 * 1000
        ),
        TERRITORIAL_DISPUTE(
            "領土紛争",
            "近隣準州での領土紛争により、金の流通が混乱しています",
            0.16,
            25 * 60 * 1000
        ),
        MERCHANT_CARAVAN(
            "大規模キャラバン",
            "中国系商人の大キャラバンが通過中。金の需要が増えています",
            0.20,
            15 * 60 * 1000
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