package net.raiid.goldrush;

import net.raiid.goldrush.GoldRushCore.GoldType;

//買取依頼システム
@SuppressWarnings("unused")
public class OrderBoard {
    private GoldType requestedType; // JEWELRY
    private double minWeight; // 最低重量
    private double priceMultiplier; // 1.2-1.5倍
	private long expirationTime; // 有効期限
}

// 信用ランク60以上で受注可能
// ランダムに装飾品依頼が生成される