package mobi.pruss.superdim;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Arrays;

import android.content.ContentResolver;
import android.util.Log;

public class LEDs {
	static final String ledsDirectory = "/sys/class/leds";
	static final String brightnessFile = "brightness";
	public static final String LCD_BACKLIGHT = "lcd-backlight";
	
	private static String getBrightnessPath(String name) {
		return ledsDirectory + "/" + name + "/" + brightnessFile;
	}
	
	public static void setBrightness(Root root, ContentResolver cr, String name, int n) {
		if (name.equals(LCD_BACKLIGHT)) 
			android.provider.Settings.System.putInt(cr,
				     android.provider.Settings.System.SCREEN_BRIGHTNESS,
				     n);
		
		writeBrightness(root, getBrightnessPath(name), n);
	}
	
	public static int getBrightness(String name) {
		return readBrightness(getBrightnessPath(name));
	}
	
	public static void writeBrightness(Root root, String path, int n) {
		if (n<0)
			n = 0;
		else if (n>255)
			n = 255;
		
		root.exec("echo "+n+" >\""+path+"\"");		
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
	
	public static String[] getNames() {
		try {
			FileFilter directoryFilter = new FileFilter() {
				public boolean accept(File file) {				
					return file.isDirectory() &&
						0 <= readBrightness(file.getPath() + "/" + brightnessFile);				
				}
			};		
		
			File dir = new File(ledsDirectory);
			File[] files = dir.listFiles(directoryFilter);		
			String[] names = new String[files.length];
			
			for (int i=0; i<files.length; i++)
				names[i] = files[i].getName();
			
			Arrays.sort(names);
			
			for (int i=0; i<names.length; i++)
				Log.v("Have", names[i]);
			
			return names;
		}
		catch(Exception e) {
			return new String[0];
		}
	}
}
