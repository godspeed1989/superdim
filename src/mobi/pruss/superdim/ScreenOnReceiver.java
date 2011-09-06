package mobi.pruss.superdim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class ScreenOnReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context c, Intent i) {
		if (i.getAction().equals(Intent.ACTION_SCREEN_ON))
			handleScreenOn(c);
		else if (i.getAction().equals(Intent.ACTION_SCREEN_OFF))
			handleScreenOff(c);
	}
	
	static void handleScreenOn(Context c) {
		Log.v("SuperDim", "screen on");
		
		SharedPreferences pref = c.getSharedPreferences(SuperDim.PREFS, 0);
		
		int doublePower = pref.getInt("doublePower", SuperDim.DOUBLEPOWER_NONE);
		
		if (doublePower != SuperDim.DOUBLEPOWER_NONE) {
			long t2 = SystemClock.uptimeMillis();
			Log.v("SuperDim", "powerOnTime:"+SystemClock.uptimeMillis());
			long t1 = pref.getLong("powerOffTime", 0);
			if (t2 >= t1 && t2-t1 < 1000) {
				Log.v("SuperDim", "double power tap");
				if (doublePower == SuperDim.DOUBLEPOWER_SUPERDIM) {
					Intent i = new Intent(c, SuperDim.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					c.startActivity(i);
				}
				else if (doublePower == SuperDim.DOUBLEPOWER_HOME) {
					Intent i = new Intent(Intent.ACTION_MAIN);
					i.addCategory(Intent.CATEGORY_HOME);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					c.startActivity(i);
				}
			}
		}
		
		if (Device.getSafeMode(c))
			return;
		
		if (android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL == 
					Device.getBrightnessMode(c)) {
			try {
				int b = android.provider.Settings.System.getInt(c.getContentResolver(), 
					     android.provider.Settings.System.SCREEN_BRIGHTNESS);
				if (0 < b && b < 20 && b<Device.getBrightness(c, Device.LCD_BACKLIGHT)) {
					Log.v("SuperDim", "screen on, must set "+b);
					Device.writeBrightness(Device.getBrightnessPath(Device.LCD_BACKLIGHT), b);
					try {
						Thread.sleep(1000,0);
					} catch (Exception e) {
					}
					Log.v("SuperDim", "writing "+b+" again for good measure");
					Device.writeBrightness(Device.getBrightnessPath(Device.LCD_BACKLIGHT), b);
				}
			} catch (SettingNotFoundException e) {
				// TODO Auto-generated catch block
			}
		}

		if (c.getSharedPreferences(SuperDim.PREFS, 0).getBoolean(SuperDim.PREF_LOCK, false)) {
			Root r = new Root();
			Device.setLock(r, Device.LCD_BACKLIGHT, true);
			r.close();
		}

	}

	static void handleScreenOff(Context c) {
		Log.v("SuperDim", "screen off");
		
		SharedPreferences pref = c.getSharedPreferences(SuperDim.PREFS, 0);
		
		int doublePower = pref.getInt("doublePower", SuperDim.DOUBLEPOWER_NONE);
		Log.v("SuperDim", "dP"+doublePower);
		
		if (doublePower != SuperDim.DOUBLEPOWER_NONE) {
			SharedPreferences.Editor ed = pref.edit();
			ed.putLong("powerOffTime", SystemClock.uptimeMillis());
			Log.v("SuperDim", "powerOffTime:"+SystemClock.uptimeMillis());
			ed.commit();
		}
		
		if (Device.getSafeMode(c) || 
				android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL != 
					Device.getBrightnessMode(c))
			return;
		
		if (pref.getBoolean("lock", false)) {
			Root r = new Root();
			Device.setLock(r, Device.LCD_BACKLIGHT, false);
			r.close();
		}
	}
}
