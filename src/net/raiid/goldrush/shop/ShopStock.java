package net.raiid.goldrush.shop;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import net.raiid.goldrush.GoldRushCore.TradingPostType;

/**
 * Shop在庫アイテムのデータクラス
 */
public class ShopStock {
    
    private final UUID stockId;
    private final TradingPostType shopType;
    private final ItemStack item;
    private final double price;
    private final long addedTime;
    private int slotIndex;
    private final boolean isDynamic; // 動的商品かどうか
    
    /**
     * 動的在庫用コンストラクタ
     */
    public ShopStock(TradingPostType shopType, ItemStack item, double price) {
        this.stockId = UUID.randomUUID();
        this.shopType = shopType;
        this.item = item.clone();
        this.price = price;
        this.addedTime = System.currentTimeMillis();
        this.slotIndex = -1;
        this.isDynamic = true;
    }
    
    /**
     * 静的商品用コンストラクタ（無限在庫）
     */
    public ShopStock(TradingPostType shopType, ItemStack item, double price, boolean isStatic) {
        this.stockId = UUID.randomUUID();
        this.shopType = shopType;
        this.item = item.clone();
        this.price = price;
        this.addedTime = System.currentTimeMillis();
        this.slotIndex = -1;
        this.isDynamic = !isStatic;
    }
    
    /**
     * データ復元用コンストラクタ
     */
    public ShopStock(UUID stockId, TradingPostType shopType, ItemStack item, double price, long addedTime, int slotIndex, boolean isDynamic) {
        this.stockId = stockId;
        this.shopType = shopType;
        this.item = item;
        this.price = price;
        this.addedTime = addedTime;
        this.slotIndex = slotIndex;
        this.isDynamic = isDynamic;
    }
    
    public UUID getStockId() {
        return stockId;
    }
    
    public TradingPostType getShopType() {
        return shopType;
    }
    
    public ItemStack getItem() {
        return item.clone();
    }
    
    public double getPrice() {
        return price;
    }
    
    public long getAddedTime() {
        return addedTime;
    }
    
    public int getSlotIndex() {
        return slotIndex;
    }
    
    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }
    
    public boolean isDynamic() {
        return isDynamic;
    }
    
    /**
     * 追加からの経過時間（ミリ秒）
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - addedTime;
    }
    
    /**
     * 追加からの経過時間（分）
     */
    public long getElapsedMinutes() {
        return getElapsedTime() / (60 * 1000);
    }
    
    /**
     * 売れ残りかどうか（60分経過で売れ残り判定）
     */
    public boolean isStale() {
        return isDynamic && getElapsedMinutes() >= 60;
    }
}