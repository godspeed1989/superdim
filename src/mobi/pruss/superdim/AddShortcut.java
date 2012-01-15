package mobi.pruss.superdim;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class AddShortcut extends Activity {
	public final static String LOAD_CUSTOM="loadCustom";
	public final static int SET_AUTOMATIC=10000;
	public final static int CYCLE=10001;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent i = getIntent();
		Log.v("AddShortcut", i.getAction());
		Log.v("AddShortcut", i.getComponent().getShortClassName());
		
		if (i.getAction().equals(Intent.ACTION_CREATE_SHORTCUT) && 
			i.getComponent() != null) {
			String shortClass = i.getComponent().getShortClassName();
			if (shortClass.endsWith(".CreateShortcut0"))
				addShortcut(0, 
						getCustomName(0), 
						R.drawable.custom0);
			else if (shortClass.endsWith(".CreateShortcut1"))
				addShortcut(1, getCustomName(1), R.drawable.custom1);
			else if (shortClass.endsWith(".CreateShortcut2"))
				addShortcut(2, getCustomName(2), R.drawable.custom2);
			else if (shortClass.endsWith(".CreateShortcut3"))
				addShortcut(3, getCustomName(3), R.drawable.custom3);
			else if (shortClass.endsWith(".CreateShortcut4"))
				addShortcut(4, getCustomName(4), R.drawable.custom4);
			else if (shortClass.endsWith(".CreateShortcutAuto"))
				addShortcut(SET_AUTOMATIC, "Auto Brightness", R.drawable.icon);
			else if (shortClass.endsWith(".CreateShortcutCycle"))
				addShortcut(CYCLE, "Cycle", R.drawable.icon);
		    finish();
		    return;
		}
		finish();
	}

	String getCustomName(int customNumber) {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(Options.PREF_PRESET_NAME_PREFIX+customNumber, 
				Options.OPT_PRESET_NAME[customNumber]);
	}

	void addShortcut(int customNumber, String name, int icon) {
		Log.v("AddShortcut", name);
		Intent si = new Intent(this, SuperDim.class);
		si.putExtra(LOAD_CUSTOM, customNumber);
		si.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		Intent i = new Intent();
		i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, si);
		i.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
		i.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, 
				Intent.ShortcutIconResource.fromContext(this, icon));
		setResult(RESULT_OK, i);
	}
}
