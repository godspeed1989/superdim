package mobi.pruss.superdim;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Options extends PreferenceActivity {
	public final static String PREF_FIX_SLEEP = "fixSleep";
	public static final String  PREF_PRESET_NAME_PREFIX = "presetName";
	public static final String[] OPT_PRESET_NAME = {"night1", "night2", "night3", "day1", "day2"};
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
}
