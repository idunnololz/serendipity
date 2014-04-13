package com.serendipity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;

import com.serendipity.utils.ApiManager;

public class StateManager {
	private Context context;
	private ApiManager apiMgr;

	private static StateManager instance;
	
	private Core core;
	
	private Location lastLocation;
	
	private Executor executor;
	
	private JSONObject result;
	
	private Geocoder geoCoder;
	
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
}
