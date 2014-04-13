package com.serendipity.utils;

import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.util.Log;

public class YelpUtils {
	private static final String TAG = YelpUtils.class.getSimpleName();
	
	public static Document scrapeSite(String url) throws IOException {
		return Jsoup.connect(url).get();
	}
	
	/**
	 * Returns the price as a % (for instance $$ would be .5 since max price is $$$$)
	 * @param doc
	 * @return
	 */
	public static double getMoney(Document doc) {
		Elements elements = doc.select("span#price_tip");
		if (elements.size() == 0) {
			Log.e(TAG, "FATAL ERROR: Empty selector when attempting to scrape " + doc.baseUri());
		}
		return elements.get(0).text().length()/ 4.0;
	}
	
	public static ArrayList<Double> getOpenTime(Document doc) {
		Elements elements = doc.select(".biz-details .biz-info .biz-hours");
		if (elements.size() == 0) {
			Log.e(TAG, "FATAL ERROR: Empty selector when attempting to scrape " + doc.baseUri());
			return new ArrayList<Double>();
		}
		return getOpenTime(elements.get(0).toString());
	}
	
	public static ArrayList<Double> getOpenTime(String times) {
		ArrayList<Double> t = new ArrayList<Double>();
		// <li class="biz-hours"> Hours today: 12 pm - 9:30 pm <span class="status closed"> Closed</span> </li>
		for (int i = 0; i < times.length(); i++) {
			char c = times.charAt(i);
			if (c >= '0' && c <= '9') {
				i = getFirstTime(times, i, t);
				
				if (i == -1) {
					Log.e(TAG, "ERROR!");
					break;
				}
			}
		}
		
		return t;
	}
	
	private static int getFirstTime(String string, int startIndex, ArrayList<Double> out) {
		int n = 0;
		final int len = string.length();
		int i;
		for (i = startIndex; i < len; i++) {
			char c = string.charAt(i);
			if (c >= '0' && c <= '9') {
				n *= 10;
				n += c - '0';
				if (n == 12) {
					n = 0;
				}
			} else if (c == ':') {
				break;
			} else if (c == 'a') {
				if (string.charAt(++i) == 'm') {
					out.add((double) (n));
					return ++i;
				} else {
					// We didn't expect this! Whoop whoop whoop whoop whoop whoop
					return -1;
				}
			} else if (c == 'p') {
				if (string.charAt(++i) == 'm') {
					out.add((double) (n + 12));
					return ++i;
				} else {
					// We didn't expect this! Whoop whoop whoop whoop whoop whoop
					return -1;
				}
			}
		}
		
		i++;
		int n2 = 0;
		for (; i < len; i++) {
			char c = string.charAt(i);
			if (c >= '0' && c <= '9') {
				n2 *= 10;
				n2 += c - '0';
			} else if (c == 'a') {
				if (string.charAt(++i) == 'm') {
					out.add((double) (n + (int)(n2 / 60f )));
					return ++i;
				} else {
					// We didn't expect this! Whoop whoop whoop whoop whoop whoop
					return -1;
				}
			} else if (c == 'p') {
				if (string.charAt(++i) == 'm') {
					out.add((double) (n + (int)(n2 / 60f) + 12));
					return ++i;
				} else {
					// We didn't expect this! Whoop whoop whoop whoop whoop whoop
					return -1;
				}
			}
		}
		
		// We should have returned already! Whoop whoop whoop whoop whoop whoop
		return -1;
	}
	
	public static double normalizeRating(double rating) {
		return rating / 5.0;
	}
}
