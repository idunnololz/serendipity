package com.serendipity;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private MainFragment frag;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// Check what fragment is currently shown, replace if needed.
		frag = (MainFragment) getFragmentManager().findFragmentById(R.id.container);
		if (frag == null) {
			// Make new fragment to show this selection.
			frag = new MainFragment();

			getFragmentManager().beginTransaction()
				.replace(R.id.container, frag)
				.commit();
		}
	}

	@Override
	public void onBackPressed() {
		if (frag != null && !frag.onBackPressed()) {
			super.onBackPressed();
		}
	}
}
