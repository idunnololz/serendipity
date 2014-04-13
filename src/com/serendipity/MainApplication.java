package com.serendipity;

import android.app.Application;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		StateManager.create(this);
	}
}
