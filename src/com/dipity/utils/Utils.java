package com.dipity.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import android.content.Context;

public class Utils {
	
	private static final Random rand = new Random();
	private static final String[] RANDOM_QUERY = {
		"restaurant", "food"
	};
	
	private static final double MILE_IN_METER = 1609.34;
	
	public static double getTimeTillClose(ArrayList<Double> intervals, double distance) {
		Calendar c = Calendar.getInstance();
		double now = c.get(Calendar.HOUR_OF_DAY);
		now += c.get(Calendar.MINUTE) / 60.0;
		
		double timeToGetThere = (distance / MILE_IN_METER) * 0.5;
		now += timeToGetThere;
		if (now > 24) {
			// If we are going to break something, better not do it...
			now -= timeToGetThere;
		}
		
		final int len = intervals.size();
		for (int i = 0; i < len; i += 2) {
			double t1 = intervals.get(i);
			double t2 = intervals.get(i + 1);
			
			if (t1 >= t2) {
				// This must mean t2 is part of the NEXT DAY
				t2 += 24;
			}
			
			if (now >= t1 && now <= t2) {
				return t2 - now;
			}
		}
		
		return 0;
	}
	
	public static String getRandomQuery() {
		return RANDOM_QUERY[rand.nextInt(RANDOM_QUERY.length)];
	}
	
	public static int convertToPixels(Context context, int dps) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dps * scale + 0.5f);
	}
}
