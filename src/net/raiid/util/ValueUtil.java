package net.raiid.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ValueUtil {

	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = BigDecimal.valueOf(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP); // 四捨五入
	    return bd.doubleValue();
	}

}
