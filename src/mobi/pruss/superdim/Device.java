package mobi.pruss.superdim;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManager;

public class Device {
	private static final boolean SAVE_ONLY_BACKLIGHT_LED = true;

	private static final String ledsDirectory = "/sys/class/leds";
	private static final String altLEDSDirectory = "/sys/class/backlight";
	private static final String brightnessFile = "brightness";

	public static final String LCD_BACKLIGHT = "lcd-backlight";
	public boolean haveLCDBacklight;
	public boolean haveOtherLED;
	private Activity context;
	public String[] names;
	private boolean setManual;
	static final String cf3dNightmode="persist.cf3d.nightmode";
	private static final String ledPrefPrefix = "leds/";
	public boolean needRedraw;
	public boolean haveCF3D;
	public boolean haveCF3DPaid;
	public static final String cf3dNightmodeCommands[] = {"disabled", "red", "green", "blue",
		"amber", "salmon", "custom:160:0:0", "custom:112:66:20", "custom:239:219:189",
		"custom:255:0:128" };
	
	public static final int cf3dNightmodeLabels[] = {R.string.nightmode_disabled,
		R.string.nightmode_red, R.string.nightmode_green, R.string.nightmode_blue,
		R.string.nightmode_amber, R.string.nightmode_salmon, R.string.nightmode_dark_red,
		R.string.nightmode_sepia, R.string.nightmode_light_sepia,
		R.string.nightmode_fuchsia };
	
	public static final boolean cf3dPaid[] = { false, false, false, false, false,
		false, true, true, true, true
	};

	private static final String defaultCF3DNightmode[] = { "disabled", "red", "green", "green", "disabled" };
	private static final int[] defaultBacklight = 
		{ 50, 10, 50, 200, 255 };
	public boolean valid;
	public static String PREF_LEDS = "leds";
	
	public Device(Activity c, Root root) {
		context = c;
		haveLCDBacklight = false;
		haveOtherLED     = false;
		setManual 		 = false;
		needRedraw		 = false;

		valid		     = false;

		names = getFiles(ledsDirectory);
		
		if (names == null)
			return;
		
		for (int i=0; i<names.length; i++) {
			setPermission(c, root, names[i]);
			if (names[i].equals(LCD_BACKLIGHT)) {
				haveLCDBacklight = true;
				saveBacklight(ledsDirectory+"/"+names[i]);
			}
			else {
				haveOtherLED = true;
			}
		}
		
		if (!haveLCDBacklight) {
			searchLCDBacklight(root);
		}

		Arrays.sort(names);
		
		detectNightmode();

		valid = true;
	}
	
	private void searchLCDBacklight(Root root) {
		String[] alt = getFiles(altLEDSDirectory);
		
		if (alt.length > 0) {
			int backlightIndex;
			
			if (alt.length > 1) {
				backlightIndex = -1;
				for (int i=0; i<alt.length; i++) {
					if (alt[i].endsWith("_bl")) {
						if (backlightIndex >= 0) {
							// Too many _bl entries -- can't figure out which
							// one is the right one.
							return;
						}
						backlightIndex = i;
					}
				}
				
				if (backlightIndex < 0) {
					return;
				}
			}
			else {
				backlightIndex = 0;
			}
			saveBacklight(altLEDSDirectory+"/"+alt[backlightIndex]);
			setPermission(context, root, LCD_BACKLIGHT);
			haveLCDBacklight = true;
			String[] newNames = new String[names.length + 1];
			for (int i=0; i<names.length; i++)
				newNames[i] = names[i];
			newNames[names.length] = LCD_BACKLIGHT;
			names = newNames;
		}
	}
	
	private void saveBacklight(String path) {
		SharedPreferences.Editor ed = context.getSharedPreferences(PREF_LEDS, 0).edit();
		ed.putString("backlight", path);
		ed.commit();
	}
	
	private void deleteIfExists(String s) {
		File f = new File(s);
		if (f.exists())
			f.delete();
	}
	
	private String[] getFiles(String dir) {
		try {
		String[] cmds = { "ls", dir };
		Process ls = Runtime.getRuntime().exec(cmds);
		BufferedReader reader = new BufferedReader(new InputStreamReader(ls.getInputStream()));

		ArrayList<String> names = new ArrayList<String>();
		String line;
		while (null != (line = reader.readLine())) {
			names.add(line.trim());
		}
		ls.destroy();
		
		return names.toArray(new String[names.size()]);
		}
		catch(Exception e) {
			return new String[0];
		}
		
/*
		final String tmpBase = "/tmp/SuperDim-" + System.currentTimeMillis();
		final String list = tmpBase + ".list";
		
  		if (!Root.runOne("rm "+list+ ";ls \""+dir+"\" > "+list))
			return null;
		
		ArrayList<String> names = new ArrayList<String>();
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(list));
			
			String line;
			
			while (null != (line = in.readLine())) {
				names.add(line.trim());
			}
			
			in.close();
		}
		catch (Exception e) {
		}
		
		return names.toArray(new String[names.size()]);
*/
/*
		File dirFile = new File(dir);
		File[] files = dirFile.listFiles();
		
		boolean haveLCD = false;
		for (File f:files) {
			if (f.getName().equals(LCD_BACKLIGHT)) {
				haveLCD = true;
				break;
			}
		}
		String[] n;
		if (haveLCD) {
			n = new String[files.length];			
		}
		else {
			n = new String[files.length + 1];
		}
		
		for (int i=0; i<files.length; i++)
			n[i] = files[i].getName();
		if (!haveLCD)
			n[files.length] = LCD_BACKLIGHT;
		
		return n; */ 
	}
	
	private void detectNightmode() {

		haveCF3D = false;
		
		try {
			haveCF3D = context.getPackageManager().getPackageInfo("eu.chainfire.cf3d", 0) != null;
		}
		catch (Exception e) {
		}

		haveCF3DPaid = false;

		if (haveCF3D) {
			try {
				haveCF3DPaid = context.getPackageManager().getPackageInfo("eu.chainfire.cf3d.pro", 0) != null;
			}
			catch (Exception e) {
			}
		}
	}
	
	public static String getBrightnessPath(Context c, String name) {
		if (name.equals(LCD_BACKLIGHT)) {
			return c.getSharedPreferences(PREF_LEDS, 0).
			       getString("backlight", ledsDirectory + "/" + LCD_BACKLIGHT)
			       + "/" + brightnessFile;
		}
		return ledsDirectory + "/" + name + "/" + brightnessFile;
	}
	
	public void setPermissions(Root r) {
		if (names == null || !valid)
			return;
		Log.v("SuperDim", "Setting permissions");
		for (String n:names)
			setPermission(context, r, n);
	}
	
	public static void lock(Context c, Root r, String name) {
		setPermission(c, r, name, "444");
	}
	
	private static void setPermission(Context c, Root r, String name, String perm) {
		String qpath = "\"" + getBrightnessPath(c, name) + "\"";
		r.exec("chmod " + perm + " "+qpath);
	}
	
	public static void setLock(Context c, Root r, String name, boolean state) {
		setPermission(c, r, name, state ? "444" : "666");
	}
	
	private static void setPermission(Context c, Root r, String name) {
		setPermission(c, r, name, "666");
	}
	
/*	private static void unsetPermission(Root r, String name) {
		setPermission(r, name, "644");
	}
	
	public void close(Root r) {
		for (String n:names) {
			unsetPermission(r,n);
		}
	} */
	
	public static boolean getSafeMode(Context c) {
		return c.getSharedPreferences("safeMode",0).getBoolean("safeMode", false);
	}
	
	public static void setSafeMode(Context c, boolean value) {
		SharedPreferences.Editor ed = c.getSharedPreferences("safeMode",0).edit();
		ed.putBoolean("safeMode", value);
		ed.commit();
	}
	
	public int getBrightnessMode() {
		return getBrightnessMode(context);
	}
	
	static public int getBrightnessMode(Context c) {
		if (8<=android.os.Build.VERSION.SDK_INT) {
			return android.provider.Settings.System.getInt(c.getContentResolver(), 
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		}
		else {
			return android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL; 
		}	
	}
	
	public static void setBrightnessMode(Activity c, int n) {
		if (8<=android.os.Build.VERSION.SDK_INT) {
			ContentResolver cr = c.getContentResolver();
			android.provider.Settings.System.putInt(cr,
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
					n);
			if (n == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				try {
					int b = android.provider.Settings.System.getInt(cr, 
							android.provider.Settings.System.SCREEN_BRIGHTNESS);
					WindowManager.LayoutParams lp = c.getWindow().getAttributes();
					lp.screenBrightness = b/255f;
					c.getWindow().setAttributes(lp);
				} catch (SettingNotFoundException e) {
				}
			}
		}		
	}
	
	public void setBrightnessMode(int n) {
		setBrightnessMode((Activity)context, n);
	}
	
	public void setBrightness(String name, int n) {
		String path = getBrightnessPath(context, name);
		
		if (name.equals(LCD_BACKLIGHT)) {
			ContentResolver cr = context.getContentResolver();
			
			if (! setManual) {
				setBrightnessMode(context, 
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				setManual = true;
			}
			android.provider.Settings.System.putInt(cr,
				     android.provider.Settings.System.SCREEN_BRIGHTNESS,
				     n);
			if (getSafeMode(context) || ! (new File(path)).exists() ) {
				WindowManager.LayoutParams lp = context.getWindow().getAttributes();
				lp.screenBrightness = n/255f;
				context.getWindow().setAttributes(lp);
				return;
			}
		}
		
		writeBrightness(path, n);
	}
	
	public int getBrightness(String name) {
		return getBrightness(context, name);
	}
	
	public static int getBrightness(Context c, String name) { 
		String path = getBrightnessPath(c, name);
	
		if ((new File(path)).exists()) { 
			return readBrightness(getBrightnessPath(c, name));
		}
		else if (name.equals(LCD_BACKLIGHT)) {
			try {
				return android.provider.Settings.System.getInt(
						 c.getContentResolver(), 
					     android.provider.Settings.System.SCREEN_BRIGHTNESS);
			}
			catch(Exception e) {
				return 128;
			}
		}
		else {
			return -1;
		}
	}
	
	public static boolean writeBrightness(String path, int n) {
		if (n<0)
			n = 0;
		else if (n>255)
			n = 255;
		
		File f = new File(path);
		
		if (!f.exists()) {
			Log.e("SuperDim", path+" does not exist");
			return false;
		}
		
		if (!f.canWrite()) {
			Log.e("SuperDim", path+" cannot be written");
			return false;
		}
		
		try {
			FileOutputStream stream = new FileOutputStream(f);
			String s = ""+n+"\n";
			stream.write(s.getBytes());
			stream.close();
			return true;
		} catch (Exception e) {
			Log.e("SuperDim", "Error writing "+path);
			return false;
		}		
	}
	
	public static int readBrightness(String path) {
		try {
			FileInputStream stream = new FileInputStream(path);
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
	
	public int customLoad(Root root, SharedPreferences pref, int n) {
		needRedraw = false;
		
		if (!SAVE_ONLY_BACKLIGHT_LED)
		for (int i = 0 ; i < names.length; i++) {
			if (names[i].equals(Device.LCD_BACKLIGHT)) {
				continue;
			}
			int b = pref.getInt(ledPrefPrefix+names[i], -1);
			if (0<=b) {
				setBrightness(names[i], b);
			}
		}
		
		int br = pref.getInt(ledPrefPrefix+LCD_BACKLIGHT,
				defaultBacklight[n]);
		setBrightness(Device.LCD_BACKLIGHT, br);

		if (haveCF3D) {
			String oldNM = getNightmode(cf3dNightmode);
			if (oldNM.equals("none"))
				oldNM = "disabled";
			String nm = pref.getString("nightmode", defaultCF3DNightmode[n]);
			if (nm.equals("none"))
				nm = "disabled";
			if (! nm.equals(oldNM)) {
				setNightmode(context, root, cf3dNightmode, nm);
				needRedraw = true;
			}
		}

		return br;
	}
	
	public boolean customSave(Root root, SharedPreferences.Editor ed) {
		if (haveCF3D) {
			String nm = getNightmode(cf3dNightmode);
			if ( nm != null)
				ed.putString("nightmode", nm);
		}

		for (int i=0 ; i < names.length; i++) {
			if (! names[i].equals(Device.LCD_BACKLIGHT))
				ed.putInt(ledPrefPrefix + names[i], getBrightness(names[i]));
		}
		ed.putInt(ledPrefPrefix + LCD_BACKLIGHT, 
				getBrightness(LCD_BACKLIGHT));
		ed.commit();
		return true;
	}

	private static String getNightmode(String propId) {
		try {
			Process p = Runtime.getRuntime().exec("getprop "+propId);
			DataInputStream stream = new DataInputStream(p.getInputStream());
			byte[] buf = new byte[128];
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

	static void setNightmode(Context c, Root root, String nmType, String s) {
		root.exec("setprop " + nmType + " "+s);
	}
}
