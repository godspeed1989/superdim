package mobi.pruss.superdim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class ScreenOnReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context c, Intent i) {
		Log.v("SuperDim", "screen on");

		if (Device.getSafeMode(c))
			return;
		
		try {
			int b = android.provider.Settings.System.getInt(c.getContentResolver(), 
				     android.provider.Settings.System.SCREEN_BRIGHTNESS);
			if (0 < b && b < 20 && b<Device.getBrightness(c, Device.LCD_BACKLIGHT)) {
				Log.v("SuperDim", "screen on, must set "+b);
				Device.writeBrightness(Device.getBrightnessPath(Device.LCD_BACKLIGHT), b);
			}
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
		}
	}
}
