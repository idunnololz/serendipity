package com.dipity;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.dipity.utils.ApiManager;
import com.dipity.utils.LogUtils;

public class StateManager {
	private static final String TAG = StateManager.class.getSimpleName();
	
	private Context context;
	private ApiManager apiMgr;

	private static StateManager instance;

	private Core core;

	private Location lastLocation;

	private Executor executor;

	private JSONObject result;

	private Geocoder geoCoder;

	private Typeface comicSans;

	public static final int TYPEFACE_COMIC_SANS = 1;
	
	private Random rand = new Random();
	
	public static final String[] DOGE_SPEAK = new String[] {
		"wow", "such search", "much thought", "very food", "so wait"
	};
	
	public static final int[] DOGE_COLOR = new int[] {
		0xFF000000, 0xFF82d94c, 0xFFd74ba2, 0xFFdd4d4c
	};

	private StateManager(Context context) {
		this.context = context;
		apiMgr = new ApiManager(
				context.getString(R.string.key_yelp_consumer), 
				context.getString(R.string.key_yelp_consumer_secret),
				context.getString(R.string.key_yelp_token),
				context.getString(R.string.key_yelp_token_secret));

		geoCoder = new Geocoder(context);

		core = new Core();
		executor = Executors.newCachedThreadPool();
	}

	public static void create(Context context) {
		instance = new StateManager(context);
	}

	public static StateManager get() {
		return instance;
	}

	public ApiManager getApiManager() {
		return apiMgr;
	}

	public Core getCore() {
		return core;
	}

	public void setLastLocation(Location lastLocation) {
		this.lastLocation = lastLocation;
	}

	public Location getLastLocation() {
		return lastLocation;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setResult(JSONObject result) {
		this.result = result;
	}

	public JSONObject getResult() {
		return result;
	}

	public Geocoder getGeocoder() {
		return geoCoder;
	}

	public Typeface getTypeface(int typefaceCode) {
		switch(typefaceCode) {
		case TYPEFACE_COMIC_SANS:
			if (comicSans == null)
				comicSans = Typeface.createFromAsset(context.getAssets(), "fonts/cs.ttf");
			return comicSans;
		default:
			LogUtils.e(TAG, "Typeface with code " + typefaceCode + " not found!");
			return null;
		}
	}
	
	public String getNextDogeSpeak() {
		return DOGE_SPEAK[rand.nextInt(DOGE_SPEAK.length)];
	}
	
	public int getNextDogeColor() {
		return DOGE_COLOR[rand.nextInt(DOGE_COLOR.length)];
	}
	
	public float nextFloat() {
		return rand.nextFloat();
	}
}
