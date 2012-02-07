package mobi.pruss.superdim;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManager;

public class Device {
//	private static final boolean SAVE_ONLY_BACKLIGHT_LED = true;

	private static final String ledsDirectory = "/sys/class/leds";
	private static final String altLEDsDirectory = "/sys/class/backlight";
	private static final String brightnessFile = "brightness";
	private static final String triggerFile = "trigger";
	
	private static final String[] myTriggers = { "off", "on" };

	private Map<String,Boolean> haveTrigger = null;
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
	private SharedPreferences options;
	public static final String PREF_LEDS = "leds";
	public static final String PREF_SYSTEM_TRIGGER_PREFIX = "systemTrigger-"; 
	
	public Device(Activity c, Root root, SharedPreferences options) {
		this.options = options;
		context = c;
		haveLCDBacklight = false;
		haveOtherLED     = false;
		setManual 		 = false;
		needRedraw		 = false;

		valid		     = false;

		names = getFiles(ledsDirectory);
		
		if (names == null)
			return;
		
		haveTrigger = new HashMap<String, Boolean>();
		
		for (int i=0; i<names.length; i++) {
			if (names[i].equals(LCD_BACKLIGHT)) {
				haveLCDBacklight = true;
				saveBacklight(ledsDirectory+"/"+names[i]);
				setPermission(c, root, names[i], "666");
				haveTrigger.put(names[i], false);
			}
			else {
				haveOtherLED = true;
				setPermission(c, root, names[i], triggerFile, "666");
				setPermission(c, root, names[i], brightnessFile, "666");
				haveTrigger.put(names[i], new File(getPath(c, names[i], triggerFile)).exists());
				String def = options.getString(PREF_SYSTEM_TRIGGER_PREFIX+names[i], "");
				if (def.length() == 0) {
					String trigger = getActiveTrigger(names[i]);
					if (trigger != null) {
						SharedPreferences.Editor ed = options.edit();
						ed.putString(PREF_SYSTEM_TRIGGER_PREFIX+names[i], trigger);
						ed.commit();
					}
				}
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
		String[] alt = getFiles(altLEDsDirectory);
		
		if (alt.length > 0) {
			int backlightIndex;
			
			if (alt.length > 1) {
				backlightIndex = -1;
				for (int i=0; i<alt.length; i++) {
					if (alt[i].endsWith("_bl") &&
							(new File(altLEDsDirectory + "/" + alt[i] + "/brightness").exists())
					) {
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
			saveBacklight(altLEDsDirectory+"/"+alt[backlightIndex]);
			setPermission(context, root, LCD_BACKLIGHT, "666");
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
		return getPath(c, name, brightnessFile);
	}
	
	public static String getPath(Context c, String name, String file) {
		if (name.equals(LCD_BACKLIGHT)) {
			return c.getSharedPreferences(PREF_LEDS, 0).
			       getString("backlight", ledsDirectory + "/" + LCD_BACKLIGHT)
			       + "/" + file;
		}
		return ledsDirectory + "/" + name + "/" + file;
	}
	
	public void setPermissions(Root r) {
		if (names == null || !valid)
			return;
		Log.v("SuperDim", "Setting permissions");
		for (String n:names) {
			if (n.equals(LCD_BACKLIGHT)) {
				setPermission(context, r, n, "666");
			}
			else {
				setPermission(context, r, n, triggerFile, "666");
				setPermission(context, r, n, brightnessFile, "666");
			}
		}
	}
	
	public static void lock(Context c, Root r, String name) {
		setPermission(c, r, name, "444");
	}
	
	private static void setPermission(Context c, Root r, String name, String perm) {
		setPermission(c, r, name, brightnessFile, perm);
	}
	
	private static void setPermission(Context c, Root r, String name, String file, String perm) {
		String qpath = "\"" + getPath(c, name, file) + "\"";
		r.exec("chmod " + perm + " "+qpath);
	}
	
	public static void setLock(Context c, Root r, String name, boolean state) {
		setPermission(c, r, name, state ? "444" : "666");
	}
	
	public void lockTriggers(Root r) {
		SharedPreferences pref = context.getSharedPreferences(SuperDim.PREFS, 0);
		for (String led: names) {
			String value = pref.getString(SuperDim.TRIGGER_PREFIX+led, "");
			if (value.length()>0) {
				if (haveTrigger.get(led)) 
					setPermission(context, r, led, triggerFile, "444");
				else
					setPermission(context, r, led, brightnessFile, "444");
			}
		}
	}
	
	public void lockAll(Root r) {
		if (haveLCDBacklight) {
			setLock(context, r, LCD_BACKLIGHT, true);
		}
		lockTriggers(r);
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
	
	public String getActiveTrigger(String s) {
		if (! haveTrigger.get(s)) {
			return getBrightness(s) > 0 ? "on" : "off";
		}
		
		String[] triggers = getTriggers(s);
		if (triggers == null) {
			return null;
		}
		for (String t: triggers) {
			if (t.startsWith("[") && t.endsWith("]"))
				return t.substring(1,t.length()-1);
		}
		
		return null;
	}
	
	public void setTrigger(String led, String value) {
		if (value.equals("")) {
			value = options.getString(PREF_SYSTEM_TRIGGER_PREFIX+led, "");
			if (value.equals("")) {
				return;
			}
		}
		
		if (haveTrigger.get(led))
			writeLine(ledsDirectory+"/"+led+"/"+triggerFile, value);
		else
			writeLine(ledsDirectory+"/"+led+"/"+brightnessFile, value.equals("on") ? "255": "0");
	}
	
	static public String cleanTrigger(String s) {
		if (s.startsWith("[") && s.endsWith("]")) 
			return s.substring(1, s.length()-1);
		else
			return s;
	}
	
	public String[] getCleanTriggers(String s) {
		String[] triggers = getTriggers(s);
		
		if (triggers == null)
			return null;
		
		for (int i=0; i<triggers.length; i++) {
			triggers[i] = cleanTrigger(triggers[i]);
		}
		
		Arrays.sort(triggers, String.CASE_INSENSITIVE_ORDER);
		
		return triggers;
	}
	
	private String[] getTriggers(String s) {
		String line = null;
		
		if (haveTrigger.get(s)) 
			line = readLine(getPath(context, s, triggerFile));
		
		if (line == null) 
			return myTriggers;
		
		return line.split(" ");		
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
	
	public static boolean writeLine(String path, String s) {
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
			String outS = s+"\n";
			stream.write(outS.getBytes());
			stream.close();
			return true;
		} catch (Exception e) {
			Log.e("SuperDim", "Error writing "+path);
			return false;		
		}
	}
	
	public static boolean writeBrightness(String path, int n) {
		if (n<0)
			n = 0;
		else if (n>255)
			n = 255;
		
		return writeLine(path, Integer.toString(n));
	}
	
	public static String readLine(String path) {
		try {
			FileInputStream stream = new FileInputStream(path);
			byte[] buf = new byte[4096];
			String s;
			
			int numRead = stream.read(buf);
			
			stream.close();
			
			if(0 < numRead) {
				s = new String(buf, 0, numRead);
				
				return s.trim();
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static int readBrightness(String path) {
		String line = readLine(path);
		if (line == null)
			return -1;
		
		try {
			return Integer.parseInt(line);
		}
		catch (NumberFormatException e) {
			return 255;
		}
	}
	
	public int customLoad(Root root, SharedPreferences options, SharedPreferences pref, int n) {
		needRedraw = false;
		SharedPreferences.Editor ed = options.edit();		
		
		for (int i = 0 ; i < names.length; i++) {
			if (names[i].equals(Device.LCD_BACKLIGHT)) {
				continue;
			}
			String trigger = pref.getString(SuperDim.TRIGGER_PREFIX+names[i], "");
			ed.putString(SuperDim.TRIGGER_PREFIX+names[i], trigger);
			setTrigger(names[i], trigger);
		}
		
		ed.commit();
		
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
	
	public boolean customSave(Root root, SharedPreferences options, SharedPreferences.Editor ed) {
		if (haveCF3D) {
			String nm = getNightmode(cf3dNightmode);
			if ( nm != null)
				ed.putString("nightmode", nm);
		}

		for (int i=0 ; i < names.length; i++) {
//			if (! names[i].equals(Device.LCD_BACKLIGHT))
//				ed.putInt(ledPrefPrefix + names[i], getBrightness(names[i]));
			ed.putString(SuperDim.TRIGGER_PREFIX+names[i], 
					options.getString(SuperDim.TRIGGER_PREFIX+names[i], ""));
					
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
