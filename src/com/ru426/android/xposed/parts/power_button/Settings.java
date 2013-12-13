package com.ru426.android.xposed.parts.power_button;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Settings extends PreferenceActivity {
	private static Context mContext;
	private static SharedPreferences prefs;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_fragment_power_button);
	    init();
	    initOption();
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}
	
	@SuppressWarnings("deprecation")
	private void init(){
		boolean isHookPowerButton = ((CheckBoxPreference) findPreference(getString(R.string.is_hook_powerbutton_key))).isChecked();
		PreferenceCategory powerButtonSettings = (PreferenceCategory) findPreference(getString(R.string.powerbutton_settings_key));		
		powerButtonSettings.setEnabled(isHookPowerButton);
		
		String key = getString(R.string.add_reboot_dialog_to_power_menu_key);
		boolean checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
		key = getString(R.string.add_softreboot_dialog_to_power_menu_key);
		checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
		key = getString(R.string.add_recovery_dialog_to_power_menu_key);
		checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
		key = getString(R.string.show_poweroff_to_power_menu_key);
		checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
		key = getString(R.string.show_airplane_to_power_menu_key);
		checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
		key = getString(R.string.show_screenshot_to_power_menu_key);
		checked = prefs.getBoolean(key, true);
		((CheckBoxPreference) findPreference(key)).setChecked(checked);
	}
	
	@SuppressWarnings("deprecation")
	private void initOption(){
		showHomeButton();
		setPreferenceChangeListener(getPreferenceScreen());
	}

	private static void setPreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory) preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory) preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(onPreferenceChangeListener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(onPreferenceChangeListener);
			}
		}
	}
	
	private static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			switch(preference.getTitleRes()){
			case R.string.is_hook_powerbutton_title:
				if(!prefs.getBoolean(preference.getKey(), false) && (Boolean) newValue){
					showRestartToast();
				}
				PreferenceCategory powerButtonSettings = (PreferenceCategory) preference.getPreferenceManager().findPreference(mContext.getString(R.string.powerbutton_settings_key));		
				powerButtonSettings.setEnabled((Boolean) newValue);
				break;
			case R.string.add_reboot_to_power_menu_title:
			case R.string.add_softreboot_to_power_menu_title:
			case R.string.add_recovery_to_power_menu_title:
			case R.string.show_poweroff_to_power_menu_title:
			case R.string.show_airplane_to_power_menu_title:
			case R.string.show_screenshot_to_power_menu_title:
			case R.string.add_reboot_dialog_to_power_menu_title:
			case R.string.add_softreboot_dialog_to_power_menu_title:
			case R.string.add_recovery_dialog_to_power_menu_title:
				sendPowerButtonStateChangeIntent(preference, (Boolean) newValue);
				break;
			default:
				return false;
			}
			return true;
		}		
	};
	
	private static void sendPowerButtonStateChangeIntent(Preference preference, boolean newValue){
		Intent intent = new Intent(AndroidPolicyModule.STATE_CHANGE);
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_REBOOT, preference.getTitleRes() == R.string.add_reboot_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_reboot_to_power_menu_key), false));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_SOFT_REBOOT, preference.getTitleRes() == R.string.add_softreboot_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_softreboot_to_power_menu_key), false));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_RECOVERY, preference.getTitleRes() == R.string.add_recovery_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_recovery_to_power_menu_key), false));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_SHOW_POWEROFF, preference.getTitleRes() == R.string.show_poweroff_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.show_poweroff_to_power_menu_key), true));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_SHOW_AIRPLANE, preference.getTitleRes() == R.string.show_airplane_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.show_airplane_to_power_menu_key), true));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_SHOW_SCREENSHOT, preference.getTitleRes() == R.string.show_screenshot_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.show_screenshot_to_power_menu_key), true));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_REBOOT_DIALOG, preference.getTitleRes() == R.string.add_reboot_dialog_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_reboot_dialog_to_power_menu_key), true));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_SOFT_REBOOT_DIALOG, preference.getTitleRes() == R.string.add_softreboot_dialog_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_softreboot_dialog_to_power_menu_key), true));
		intent.putExtra(AndroidPolicyModule.STATE_EXTRA_ADD_RECOVERY_DIALOG, preference.getTitleRes() == R.string.add_recovery_dialog_to_power_menu_title ? newValue : prefs.getBoolean(mContext.getString(R.string.add_recovery_dialog_to_power_menu_key), true));
		mContext.sendBroadcast(intent);
	}
}
