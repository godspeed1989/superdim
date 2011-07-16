package mobi.pruss.superdim;

import java.io.DataOutputStream;
import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;
	private static final boolean LOG_SU = false;
	
	public Root() {
		this(false);
	}

	public Root(boolean output) {
		try {
			if (output) {
				rootShell = Runtime.getRuntime().exec("su");
			}
			else {
				String[] cmds = { "sh", "-c", 
				LOG_SU ?		
				"su >> /tmp/superdim.txt 2>> /tmp/superdim.txt" 
				: "su > /dev/null 2> /dev/null" 		
				};
				rootShell = Runtime.getRuntime().exec(cmds);
			}
			
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
		}
		catch (Exception e) {
			rootCommands = null;
		}
	}
	
	public static boolean test() {
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
	
	public void close() {
		if (rootCommands != null) {
			try {
				rootCommands.close();
			}
			catch (Exception e) {
			}
			rootCommands = null;
		}
	}
	
	public void exec( String s ) {
		try {
			Log.v("root", s);
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
		}
		catch (Exception e) {
			Log.e("Error executing",s);
		}
	}
}
