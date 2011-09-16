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
import android.text.Editable;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SuperDim extends Activity {
	private Root root;
	private Device device;
	private static final int BREAKPOINT_BAR = 3000;

	private static final int MAX_BAR = 10000;

	private static final int CF3D_NIGHTMODE_MENU_GROUP = 1;
	private static final int CM_NIGHTMODE_MENU_GROUP = 2;
	private static final int LED_MENU_GROUP = 3;

	private static final int CF3D_NIGHTMODE_MENU_START = 1000;

	private static final int CM_NIGHTMODE_MENU_START = 2000;

	private static final int LED_MENU_START = 2000;
	private int minBrightness;

	private SeekBar barControl;
	private TextView currentValue;
	public static final String CUSTOM_PREFIX = "custom_";
	private boolean getOut;
	public static final String PREFS = "SuperDim";
	public static final String PREF_LOCK = "lock";
	private CheckBox lockCheckBox;
	public static final int DOUBLEPOWER_NONE = 0;
	public static final int DOUBLEPOWER_SUPERDIM = 1;
	public static final int DOUBLEPOWER_HOME = 2;
	private int doublePowerMode = DOUBLEPOWER_NONE;
	
	private int breakpointBrightness() {
		return minBrightness+29;
	}

	private int toBrightness(int bar) {
		if (BREAKPOINT_BAR<=bar) {
			return (int)Math.round(((double)bar-BREAKPOINT_BAR)*(255-breakpointBrightness())/(MAX_BAR-BREAKPOINT_BAR)
					+breakpointBrightness());			
		}
		else {
			return (int)Math.round((double)minBrightness + (double)bar*(breakpointBrightness()-minBrightness)/BREAKPOINT_BAR);
		}
	}

	private int toBar(int brightness) {
		if (breakpointBrightness()<=brightness) {
			return (int) Math.round(((double)brightness-breakpointBrightness())*(MAX_BAR-BREAKPOINT_BAR)/(255-breakpointBrightness())
					+ BREAKPOINT_BAR);
		}
		else {
			return (int)Math.round(((double)brightness-minBrightness)*BREAKPOINT_BAR / (breakpointBrightness()-minBrightness));
		}
	}

	public void contextMenuOnClick(View v) {
		v.showContextMenu();
	}
	
	private void setValueOnClick(int id) {
		int newValue;
		
		switch(id) {
		case R.id.min:
			newValue = minBrightness;
			break;
		case R.id.percent_25:
			newValue = minBrightness + (256-minBrightness)/4;
			break;
		case R.id.percent_50:
			newValue = minBrightness + (256-minBrightness)/2;
			break;
		case R.id.percent_75:
			newValue = minBrightness + (256-minBrightness)*3/4;
			break;
		case R.id.percent_100:
			newValue = 255;
			break;
		case R.id.minus:
			newValue = device.getBrightness(Device.LCD_BACKLIGHT) - 1;
			if (newValue < minBrightness)
				newValue = minBrightness;
			break;
		case R.id.plus:
			newValue = device.getBrightness(Device.LCD_BACKLIGHT) + 1;
			if (newValue > 255)
				newValue = 255;
			break;
		default:
			return;
		}

		device.setBrightness(Device.LCD_BACKLIGHT, newValue);
		barControl.setProgress(toBar(newValue));		
	}
	
	public void setValueOnClick(View v) {
		setValueOnClick(v.getId());
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
	
	void chooseDoublePower() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Double power key press action");

		final SharedPreferences pref = getSharedPreferences(PREFS, 0);
		
		doublePowerMode = pref.getInt("doublePower", DOUBLEPOWER_NONE); 
		
		builder.setSingleChoiceItems(new CharSequence[] {"none", "Launch SuperDim", "Go home"},
				pref.getInt("doublePower", DOUBLEPOWER_NONE), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						doublePowerMode = which;
					}});
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences.Editor ed = pref.edit();
				ed.putInt("doublePower", doublePowerMode);
				ed.commit();
				startServiceIfNeeded(pref);
				Log.v("SuperDim", "doublePower "+doublePowerMode);
			}
		});
		
		builder.create().show();
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
	
	private void setMinimum() {
		if (!device.haveLCDBacklight)
			return;

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		
		alertDialog.setTitle("Set brightness minimum");
		alertDialog.setMessage("Choose a value between 1 and 40 to stop SuperDim from going dimmer than that:");
		final EditText field = new EditText(this);
		NumberKeyListener listener = new NumberKeyListener() {
			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_NUMBER;
			}

			@Override
			protected char[] getAcceptedChars() {
				return new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
			}
		};
		field.setKeyListener(listener);
		field.setText(""+minBrightness);
		alertDialog.setView(field);
		
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				CharSequence s = field.getText();
				if (s.length()>0) {
					int curBrightness = toBrightness(barControl.getProgress());
					minBrightness = Integer.parseInt(s.toString());
					SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
					ed.putInt("minBrightness", minBrightness);
					ed.commit();
					if (curBrightness < minBrightness) {
						device.setBrightness(Device.LCD_BACKLIGHT, minBrightness);
						curBrightness = minBrightness;
					}					
					barControl.setProgress(toBar(curBrightness));
				}
			} });
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
		if (! getSharedPreferences(PREFS, 0).getBoolean("firstTime", true))
			return;

		SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
		ed.putBoolean("firstTime2", false);
		ed.commit();           

		if (device.haveLCDBacklight) {
			message("Warning", "SuperDim lets you set very low "+
					"brightness values on your device.  Note that you can adjust "+
					"brightness with your device's volume (as well as up/down/left/right) keys, so "+
					"if you set your brightness so low that the screen disappears, you should be able to restore it." +
					"If you get stuck with the screen off, you may need to reboot your device.\n" +
					"The lock mode lets lock in the settings, making it harder for other applications to change them.  But "+
					"the lock mode also causes some Nooks to hang when returning from sleep--they then need to be rebooted.");
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

		SharedPreferences pref = getCustomPreferences(n);
		
		barControl.setProgress(toBar(
				device.customLoad(root, pref, n)));
		
		if (device.needRedraw) {
			device.needRedraw = false;
			redraw();
		}

		lockCheckBox = (CheckBox)findViewById(R.id.lock);

		if (device.haveLCDBacklight) {
			lockCheckBox.setChecked(pref.getBoolean(PREF_LOCK, false));
		}
	}

	private void customSave(View v) {
		int	n = getCustomNumber(v);
		if (n<0)
			return;

		SharedPreferences pref = getCustomPreferences(n);
		
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(PREF_LOCK, isLockCheckBoxSet());
		
		if (device.customSave(root, ed))
			Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT).show();
		
	}
	
	private boolean isLockCheckBoxSet() {
		return lockCheckBox.getVisibility() == View.VISIBLE && 
			lockCheckBox.isChecked(); 
	}
	
	void startServiceIfNeeded(SharedPreferences pref) {
		Log.v("SuperDim","stop service");
		Intent i = new Intent(this, ScreenOnListen.class);
		stopService(i);

		if (device.haveLCDBacklight) { 
			Log.v("SuperDim","start service");
			startService(new Intent(this, ScreenOnListen.class));
		}		
	}
	
	void loadCustomShortcut(int customNumber) {
		if (customNumber == AddShortcut.SET_AUTOMATIC) {
			Device.setBrightnessMode(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
			return;
		}		

//		if (!Root.test()) 
//			return;
		
		SharedPreferences pref = getSharedPreferences(PREFS, 0);

		if (customNumber == AddShortcut.CYCLE) {
			customNumber = pref.getInt("lastCycle", 4);
			customNumber = (customNumber+1)%5;
			SharedPreferences.Editor ed = pref.edit();
			ed.putInt("lastCycle", customNumber);
			ed.commit();
		}		
		
		root = new Root();
		device = new Device(this, root);
		if (!device.valid) {
			return;
		}

		startServiceIfNeeded(pref);

		SharedPreferences customPref = getCustomPreferences(customNumber);
		device.customLoad(root, customPref, customNumber);
		
		if (device.haveLCDBacklight) {
			boolean lock = customPref.getBoolean(PREF_LOCK, false);
			if (lock)
				Device.setLock(root, Device.LCD_BACKLIGHT, true);
			SharedPreferences.Editor ed = pref.edit();
			ed.putBoolean(PREF_LOCK, lock);
			ed.commit();
		}
		
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
		
		root = null;
		
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
			getOut = true;
			return;
		}

		root = new Root();
		Log.v("SuperDim", "root set");
		device = new Device(this, root);
		
		if (!device.valid) {
			fatalError(R.string.incomp_device_title, R.string.incomp_device);
			getOut = true;
			return;
		}

		Button button = (Button)findViewById(R.id.cf3d_nightmode);
		if (device.haveCF3D) {
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
		minBrightness = getSharedPreferences(PREFS, 0).getInt("minBrightness", 1);
		barControl.setProgress(toBar(device.getBrightness(Device.LCD_BACKLIGHT)));
		
		lockCheckBox = (CheckBox)findViewById(R.id.lock); 

		SharedPreferences pref = getSharedPreferences(PREFS, 0);
		lockCheckBox.setChecked(pref.getBoolean(PREF_LOCK, false));
		
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

		SharedPreferences pref = getSharedPreferences(PREFS, 0);
		boolean locked = pref.getBoolean(PREF_LOCK, false);

		if (locked) {
			SharedPreferences.Editor ed = pref.edit();
			/* Turn this off while in app */
			ed.putBoolean(PREF_LOCK, false);
			ed.commit();
		}			
	}

	@Override
	public void onPause() {
		super.onPause();
		
		if (root != null) {
			if (!getOut) {
				SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
				
				ed.putBoolean(PREF_LOCK, isLockCheckBoxSet());
				ed.commit();
				
				if (isLockCheckBoxSet())
					Device.setLock(root, Device.LCD_BACKLIGHT, true);
			}
			
			root.close();
			root = null;
		}
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
			Device.setNightmode(this, root, Device.cf3dNightmode, Device.cf3dNightmodeCommands[id - CF3D_NIGHTMODE_MENU_START]);
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
		case R.id.min_brightness:
			setMinimum();
			return true;
		case R.id.please_buy:
			new PleaseBuy(this, true);
			return true;
		case R.id.auto:
			device.setBrightnessMode(android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
			finish();
			return true;
		case R.id.double_power_key:
			chooseDoublePower();
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
		menu.findItem(R.id.auto).setVisible(8 <= android.os.Build.VERSION.SDK_INT);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v("down", ""+keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case 92: // nook 
			case 94: // nook
				if (event.getAction() == KeyEvent.ACTION_DOWN) 
					setValueOnClick(R.id.plus);
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case 93: // nook
			case 95: // nook
				if (event.getAction() == KeyEvent.ACTION_DOWN) 
					setValueOnClick(R.id.minus);
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case 92: // nook 
			case 94: // nook
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case 93: // nook
			case 95: // nook
				return true;
		}
		return super.onKeyUp(keyCode, event);
	} 
}
