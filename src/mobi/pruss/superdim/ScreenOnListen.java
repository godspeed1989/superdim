package mobi.pruss.superdim;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


public class ScreenOnListen extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
	}
	
	@Override
	public void onStart(Intent intent, int flags) {
		Log.v("SuperDim","Listening for screen on");
    	registerReceiver(new ScreenOnReceiver(), 
    			new IntentFilter(Intent.ACTION_SCREEN_ON));      		
    	registerReceiver(new ScreenOnReceiver(), 
    			new IntentFilter(Intent.ACTION_SCREEN_OFF));      		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, flags);
		return START_STICKY;
	}
}
