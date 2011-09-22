package mobi.pruss.superdim;

import java.io.DataOutputStream;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Options.PREF_FIX_SLEEP, false)) {			
			Root r = new Root();
			Device.setLock(context, r, Device.LCD_BACKLIGHT, false);
			r.close();

			try {
				int b;
				b = android.provider.Settings.System.getInt(context.getContentResolver(), 
					     android.provider.Settings.System.SCREEN_BRIGHTNESS);
				if (b<30) {
					android.provider.Settings.System.putInt(context.getContentResolver(), 
						     android.provider.Settings.System.SCREEN_BRIGHTNESS, 30);
				}
			} catch (SettingNotFoundException e) {
			}
			
			context.startService(new Intent(context, ScreenOnListen.class));
		}
	}
}
