package mobi.pruss.superdim;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.view.WindowManager;

public class LEDs {
	static final String ledsDirectory = "/sys/class/leds";
	static final String brightnessFile = "brightness";

	public static final String LCD_BACKLIGHT = "lcd-backlight";
	public boolean haveLCDBacklight;
	public boolean haveOtherLED;
	private Activity context;
	public String[] names;
	
	public LEDs(Activity c, Root root) {
		context = c;
		haveLCDBacklight = false;
		haveOtherLED     = false;		

		try {
			FileFilter directoryFilter = new FileFilter() {
				public boolean accept(File file) {				
					return file.isDirectory() &&
						0 <= readBrightness(file.getPath() + "/" + brightnessFile);				
				}
			};		
		
			File dir = new File(ledsDirectory);
			File[] files = dir.listFiles(directoryFilter);		
			names = new String[files.length];
			
			for (int i=0; i<files.length; i++) {
				names[i] = files[i].getName();
				setPermission(root, files[i].getPath());
				if (names[i].equals(LCD_BACKLIGHT)) {
					haveLCDBacklight = true;
				}
				else {
					haveOtherLED = true;
				}
			}
			
			Arrays.sort(names);
		}
		catch(Exception e) {
			names = new String[0];
		}
	
	}
	
	public static String getBrightnessPath(String name) {
		return ledsDirectory + "/" + name + "/" + brightnessFile;
	}
	
	public static void setPermission(Root r, String name) {
		String qpath = "\"" + getBrightnessPath(name) + "\"";
		r.exec("chgrp "+android.os.Process.myUid()+" "+qpath+";" +
				"chmod g+w "+qpath); 
	}	
	
	public void setBrightness(String name, int n) {
		String path = getBrightnessPath(name);
		
		if (name.equals(LCD_BACKLIGHT)) {
			android.provider.Settings.System.putInt(context.getContentResolver(),
				     android.provider.Settings.System.SCREEN_BRIGHTNESS,
				     n);
			if (! (new File(path)).exists() ) {
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
		
		if (!f.exists()) {
			return false;
		}
		
		if (!f.canWrite())
			return false;
		
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
}
