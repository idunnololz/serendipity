package com.dipity;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import com.dipity.utils.LogUtils;

import android.util.Log;

/**
 * Heuristics for determining the best result 
 */
public class Core {
	private static final String TAG = Core.class.getSimpleName();
	
	private Random rand = new Random();
	private JSONArray dataset;
	
	private static final double WEIGHT_DISTANCE = 0.79;
	private static final double WEIGHT_OPEN = 0.05;
	private static final double WEIGHT_PRICE = 0.01;
	private static final double WEIGHT_RATING = 0.05;
	private static final double WEIGHT_LUCK = 0.05;

	private static final double MILE_IN_METER = 1609.34;
	
	public Core() {}
	
	public Core setDataSet(JSONArray data) {
		/* Each item in the dataset must have the structure:
		 *{ 
		 *	...
		 *	"distance" : ...,			// this is in meters
		 *	"delta-open-time" : ...,	// this is in hours
		 * 	"rating": ...,				// percentage
		 *  "price": ...,				// percentage 
		 *  ...
		 *}
		 */
		dataset = data;
		return this;
	}
	
	private double calculateHeuristic(JSONObject obj) throws Exception {
		double distance = obj.getDouble("distance");
		double deltaOpenTime = obj.getDouble("delta-open-time");
		double rating = obj.getDouble("rating");
		double price = obj.getDouble("price");
		double h = 
				(1 / ((distance / MILE_IN_METER) * 4 + 1)) * WEIGHT_DISTANCE +
				(1 - 1/((deltaOpenTime - 0.5) / 0.5 * 4 + 1)) * WEIGHT_OPEN +
				((0.5 - price) / 0.5) * WEIGHT_PRICE +
				(1 - (1/(((rating - 3.5) / 1.5) * 4 + 1))) * 1.25 * WEIGHT_RATING +
				(rand.nextDouble()) * WEIGHT_LUCK;
		return h;
	}
	
	public Core filter() throws Exception {
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		final int len = dataset.length();
		for (int i = 0; i < len; i++) {
			JSONObject obj = dataset.getJSONObject(i);
			if (obj.getDouble("delta-open-time") < 0.5) {
				toRemove.add(i);
			} else if (obj.getDouble("rating") < 0.7) {
				toRemove.add(i);
			} else if (obj.getDouble("price") > 0.5) {
				toRemove.add(i);
			}
		}
		
		JSONArray newArr = new JSONArray();
		final int removeLen = toRemove.size();
		if (removeLen == 0) {
			return this;
		}
		
		int n = 0;
		int next = toRemove.get(n);
		for (int i = 0; i < len; i++) {
			if (i != next) {
				newArr.put(dataset.getJSONObject(i));
			} else {
				n++;
				if (n < removeLen) {
					next = toRemove.get(n);
				} else {
					next = -1;
				}
			}
		}
		
		dataset = newArr;
		
		return this;
	}
	
	public JSONObject getTheBest() throws Exception {
		JSONObject theBest = null;
		double bestScore = 0;
		
		// First calculate the heuristics per Object
		final int len = dataset.length();
		for (int i = 0; i < len; i++) {
			JSONObject obj = dataset.getJSONObject(i);
			double score = calculateHeuristic(obj);
			double distance = obj.getDouble("distance");
			
			LogUtils.d(TAG, "name: " + obj.getString("name") + " score: " + score + " dist: " + ((1 / ((distance / MILE_IN_METER) + 4)) * WEIGHT_DISTANCE));
			
			if (score > bestScore) {
				theBest = obj;
				bestScore = score;
			}
		}
		
		return theBest;	
	}
	
}
