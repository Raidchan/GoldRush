package net.raiid.goldrush;

/**
 * 現在のコードには取引所ごとの価格差は実装されているが、プレイヤーの信用ランクシステムは未実装。
 * PlayerDataにも追加が必要。
 */

public class PlayerReputation {/*
    // 全体信用度 0-100
    private int globalReputation = 50;
    
    // 取引所ごとの信用度
    private Map<GoldRushCore.TradingPostType, Integer> postReputation;
    
    // 信用度による価格倍率
    public double getPriceMultiplier() {
        if (globalReputation >= 80) return 1.05; // +5%
        if (globalReputation >= 60) return 1.0;
        if (globalReputation >= 40) return 0.95;
        if (globalReputation >= 20) return 0.90;
        return 0.85; // -15%
    }
    
    // 取引で上昇（重量に応じて）
    public void onTrade(double goldWeight, boolean isLegal) {
        int gain = (int)(goldWeight / 100.0);
        if (!isLegal) gain /= 2; // 違法取引は半分
        globalReputation = Math.min(100, globalReputation + gain);
    }
    
    // 没収で大幅低下
    public void onConfiscated() {
        globalReputation = Math.max(0, globalReputation - 20);
    }
    
    // 偽物販売でランダム低下
    public void onFakeSold() {
        int penalty = 5 + (int)(Math.random() * 10); // 5-15
        globalReputation = Math.max(0, globalReputation - penalty);
    }
*/
}