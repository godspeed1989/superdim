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
import android.content.SharedPreferences;
import android.util.Log;
import android.view.WindowManager;

public class Device {
	private static final boolean SAVE_ONLY_BACKLIGHT_LED = true;

	private static final String ledsDirectory = "/sys/class/leds";
	private static final String brightnessFile = "brightness";

	public static final String LCD_BACKLIGHT = "lcd-backlight";
	public boolean haveLCDBacklight;
	public boolean haveOtherLED;
	private Activity context;
	public String[] names;
	private boolean setManual;
	static final String cf3dNightmode="persist.cf3d.nightmode";
	static final String cmNightmode="debug.sf.render_effect";
	private static final String cmNightmode_red="debug.sf.render_color_red";
	private static final String cmNightmode_green="debug.sf.render_color_green";
	private static final String cmNightmode_blue="debug.sf.render_color_blue";
	private static final String ledPrefPrefix = "leds/";
	public boolean needRedraw;
	public boolean haveCF3D;
	public boolean haveCF3DPaid;
	public boolean haveCM;
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

	public static final int cmNightmodeLabels[] = {R.string.nightmode_disabled,
		R.string.nightmode_red, R.string.nightmode_green, R.string.nightmode_blue, 
		R.string.nightmode_amber, R.string.nightmode_salmon,
		R.string.nightmode_fuchsia };
	private static final int CM_FIRST_CUSTOM_MODE = 7;
	private static final String defaultCF3DNightmode[] = { "disabled", "red", "green", "green", "disabled" };
	private static final int defaultCMNightmode[] = { 0, 1, 2, 2, 0};
	private static final int[] defaultBacklight = 
		{ 50, 10, 50, 200, 255 };
	public boolean valid;
	
	public Device(Activity c, Root root) {
		context = c;
		haveLCDBacklight = false;
		haveOtherLED     = false;
		setManual 		 = false;
		needRedraw		 = false;

		valid		     = false;

		names = getFiles(root, ledsDirectory);
		
		if (names == null)
			return;
		
		for (int i=0; i<names.length; i++) {
			setPermission(root, names[i]);
			if (names[i].equals(LCD_BACKLIGHT)) {
				haveLCDBacklight = true;
			}
			else {
				haveOtherLED = true;
			}
		}

		Arrays.sort(names);
		
		detectNightmode();

		valid = true;
	}
	
	private void deleteIfExists(String s) {
		File f = new File(s);
		if (f.exists())
			f.delete();
	}
	
	private String[] getFiles(Root root, String dir) {
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
        	haveCM = false;
		}
        else {
        	haveCM = (getNightmode(cmNightmode) != null);
        }

        }
	
	public static String getBrightnessPath(String name) {
		return ledsDirectory + "/" + name + "/" + brightnessFile;
	}
	
	public void setPermissions(Root r) {
		if (names == null || !valid)
			return;
		Log.v("SuperDim", "Setting permissions");
		for (String n:names)
			setPermission(r, n);
	}
	
	private static void setPermission(Root r, String name) {
		String qpath = "\"" + getBrightnessPath(name) + "\"";

// In theory, this should work, but in practice, some devices seem to
// report the LCD_BRIGHTNESS file as writeable though it's not.
//		File f = new File(getBrightnessPath(name));
//		if (f.canWrite())
//			return;
		
// This might be a better way of doing this, but is probably a bit less
// trustworthy.		
//		r.exec("chown 1000."+android.os.Process.myUid()+" "+qpath+";" + 
//				"chmod 664 "+qpath);
//		r.exec("echo chmod 666 "+qpath);		
		
//		r.exec("echo Doing: chmod 666 "+qpath);
		r.exec("chmod 666 "+qpath);
	}	
	
	public static void unsetPermission(Root r, String name) {		
		String qpath = "\"" + getBrightnessPath(name) + "\"";
		r.exec("chmod 644 "+qpath); 
	}
	
	public void close(Root r) {
		for (String n:names) {
			unsetPermission(r,n);
		}
	}
	
	public static boolean getSafeMode(Context c) {
		return c.getSharedPreferences("safeMode",0).getBoolean("safeMode", false);
	}
	
	public static void setSafeMode(Context c, boolean value) {
		SharedPreferences.Editor ed = c.getSharedPreferences("safeMode",0).edit();
		ed.putBoolean("safeMode", value);
		ed.commit();
	}
	
	public void setBrightness(String name, int n) {
		String path = getBrightnessPath(name);
		
		if (name.equals(LCD_BACKLIGHT)) {
			ContentResolver cr = context.getContentResolver();
			
			if (8<=android.os.Build.VERSION.SDK_INT && ! setManual) {
				android.provider.Settings.System.putInt(cr, 
					android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
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
		String path = getBrightnessPath(name);
	
		if ((new File(path)).exists()) { 
			return readBrightness(getBrightnessPath(name));
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
		
		Log.v("SuperDim", "Request write to "+path);
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
			String nm = pref.getString("nightmode", defaultCF3DNightmode[n]);
			if (! nm.equals(oldNM)) {
				setNightmode(root, cf3dNightmode, nm);
				needRedraw = true;
			}
			
		}
		else if (haveCM) {
			String oldNM = getNightmode(cmNightmode);
			String oldR  = getNightmode(cmNightmode_red);
			String oldG  = getNightmode(cmNightmode_green);
			String oldB  = getNightmode(cmNightmode_blue);
			String nm = pref.getString("cm_nightmode", ""+defaultCMNightmode[n]);
			String r  = pref.getString("cm_nightmode_red", "975"); 
			String g  = pref.getString("cm_nightmode_green", "937"); 
			String b  = pref.getString("cm_nightmode_blue", "824"); 
			if (! nm.equals(oldNM) || 
				(Integer.parseInt(oldNM) >= CM_FIRST_CUSTOM_MODE && 
				( ! r.equals(oldR) || ! g.equals(oldG) || ! b.equals(oldB) ) 		
				) ) {
				
				setNightmode(root, cmNightmode, nm);
				if (Integer.parseInt(nm) >= CM_FIRST_CUSTOM_MODE) {
					setNightmode(root, cmNightmode_red, r);
					setNightmode(root, cmNightmode_green, g);
					setNightmode(root, cmNightmode_blue, b);
				}
				
				needRedraw = true;
			}
		}

		return br;
	}
	
	public boolean customSave(Root root, SharedPreferences pref) {
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
			if (Integer.parseInt(nm) >= CM_FIRST_CUSTOM_MODE) {
				String c = getNightmode(cmNightmode_red);
				if ( c != null)
					ed.putString("cm_nightmode_red", c);
				c = getNightmode(cmNightmode_green);
				if ( c != null)
					ed.putString("cm_nightmode_green", c);
				c = getNightmode(cmNightmode_blue);
				if ( c != null)
					ed.putString("cm_nightmode_blue", c);
			}
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

	static void setNightmode(Root root, String nmType, String s) {
		root.exec("setprop " + nmType + " "+s);
	}
}
