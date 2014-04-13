package com.serendipity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.serendipity.utils.ApiManager;
import com.serendipity.utils.LogUtils;
import com.serendipity.utils.Utils;
import com.serendipity.utils.YelpUtils;

public class MainFragment extends Fragment{
	private static final String TAG = MainFragment.class.getSimpleName();

	private StateManager stateMgr;
	private ApiManager apiMgr;
	private LocationManager locationMgr;
	private InputMethodManager inputMethodMgr;

	private static final int MIN_TIME_BW_UPDATES = 30000;
	private static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

	private static final int MAX_RESULTS_YELP = 10;

	private Location lastLocation;

	private ImageView bg;
	private ViewGroup queryView;
	private ImageButton btnSearch;
	private EditText txtQuery;
	private View spinnerContainer;
	private RelativeLayout rootView;

	private QueryTask queryTask = new QueryTask();

	private static final int SCALE_DURATION = 60000;
	private static final int ROTATE_DURATION = 1300;
	private static final int FADE_DURATION = 500;

	private Object lock = new Object();

	private boolean shouldResetViews = false;

	private boolean dogeMode = false;

	private LayoutInflater inflater;

	private Thread dogeThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		LogUtils.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		stateMgr = StateManager.get();
		apiMgr = stateMgr.getApiManager();
		locationMgr = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		inputMethodMgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	private boolean queryRunning()  {
		return queryTask != null && !queryTask.isCancelled() && queryTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LogUtils.d(TAG, "onCreateView()");

		this.inflater = inflater;
		rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);

		bg = (ImageView) rootView.findViewById(R.id.bg);
		queryView = (ViewGroup) rootView.findViewById(R.id.queryView);
		btnSearch = (ImageButton) rootView.findViewById(R.id.search);
		txtQuery = (EditText) rootView.findViewById(R.id.query);
		spinnerContainer = rootView.findViewById(R.id.spinnerContainer);

		txtQuery.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					// hide virtual keyboard
					inputMethodMgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), 0);

					doQuery();
					return true;
				}
				return false;
			}

		});

		btnSearch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				doQuery();
			}

		});

		if (queryRunning()) {
			doQueryAnimations();
		}

		if (dogeMode) {
			setupDoge(true);
		}

		return rootView;
	}

	private void doQuery() {
		String query = txtQuery.getText().toString();
		if (query == null || query.length() == 0) {
			query = Utils.getRandomQuery();
		}

		if (query.toLowerCase(Locale.US).equals("doge")) {
			setupDoge();

			txtQuery.getText().clear();
			return;
		}

		LogUtils.d(TAG, "Sending query for " + query);
		if (stateMgr.getLastLocation() != null) {
			if (queryTask != null) {
				queryTask.cancel(true);
			}

			queryTask = new QueryTask();
			queryTask.execute(stateMgr.getLastLocation(), query);
		}

		doQueryAnimations();
	}

	private void doQueryAnimations() {
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out_zoom_in);
		fadeOut.setFillAfter(true);
		fadeOut.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				btnSearch.setClickable(true);
				queryView.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}

		});
		fadeOut.setFillBefore(false);
		fadeOut.setFillAfter(false);
		fadeOut.setFillEnabled(false);
		btnSearch.setClickable(false);
		queryView.startAnimation(fadeOut);

		ScaleAnimation scaleAni = new ScaleAnimation(1, 4f, 1, 4f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
		scaleAni.setDuration(SCALE_DURATION);
		scaleAni.setFillBefore(false);
		scaleAni.setFillAfter(false);
		scaleAni.setFillEnabled(false);
		scaleAni.setInterpolator(new LinearInterpolator());
		bg.startAnimation(scaleAni);

		RotateAnimation spinnerAni = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		spinnerAni.setDuration(ROTATE_DURATION);
		spinnerAni.setRepeatMode(Animation.RESTART);
		spinnerAni.setRepeatCount(Animation.INFINITE);
		spinnerAni.setInterpolator(new LinearInterpolator());

		spinnerContainer.setVisibility(View.VISIBLE);

		AlphaAnimation alphaAni = new AlphaAnimation(0f, 1f);
		alphaAni.setDuration(FADE_DURATION);

		spinnerContainer.startAnimation(alphaAni);

		rootView.findViewById(R.id.spinner).startAnimation(spinnerAni);

		if (dogeMode) {
			// god mode is active!

			Display display = getActivity().getWindowManager().getDefaultDisplay();
			final Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;

			if (dogeThread != null && dogeThread.isAlive()) {

			} else {
				dogeThread = new Thread() {
					@Override
					public void run() {
						// let's fk things up...
						while(queryTask != null && queryTask.getStatus() != null && 
								queryTask.getStatus() != AsyncTask.Status.FINISHED &&
								!queryTask.isCancelled()) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {/* do nothing */}

							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run() {
									final TextView newView = new TextView(getActivity());
									newView.setText(stateMgr.getNextDogeSpeak());
									newView.setTextColor(stateMgr.getNextDogeColor());
									newView.setTextSize(Utils.convertToPixels(getActivity(), (int) (stateMgr.nextFloat() * 10f + 12f)));

									AlphaAnimation ani = new AlphaAnimation(1f, 0f);
									ani.setDuration(2000);
									ani.setAnimationListener(new AnimationListener() {

										@Override
										public void onAnimationEnd(Animation arg0) {
											rootView.removeView(newView);
										}

										@Override
										public void onAnimationRepeat(Animation arg0) {}

										@Override
										public void onAnimationStart(Animation arg0) {}

									});

									newView.setAnimation(ani);

									RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
											RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

									lp.topMargin = (int) (stateMgr.nextFloat() * 0.8f * size.y);
									lp.leftMargin = (int) ((stateMgr.nextFloat() * 0.8f) * size.x);
									newView.setLayoutParams(lp);

									rootView.addView(newView);
								}

							});
						}

					}
				};
				dogeThread.start();
			}
		}

	}

	private void setupDoge() {
		setupDoge(false);
	}

	private void setupDoge(boolean forceSetup) {
		if (!forceSetup && dogeMode) return;

		dogeMode = true;
		bg.setImageResource(R.drawable.doge);

		TextView title = (TextView) rootView.findViewById(R.id.i_want);
		TextView loading = (TextView) rootView.findViewById(R.id.txtLoading);

		Typeface dogeSans = stateMgr.getTypeface(StateManager.TYPEFACE_COMIC_SANS);
		title.setTypeface(dogeSans);
		title.setTextColor(Color.BLACK);
		txtQuery.setTypeface(dogeSans);

		loading.setTypeface(dogeSans);

		Toast.makeText(getActivity(), R.string.doge_toast, Toast.LENGTH_SHORT).show();
	}

	private void resetViews() {
		queryView.setVisibility(View.VISIBLE);
		bg.clearAnimation();
		rootView.findViewById(R.id.spinnerContainer).setVisibility(View.INVISIBLE);
	}

	@Override
	public void onResume() {
		LogUtils.d(TAG, "onResume()");
		
		super.onResume();

		if (shouldResetViews) {
			resetViews();
		}

		Calendar c = Calendar.getInstance();
		int hr = c.get(Calendar.HOUR_OF_DAY);
		if (hr < 6 || hr > 18) {
			// night
			bg.setBackgroundResource(R.drawable.android_bg);
		} else {
			// day
			bg.setBackgroundResource(R.drawable.android_bg_day);
		}

		registerLocationUpdates();
	}

	@Override
	public void onPause() {
		LogUtils.d(TAG, "onPause()");
		
		super.onPause();

		locationMgr.removeUpdates(locationListener);
	}

	private void registerLocationUpdates() {
		Location location = null;

		// getting GPS status
		boolean isGPSEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
		// getting network status
		boolean isNetworkEnabled = locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (!isGPSEnabled && !isNetworkEnabled) {
			// no network provider is enabled
		} else {
			// First get location from Network Provider
			if (isNetworkEnabled) {
				locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,  MIN_TIME_BW_UPDATES,  MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
				LogUtils.d(TAG, "Network");
				if (locationMgr != null) {
					location = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if (location != null) {
						updateLocation(location);
					}
				}
			}
			//get the location by gps
			if (isGPSEnabled) {
				if (location == null) {
					locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
					LogUtils.d(TAG, "GPS Enabled");
					if (locationMgr != null) {
						location = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						if (location != null) {
							updateLocation(location);
						}
					}
				}
			}
		}
	}

	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location provider.
			updateLocation(location);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {}

	};

	private void updateLocation(Location loc) {
		LogUtils.d(TAG, "Location changed: " + loc.getLongitude() + "," + loc.getLatitude());
		lastLocation = loc;

		stateMgr.setLastLocation(lastLocation);
	}

	private class QueryTask extends AsyncTask<Object, Object, JSONObject> {

		private Semaphore sem = new Semaphore(MAX_RESULTS_YELP, true);

		@Override
		protected JSONObject doInBackground(Object... args) {
			Location loc = (Location) args[0];
			String query = (String) args[1];
			String result = apiMgr.search(query, loc.getLatitude(), loc.getLongitude(), MAX_RESULTS_YELP);
			JSONObject response = null;

			long startTime = SystemClock.uptimeMillis();

			try {
				response = new JSONObject(result);

				JSONArray results = response.getJSONArray("businesses");
				final int len = results.length();
				LogUtils.d(TAG, "numResults: " + len);

				for (int i = 0; i < len; i++) {
					final JSONObject r = results.getJSONObject(i);
					sem.acquire();
					stateMgr.getExecutor().execute(new Runnable() {

						@Override
						public void run() {
							try {
								Document scraped = YelpUtils.scrapeSite(r.getString("mobile_url"));
								ArrayList<Double> times = YelpUtils.getOpenTime(scraped);

								r.put("delta-open-time", Utils.getTimeTillClose(YelpUtils.getOpenTime(scraped), r.getDouble("distance")));
								r.put("price", YelpUtils.getMoney(scraped));
								r.put("rating", YelpUtils.normalizeRating(r.getDouble("rating")));

								synchronized(lock) {
									LogUtils.d(TAG, "URL: " + r.getString("mobile_url"));
									LogUtils.d(TAG, "Name: " + r.getString("name"));
									LogUtils.d(TAG, "Money: " + r.getDouble("price"));
									LogUtils.d(TAG, "Open: " + YelpUtils.getOpenTime(scraped));
									LogUtils.d(TAG, "Open for: " + r.getDouble("delta-open-time"));
									LogUtils.d(TAG, "Distance: " + r.getString("distance"));
									LogUtils.d(TAG, "Rating: " + r.getString("rating"));
								}
							} catch (Exception e) {
								LogUtils.e(TAG, "", e);
							}
							sem.release();
						}

					});
				}

				sem.acquire(MAX_RESULTS_YELP);

				// start filter list...
				Core c = stateMgr.getCore();
				JSONObject theBest = c.setDataSet(results).filter().getTheBest();
				LogUtils.d(TAG, "request took: " + ((SystemClock.uptimeMillis() - startTime) / 1000.0) + "s");

				return theBest;
			} catch (Exception e) {
				LogUtils.e(TAG, "", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			LogUtils.d(TAG, "Winner!");
			queryTask = null;

			if (result == null) {
				fadeViewsBackIn();
				Toast.makeText(getActivity(), R.string.no_open_restaurants, Toast.LENGTH_LONG).show();
				return;
			}

			try {
				JSONObject r = result;
				LogUtils.d(TAG, "URL: " + r.getString("mobile_url"));
				LogUtils.d(TAG, "Name: " + r.getString("name"));
				LogUtils.d(TAG, "Money: " + r.getDouble("price"));
				LogUtils.d(TAG, "Open for: " + r.getDouble("delta-open-time"));
				LogUtils.d(TAG, "Distance: " + r.getString("distance"));
				LogUtils.d(TAG, "Rating: " + r.getString("rating"));

				Uri uri = new Uri.Builder()
				.scheme("geo")
				.encodedOpaquePart("0,0?q=" + result.getString("name") + "," + result.getJSONObject("location").getString("display_address"))
				.build();
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				getActivity().startActivity(intent);

				shouldResetViews = true;

				// show the remote view!
				Point out = new Point();
				getActivity().getWindowManager().getDefaultDisplay().getSize(out);
				final Toast toast = new Toast(getActivity());

				View v = inflater.inflate(R.layout.remote_view, null);
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(out.x, Utils.convertToPixels(getActivity(), 150));
				v.setLayoutParams(lp);
				v.findViewById(R.id.viewBg).setLayoutParams(lp);

				TextView title = (TextView) v.findViewById(R.id.txtTitle);
				TextView subtext = (TextView) v.findViewById(R.id.txtSubtext);
				title.setText(result.getString("name"));
				subtext.setText("on Yelp");

				RatingBar rb = (RatingBar) v.findViewById(R.id.ratingBar);
				rb.setRating((float) (result.getDouble("rating") * 5));

				toast.setView(v);
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP, 0, Utils.convertToPixels(getActivity(), 15));
				bg.postDelayed(new Runnable() {

					@Override
					public void run() {
						toast.show();
					}

				}, 1800);
			} catch (Exception e) {
				LogUtils.e(TAG, "", e);
			}
		}

	};

	public void fadeViewsBackIn() {
		Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_zoom_out);
		fadeIn.setFillAfter(true);
		fadeIn.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}

		});
		fadeIn.setFillAfter(true);
		fadeIn.setFillEnabled(true);
		queryView.setVisibility(View.VISIBLE);
		queryView.startAnimation(fadeIn);

		AlphaAnimation alphaAni = new AlphaAnimation(1f, 0f);
		alphaAni.setDuration(FADE_DURATION);
		alphaAni.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				spinnerContainer.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {}

			@Override
			public void onAnimationStart(Animation arg0) {}

		});
		spinnerContainer.startAnimation(alphaAni);

		long elapsed = SystemClock.uptimeMillis() - bg.getAnimation().getStartTime();
		float amount = 3f * Math.min(1f, (elapsed / (float)SCALE_DURATION)) + 1f;
		ScaleAnimation scaleAni = new ScaleAnimation(amount, 1f, amount, 1f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
		scaleAni.setDuration(500);
		scaleAni.setFillAfter(true);

		bg.clearAnimation();
		bg.startAnimation(scaleAni);

		rootView.findViewById(R.id.spinnerContainer).setVisibility(View.INVISIBLE);
	}

	public boolean onBackPressed() {
		if (queryTask != null && !queryTask.isCancelled()) {
			queryTask.cancel(true);

			fadeViewsBackIn();
			return true;
		} else {
			return false;
		}
	}
}
