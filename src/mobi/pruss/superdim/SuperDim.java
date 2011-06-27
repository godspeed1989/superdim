package mobi.pruss.superdim;

import java.io.*;

import mobi.pruss.superdim.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;  
import android.view.ContextMenu.ContextMenuInfo;  
import android.view.Window;
import android.view.WindowManager;

public class SuperDim extends Activity {
	private Root root;
	private String[] ledNames;
	private static final String cf3dNightmode="persist.cf3d.nightmode";
	private static final String cmNightmode="debug.sf.render_effect";
	private static final String ledPrefPrefix = "leds/";
	private static final int BREAKPOINT_BAR = 3000;
	private static final int BREAKPOINT_BRIGHTNESS = 30;

	private static final int MAX_BAR = 10000;
	
	private static final int CF3D_NIGHTMODE_MENU_GROUP = 1;
	private static final int CM_NIGHTMODE_MENU_GROUP = 2;
	private static final int LED_MENU_GROUP = 3;
	
	private static final int CF3D_NIGHTMODE_MENU_START = 1000;
	
	private static final String cf3dNightmodeCommands[] = {"disabled", "red", "green", "blue",
		"amber", "salmon", "custom:160:0:0", "custom:112:66:20", "custom:239:219:189",
		"custom:255:0:128" };
	private static final int cf3dNightmodeLabels[] = {R.string.nightmode_disabled,
		R.string.nightmode_red, R.string.nightmode_green, R.string.nightmode_blue,
		R.string.nightmode_amber, R.string.nightmode_salmon, R.string.nightmode_dark_red,
		R.string.nightmode_sepia, R.string.nightmode_light_sepia,
		R.string.nightmode_fuscia };	

	private static final int cmNightmodeLabels[] = {R.string.nightmode_disabled,
		R.string.nightmode_red, R.string.nightmode_green, 
		R.string.nightmode_amber, R.string.nightmode_salmon,
		R.string.nightmode_fuscia };

	private static final int CM_NIGHTMODE_MENU_START = 2000;
	
	private static final int LED_MENU_START = 2000;
	
	private static final String defaultCF3DNightmode[] = { "disabled", "red", "green", "green", "disabled" };
	private static final int defaultCMNightmode[] = { 0, 1, 2, 2, 0};
	private static final int[] defaultBacklight = 
		{ 50, 10, 50, 200, 255 };
	private SeekBar barControl;
	private TextView currentValue;
	private Resources res;
	private boolean haveCF3D;
	private boolean haveCM;
	public static final String CUSTOM_PREFIX = "custom_";
	
	private int toBrightness(int bar) {
		if (BREAKPOINT_BAR<=bar) {
			return (bar-BREAKPOINT_BAR)*(255-BREAKPOINT_BRIGHTNESS)/(MAX_BAR-BREAKPOINT_BAR)
				+BREAKPOINT_BRIGHTNESS;			
		}
		else {
			return 1 + bar*(BREAKPOINT_BRIGHTNESS-1)/BREAKPOINT_BAR;
		}
	}
	
	private int toBar(int brightness) {
		if (BREAKPOINT_BRIGHTNESS<=brightness) {
			return (brightness-BREAKPOINT_BRIGHTNESS)*(MAX_BAR-BREAKPOINT_BAR)/(255-BREAKPOINT_BRIGHTNESS)
				+ BREAKPOINT_BAR;
		}
		else {
			return (brightness-1)*BREAKPOINT_BAR / (BREAKPOINT_BRIGHTNESS-1);
		}
	}
	
	private void setNightmode(String nmType, String s) {
		root.exec("setprop " + nmType + " "+s);
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

		LEDs.setBrightness(root, getContentResolver(), LEDs.LCD_BACKLIGHT, newValue);
        barControl.setProgress(toBar(newValue));
	}
		
	private String getNightmode(String nmType) {
		try {
			Process p = Runtime.getRuntime().exec("getprop "+nmType);
			DataInputStream stream = new DataInputStream(p.getInputStream());
			byte[] buf = new byte[12];
			String s;
			
			int numRead = stream.read(buf);
			if(p.waitFor() != 0) {
				return null;
			}			
			stream.close();
			
			if(0 < numRead) {
				s = (new String(buf, 0, numRead)).trim();
				
				if (s.equals(""))
					return null;
				else
					return s;
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private void redraw() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        res = getResources();
        
        alertDialog.setTitle("Changing nightmode");
        alertDialog.setMessage("Please press OK.");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.ok), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.show();  
	}
	
	private void fatalError(int title, int msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        res = getResources();
        
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
		
		if (pref == null)
			return;
				
		for (int i = 0 ; i < ledNames.length; i++) {
			boolean isBacklight = ledNames[i].equals(LEDs.LCD_BACKLIGHT);
			int b = pref.getInt(ledPrefPrefix+ledNames[i], isBacklight ? defaultBacklight[n] : -1);
			if (0<=b) {
				LEDs.setBrightness(root, getContentResolver(), ledNames[i], b);
				if (isBacklight)
					barControl.setProgress(toBar(b));
			}
		}
		
		
		if (haveCF3D) {
			String oldNM = getNightmode(cf3dNightmode);
			String nm = pref.getString("nightmode", defaultCF3DNightmode[n]);
			if (! nm.equals(oldNM)) {
				setNightmode(cf3dNightmode, nm);
				redraw();
			}
		}
		else if (haveCM) {
			String oldNM = getNightmode(cmNightmode);
			String nm = pref.getString("cm_nightmode", ""+defaultCMNightmode[n]);
			if (! nm.equals(oldNM)) {
				setNightmode(cmNightmode, nm);
				redraw();
			}
		}
}
	
	private void customSave(View v) {
		int	n = getCustomNumber(v);
		if (n<0)
			return;
		SharedPreferences pref = getCustomPreferences(n);
		
		SharedPreferences.Editor ed = pref.edit();
		
		if (haveCF3D) {
			String nm = getNightmode(cf3dNightmode);
			if ( nm != null)
				ed.putString("nightmode", nm);
		}
		else if (haveCM) {
			String nm = getNightmode(cmNightmode);
			if ( nm != null)
				ed.putString("cm_nightmode", nm);			
		}

		for (int i=0 ; i < ledNames.length; i++) {
			ed.putInt(ledPrefPrefix + ledNames[i], LEDs.getBrightness(ledNames[i]));
		}
		ed.commit();
		Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT).show();
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	int i;
        
    	ledNames = LEDs.getNames();
    	
    	if (ledNames.length == 0) {
    		fatalError(R.string.incomp_device_title, R.string.incomp_device);
    		return;
    	}    		
    	
        Log.v("SuperDim", "entering");
        haveCF3D = (getNightmode(cf3dNightmode) != null);
        if (haveCF3D) {
        	haveCM = false;
        }
        else {
        	haveCM = (getNightmode(cmNightmode) != null);
        }
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        if (!Root.test()) {
        	fatalError(R.string.need_root_title, R.string.need_root);
        	return;
        }
        
        root = new Root();
        Log.v("SuperDim", "root set");
        
        Button button = (Button)findViewById(R.id.cf3d_nightmode);
        if (haveCF3D) {
        	registerForContextMenu(button);
        }
        else {
        	button.setVisibility(View.GONE);  
        }

        button = (Button)findViewById(R.id.cm_nightmode);
        if (haveCM) {
        	registerForContextMenu(button);
        }
        else {
        	button.setVisibility(View.GONE);  
        }

        boolean haveOtherLED = false;
        boolean haveLCDBacklight = false;
        for (i=0; i<ledNames.length; i++)
        	if (!ledNames[i].equals(LEDs.LCD_BACKLIGHT)) {
        		haveOtherLED = true;
        	}
        	else {
        		haveLCDBacklight = true;
        	}
        haveLCDBacklight = true;
        
        button = (Button)findViewById(R.id.led);        
        if (haveOtherLED) {
        	registerForContextMenu(button);
        }
        else {
        	button.setVisibility(View.GONE);  
        }        		

        if (!haveLCDBacklight) {
        	findViewById(R.id.brightness).setVisibility(View.GONE);
        	findViewById(R.id.min).setVisibility(View.GONE);
        	findViewById(R.id.percent_25).setVisibility(View.GONE);
        	findViewById(R.id.percent_50).setVisibility(View.GONE);
        	findViewById(R.id.percent_75).setVisibility(View.GONE);
        	findViewById(R.id.percent_100).setVisibility(View.GONE);
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

        if (haveLCDBacklight) {
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
						LEDs.setBrightness(root, getContentResolver(),
								LEDs.LCD_BACKLIGHT, toBrightness(progress));					
					}
				};
	
			barControl.setOnSeekBarChangeListener(seekbarListener);
			barControl.setProgress(toBar(LEDs.getBrightness(LEDs.LCD_BACKLIGHT)));
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	root = new Root();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	root.close();
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	int id = item.getItemId();
    	int group = item.getGroupId();
    	
    	switch(group) {
    	case LED_MENU_GROUP:
    		int ledNumber = id - LED_MENU_START;
    		int b = LEDs.getBrightness(ledNames[ledNumber]);
    		if (0<=b) {
    			Log.v(ledNames[ledNumber],""+b);
    			LEDs.setBrightness( root, getContentResolver(), ledNames[ledNumber],
    					(b != 0) ? 0 : 255 );
    		}
    		return true;
    	case CF3D_NIGHTMODE_MENU_GROUP:
    		setNightmode(cf3dNightmode, cf3dNightmodeCommands[id - CF3D_NIGHTMODE_MENU_START]);
    		return true;
    	case CM_NIGHTMODE_MENU_GROUP:
    		setNightmode(cmNightmode, ""+(id-CM_NIGHTMODE_MENU_START));
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
    		for (int i=0; i<cf3dNightmodeLabels.length; i++) {
    			menu.add(CF3D_NIGHTMODE_MENU_GROUP, CF3D_NIGHTMODE_MENU_START+i,
    					Menu.NONE, cf3dNightmodeLabels[i]);
    		}
    		break;
    	case R.id.cm_nightmode:
    		menu.setHeaderTitle("Nightmode");
    		for (int i=0; i<cmNightmodeLabels.length; i++) {
    			menu.add(CM_NIGHTMODE_MENU_GROUP, CM_NIGHTMODE_MENU_START+i,
    					Menu.NONE, cmNightmodeLabels[i]);
    		}
    		break;
    	case R.id.led:
    		menu.setHeaderTitle("Other lights");
    		for (int i=0; i<ledNames.length; i++) {
    			if (! ledNames[i].equals(LEDs.LCD_BACKLIGHT)) {
    				menu.add(LED_MENU_GROUP, LED_MENU_START+i, Menu.NONE,
    					ledNames[i]);
    			}
    		}
    	default:
    		break;
    	}
    }
}
