package mobi.pruss.superdim;

import java.io.*;

import mobi.pruss.superdim.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SuperDim extends Activity {
	private static final String backlightFile="/sys/class/leds/lcd-backlight/brightness";
	private static final String powerLEDFile="/sys/class/leds/power/brightness";
	private DataOutputStream rootCommands = null;
	private Process rootShell;
	private static final int BREAKPOINT_BAR = 3000;
	private static final int BREAKPOINT_BRIGHTNESS = 30;
	private static final int MAX_BAR = 10000;
	private SeekBar barControl;
	

	TextView currentValue;	
	
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
	
	private boolean testRoot() {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private boolean initRoot() {
		try {
			rootShell = Runtime.getRuntime().exec("su");
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private void closeRoot() {
		if (rootCommands != null) {
			try {
				rootCommands.close();
			}
			catch (Exception e) {
			}
		}
	}
	
	private void setBrightness(String file, int n) {
		if (n<0)
			n = 0;
		else if (n>255)
			n = 255;
		
		android.provider.Settings.System.putInt(getContentResolver(),
			     android.provider.Settings.System.SCREEN_BRIGHTNESS,
			     n);

		try {
			rootCommands.writeBytes("echo "+n+" >\""+file+"\"\n");
			rootCommands.flush();
		}
		catch (Exception e) {
			Log.e("Error","setting "+n);
		}
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

		setBrightness(backlightFile, newValue);
        barControl.setProgress(toBar(newValue));
	}
		
	public void powerLEDOnClick(View v) {
		int b = getBrightness(powerLEDFile);
		
		if (b<0)
			return;
		
		setBrightness(powerLEDFile, b==0 ? 255 : 0);
	}
	
	private int getBrightness(String file) {
		try {
			FileInputStream stream = new FileInputStream(file);
			byte[] buf = new byte[12];
			String s;
			
			int numRead = stream.read(buf);
			
			stream.close();
			
			if(0 < numRead) {
				s = new String(buf, 0, numRead);
				
				return Integer.parseInt(s.trim());
			}
			else {
				return -1;
			}
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	private void fatalError(int title, int msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        Resources res = getResources();
        
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.v("SuperDim", "entering");

        setContentView(R.layout.main);
        
        if (getBrightness(backlightFile)<0) {
        	fatalError(R.string.incomp_device_title, R.string.incomp_device);
        	return;
        }
        
        if (!testRoot()) {
        	fatalError(R.string.need_root_title, R.string.need_root);
        	return;
        }
        
        initRoot();

        currentValue = (TextView)findViewById(R.id.current_value);
        barControl = (SeekBar)findViewById(R.id.brightness);

        SeekBar.OnSeekBarChangeListener listener = 
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
//	        		Log.v("Set", progress+" "+toBrightness(progress));
					currentValue.setText(""+toBrightness(progress)+"/255");
					setBrightness(backlightFile, toBrightness(progress));					
				}
			};
    
        barControl.setOnSeekBarChangeListener(listener);
        
        barControl.setProgress(toBar(getBrightness(backlightFile)));
    }
    
    public void onRestart() {
    	super.onRestart();
    	initRoot();
    }
    
    public void onStop() {
    	super.onStop();
    	closeRoot();
    }
}
