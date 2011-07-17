package mobi.pruss.superdim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SuperDim extends Activity {
	private Root root;
	private Device device;
	private static final int BREAKPOINT_BAR = 3000;
	private static final int BREAKPOINT_BRIGHTNESS = 30;

	private static final int MAX_BAR = 10000;

	private static final int CF3D_NIGHTMODE_MENU_GROUP = 1;
	private static final int CM_NIGHTMODE_MENU_GROUP = 2;
	private static final int LED_MENU_GROUP = 3;

	private static final int CF3D_NIGHTMODE_MENU_START = 1000;

	private static final int CM_NIGHTMODE_MENU_START = 2000;

	private static final int LED_MENU_START = 2000;

	private SeekBar barControl;
	private TextView currentValue;
	public static final String CUSTOM_PREFIX = "custom_";
	private boolean getOut;

	private int toBrightness(int bar) {
		if (BREAKPOINT_BAR<=bar) {
			return (int)Math.round(((double)bar-BREAKPOINT_BAR)*(255-BREAKPOINT_BRIGHTNESS)/(MAX_BAR-BREAKPOINT_BAR)
					+BREAKPOINT_BRIGHTNESS);			
		}
		else {
			return (int)Math.round((double)1 + (double)bar*(BREAKPOINT_BRIGHTNESS-1)/BREAKPOINT_BAR);
		}
	}

	private int toBar(int brightness) {
		if (BREAKPOINT_BRIGHTNESS<=brightness) {
			return (int) Math.round(((double)brightness-BREAKPOINT_BRIGHTNESS)*(MAX_BAR-BREAKPOINT_BAR)/(255-BREAKPOINT_BRIGHTNESS)
					+ BREAKPOINT_BAR);
		}
		else {
			return (int)Math.round(((double)brightness-1)*BREAKPOINT_BAR / (BREAKPOINT_BRIGHTNESS-1));
		}
	}

	public void contextMenuOnClick(View v) {
		v.showContextMenu();
	}

	public void setValueOnClick(View v) {
		int newValue;
		switch(v.getId()) {
		case R.id.min:
			newValue = 1;
			break;
		case R.id.percent_25:
			newValue = 1 + 256 / 4;
			break;
		case R.id.percent_50:
			newValue = 1 + 256 / 2;
			break;
		case R.id.percent_75:
			newValue = 1 + 3 * 256 / 4;
			break;
		case R.id.percent_100:
			newValue = 255;
			break;
		default:
			return;
		}

		device.setBrightness(Device.LCD_BACKLIGHT, newValue);
		barControl.setProgress(toBar(newValue));
	}

	private void redraw() {
		/*        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        res = getResources();

        alertDialog.setTitle("Changing nightmode");
        alertDialog.setMessage("Please press OK.");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.ok), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.show(); */

		Intent intent = getIntent();
		finish();
		if (Build.VERSION.SDK_INT >= 5) {
			overridePendingTransition(0, 0);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		startActivity(intent);		
	}

	private void message(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getText(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();

	}

	private void fatalError(int title, int msg) {
		Resources res = getResources();

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		Log.e("fatalError", (String) res.getText(title));

		alertDialog.setTitle(res.getText(title));
		alertDialog.setMessage(res.getText(msg));
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				res.getText(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {finish();} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {finish();} });
		alertDialog.show();		
	}

	private void firstTime() {
		if (! getPreferences(0).getBoolean("firstTime", true))
			return;

		SharedPreferences.Editor ed = getPreferences(0).edit();
		ed.putBoolean("firstTime", false);
		ed.commit();           

		if (device.haveLCDBacklight) {
			message("Warning", "SuperDim lets you set very low "+
					"brightness values on your device.  It is recommended that "+
					"you keep your finger on the brightness slider so that "+
					"if the screen turns completely off, you will be able to "+
					"turn it back on by moving  your finger to the right.  If you get "+
			"stuck with the screen off, you may need to reboot your device.");
		}
		else {
			message("LCD backlight not found",
					"SuperDim cannot find an LCD backlight on your "+
					"device.  Most likely, your device has an OLED screen which does "+
					"not have a backlight.  On OLED devices, SuperDim will be unable "+
			"to keep very low brightness settings after exiting SuperDim.");
		}
	}

	private SharedPreferences getCustomPreferences(int n) {
		if (n < 0)
			return null;
		return getSharedPreferences(CUSTOM_PREFIX+n, 0); 
	}

	private int getCustomNumber(View v) {
		switch(v.getId()) {
		case R.id.custom0:
			return 0;
		case R.id.custom1:
			return 1;
		case R.id.custom2:
			return 2;
		case R.id.custom3:
			return 3;
		case R.id.custom4:
			return 4;
		default:
			return -1;
		}
	}

	public void customLoad(View v) {		
		int	n = getCustomNumber(v);
		if (n<0)
			return;

		barControl.setProgress(toBar(
				device.customLoad(root, getCustomPreferences(n), n)));
		if (device.needRedraw) {
			device.needRedraw = false;
			redraw();
		}
	}

	private void customSave(View v) {
		int	n = getCustomNumber(v);
		if (n<0)
			return;

		SharedPreferences pref = getCustomPreferences(n);
		if (device.customSave(root, pref))
			Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT).show();
	}
	
	void loadCustomShortcut(int customNumber) {
		if (!Root.test()) 
			return;
		
		root = new Root();
		device = new Device(this, root);
		if (!device.valid) {
			return;
		}
		
		device.customLoad(root,getCustomPreferences(customNumber), customNumber);
		if (device.needRedraw) {
			device.needRedraw = false;
			/* TODO: Handle needRedraw in some smart way */			
		}
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		int customNumber = intent.getIntExtra(AddShortcut.LOAD_CUSTOM, -1);
		if (0<=customNumber) {
			loadCustomShortcut(customNumber);
			finish();
			getOut = true;			
			return;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v("SuperDim", "OnCreate");
		
		int customNumber = getIntent().getIntExtra(AddShortcut.LOAD_CUSTOM, -1);
		if (0<=customNumber) {
			loadCustomShortcut(customNumber);
			finish();
			getOut = true;
			Log.v("SuperDim", "finishing");
			return;
		}
		
		getOut = false;

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		if (!Root.test()) {
			fatalError(R.string.need_root_title, R.string.need_root);
			return;
		}

		root = new Root();
		Log.v("SuperDim", "root set");
		device = new Device(this, root);
		if (!device.valid) {
			fatalError(R.string.incomp_device_title, R.string.incomp_device);
			return;
		}

		Button button = (Button)findViewById(R.id.cf3d_nightmode);
		if (device.haveCF3D) {
			registerForContextMenu(button);
		}
		else {
			button.setVisibility(View.GONE);  
		}

		button = (Button)findViewById(R.id.cm_nightmode);
		if (device.haveCM) {
			registerForContextMenu(button);
		}
		else {
			button.setVisibility(View.GONE);  
		}

		if (device.haveLCDBacklight) {
			startService(new Intent(this, ScreenOnListen.class));
		}           

		button = (Button)findViewById(R.id.led);        
		if (device.haveOtherLED) {
			registerForContextMenu(button);
		}
		else {
			button.setVisibility(View.GONE);  
		}        		

		Button.OnLongClickListener customSaveListener = 
			new Button.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				customSave(v);
				return false;
			}
		};

		((Button)findViewById(R.id.custom0)).setOnLongClickListener(customSaveListener);
		((Button)findViewById(R.id.custom1)).setOnLongClickListener(customSaveListener);
		((Button)findViewById(R.id.custom2)).setOnLongClickListener(customSaveListener);
		((Button)findViewById(R.id.custom3)).setOnLongClickListener(customSaveListener);
		((Button)findViewById(R.id.custom4)).setOnLongClickListener(customSaveListener);

		currentValue = (TextView)findViewById(R.id.current_value);
		barControl = (SeekBar)findViewById(R.id.brightness);

		SeekBar.OnSeekBarChangeListener seekbarListener = 
			new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				currentValue.setText(""+toBrightness(progress)+"/255");
				device.setBrightness(Device.LCD_BACKLIGHT, toBrightness(progress));					
			}
		};

		barControl.setOnSeekBarChangeListener(seekbarListener);
		barControl.setProgress(toBar(device.getBrightness(Device.LCD_BACKLIGHT)));

		new PleaseBuy(this, false);
		firstTime();        
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (getOut) { 
			Log.v("SuperDim", "getting out");
			return;
		}
		
		Log.v("SuperDim", "resuming");
		if (root == null) {
			root = new Root();
			device.setPermissions(root);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		root.close();
		root = null;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		int group = item.getGroupId();

		switch(group) {
		case LED_MENU_GROUP:
			int ledNumber = id - LED_MENU_START;
			int b = device.getBrightness(device.names[ledNumber]);
			if (0<=b) {
				Log.v(device.names[ledNumber],""+b);
				device.setBrightness( device.names[ledNumber],
						(b != 0) ? 0 : 255 );
			}
			return true;
		case CF3D_NIGHTMODE_MENU_GROUP:
			Device.setNightmode(root, Device.cf3dNightmode, Device.cf3dNightmodeCommands[id - CF3D_NIGHTMODE_MENU_START]);
			return true;
		case CM_NIGHTMODE_MENU_GROUP:
			Device.setNightmode(root, Device.cmNightmode, ""+(id-CM_NIGHTMODE_MENU_START));
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		switch(v.getId()) {
		case R.id.cf3d_nightmode:
			menu.setHeaderTitle("Nightmode");
			for (int i=0; i<Device.cf3dNightmodeLabels.length; i++) {
				if (!Device.cf3dPaid[i] || device.haveCF3DPaid)
					menu.add(CF3D_NIGHTMODE_MENU_GROUP, CF3D_NIGHTMODE_MENU_START+i,
							Menu.NONE, Device.cf3dNightmodeLabels[i]);
			}
			break;
		case R.id.cm_nightmode:
			menu.setHeaderTitle("Nightmode");
			for (int i=0; i<Device.cmNightmodeLabels.length; i++) {
				menu.add(CM_NIGHTMODE_MENU_GROUP, CM_NIGHTMODE_MENU_START+i,
						Menu.NONE, Device.cmNightmodeLabels[i]);
			}
			break;
		case R.id.led:
			menu.setHeaderTitle("Other lights");
			for (int i=0; i<device.names.length; i++) {
				if (! device.names[i].equals(Device.LCD_BACKLIGHT)) {
					menu.add(LED_MENU_GROUP, LED_MENU_START+i, Menu.NONE,
							device.names[i]);
				}
			}
		default:
			break;
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.please_buy:
			new PleaseBuy(this, true);
			return true;
		case R.id.safe_mode:
			if (Device.getSafeMode(this)) {
				item.setTitle("Turn on safe mode");
				Device.setSafeMode(this, false);
			}
			else {
				item.setTitle("Turn off safe mode");
				Device.setSafeMode(this, true);
				message("Safe mode on", 
						"In safe mode, very low brightness settings will not be saved "+
				"when you exit SuperDim.");
			}
		default:
			return false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.safe_mode).setTitle(Device.getSafeMode(this)?
				"Turn off safe mode":"Turn on safe mode");
		return true;
	}
}
