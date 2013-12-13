package com.ru426.android.xposed.parts.power_button;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ru426.android.xposed.library.ModuleBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AndroidPolicyModule extends ModuleBase {
	private static final String TAG = AndroidPolicyModule.class.getSimpleName();
	
	public static final String STATE_CHANGE = AndroidPolicyModule.class.getName() + ".intent.action.STATE_CHANGE";
	public static final String STATE_EXTRA_ADD_REBOOT = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_REBOOT";
	public static final String STATE_EXTRA_ADD_SOFT_REBOOT = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_SOFT_REBOOT";
	public static final String STATE_EXTRA_ADD_RECOVERY = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_RECOVERY";
	public static final String STATE_EXTRA_SHOW_POWEROFF = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_SHOW_POWEROFF";
	public static final String STATE_EXTRA_SHOW_AIRPLANE = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_SHOW_AIRPLANE";
	public static final String STATE_EXTRA_SHOW_SCREENSHOT = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_SHOW_SCREENSHOT";
	public static final String STATE_EXTRA_ADD_REBOOT_DIALOG = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_REBOOT_DIALOG";
	public static final String STATE_EXTRA_ADD_SOFT_REBOOT_DIALOG = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_SOFT_REBOOT_DIALOG";
	public static final String STATE_EXTRA_ADD_RECOVERY_DIALOG = AndroidPolicyModule.class.getName() + ".intent.extra.STATE_EXTRA_ADD_RECOVERY_DIALOG";
	
	
	boolean add_reboot_to_power_menu = false;	
	boolean add_recovery_to_power_menu = false;
	boolean add_softreboot_to_power_menu = false;
	boolean show_poweroff_to_power_menu = true;				
	boolean show_airplane_to_power_menu = true;
	boolean show_screenshot_to_power_menu = true;
	static boolean add_reboot_dialog_to_power_menu = true;
	static boolean add_softreboot_dialog_to_power_menu = true;
	static boolean add_recovery_dialog_to_power_menu = true;

	@Override
	public void init(final XSharedPreferences prefs, final ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		add_reboot_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.add_reboot_to_power_menu_key), false);
		add_softreboot_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.add_softreboot_to_power_menu_key), false);
		add_recovery_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.add_recovery_to_power_menu_key), false);		
		show_poweroff_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.show_poweroff_to_power_menu_key), true);
		show_airplane_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.show_airplane_to_power_menu_key), true);
		show_screenshot_to_power_menu = (Boolean) xGetValue(prefs, xGetString(R.string.show_screenshot_to_power_menu_key), true);
		add_reboot_dialog_to_power_menu = (Boolean) xGetValue(xSharedPreferences, xGetString(R.string.add_reboot_dialog_to_power_menu_key), true);
		add_softreboot_dialog_to_power_menu = (Boolean) xGetValue(xSharedPreferences, xGetString(R.string.add_softreboot_dialog_to_power_menu_key), true);
		add_recovery_dialog_to_power_menu = (Boolean) xGetValue(xSharedPreferences, xGetString(R.string.add_recovery_dialog_to_power_menu_key), true);

		Class<?> xGlobalActions = XposedHelpers.findClass("com.android.internal.policy.impl.GlobalActions", null);
        final Class<?> xGlobalActionsAction = XposedHelpers.findClass("com.android.internal.policy.impl.GlobalActions.Action", null);
		Object callback[] = new Object[1];
		callback[0] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try {
					xLog(TAG + " : " + "afterHookedMethod createDialog");
					if(XposedHelpers.getObjectField(param.thisObject, "mContext") != null){
						mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
						IntentFilter intentFilter = new IntentFilter();
						intentFilter.addAction(STATE_CHANGE);
						xRegisterReceiver(mContext, intentFilter);
						
						Configuration config = new Configuration();
						config.locale = Locale.getDefault();
					    xModuleResources.updateConfiguration(config, null);
						
						@SuppressWarnings("unchecked")
						ArrayList<Object> mItems = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mItems");
						Object powerOffAction = null;
						Object rebootAction = null;
						Object recoveryAction = null;
						Object airplaneAction = null;
						Object screenshotAction = null;
						Resources res = mContext.getResources();
						for (Object mSinglePressAction : mItems) {
							try {
	                            Field mIconResId = XposedHelpers.findField(mSinglePressAction.getClass(), "mIconResId");
	                            String mIconResName = res.getResourceEntryName((Integer) mIconResId.get(mSinglePressAction)).toLowerCase(Locale.US);
	                            Field mMessageResId = XposedHelpers.findField(mSinglePressAction.getClass(), "mMessageResId");
	                            String mMessageResName = res.getResourceEntryName((Integer) mMessageResId.get(mSinglePressAction)).toLowerCase(Locale.US);	                            
	                            if (mIconResName.contains("power_off") || mMessageResName.contains("power_off")) {
	                            	powerOffAction = mSinglePressAction;
	                            }
	                            if (mIconResName.contains("reboot") || mIconResName.contains("restart") || mMessageResName.contains("reboot") || mMessageResName.contains("restart")) {
	                            	rebootAction = mSinglePressAction;
	                            }
	                            if (mIconResName.contains("recovery") || mMessageResName.contains("recovery")) {
	                            	recoveryAction = mSinglePressAction;
	                            }
	                            if (mIconResName.contains("screenshot") || mMessageResName.contains("screenshot")) {
	                            	screenshotAction = mSinglePressAction;
	                            }
	                        } catch (NoSuchFieldError e) {
	                        } catch (Resources.NotFoundException e) {
	                        } catch (IllegalArgumentException e) {
	                        }
							
							try {
	                            Field mIconResId = XposedHelpers.findField(mSinglePressAction.getClass(), "mEnabledIconResId");
	                            String mIconResName = res.getResourceEntryName((Integer) mIconResId.get(mSinglePressAction)).toLowerCase(Locale.US);
	                            Field mMessageResId = XposedHelpers.findField(mSinglePressAction.getClass(), "mMessageResId");
	                            String mMessageResName = res.getResourceEntryName((Integer) mMessageResId.get(mSinglePressAction)).toLowerCase(Locale.US);	                            
	                            if (mIconResName.contains("airplane") || mMessageResName.contains("airplane")) {
	                            	airplaneAction = mSinglePressAction;
	                            }
	                        } catch (NoSuchFieldError e) {
	                        } catch (Resources.NotFoundException e) {
	                        } catch (IllegalArgumentException e) {
	                        }
						}
												
						if (add_recovery_to_power_menu) {							
							if(recoveryAction != null){
								mItems.remove(recoveryAction);
							}
							Object mRecoveryAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { xGlobalActionsAction }, new RebootAction(xModuleResources.getDrawable(R.drawable.ic_lock_recovery), xModuleResources.getString(R.string.global_action_recovery)));
							mItems.add(show_poweroff_to_power_menu?1:0, mRecoveryAction);
						}
						if(add_softreboot_to_power_menu) {
							Object mSoftRebootAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { xGlobalActionsAction }, new RebootAction(xModuleResources.getDrawable(R.drawable.ic_lock_reboot_soft), xModuleResources.getString(R.string.global_action_softreboot)));
							mItems.add(show_poweroff_to_power_menu?1:0, mSoftRebootAction);
						}
						if(add_reboot_to_power_menu) {
							if(rebootAction != null){
								mItems.remove(rebootAction);
							}
							Object mRebootAction = Proxy.newProxyInstance(classLoader, new Class<?>[] { xGlobalActionsAction }, new RebootAction(xModuleResources.getDrawable(R.drawable.ic_lock_reboot), xModuleResources.getString(R.string.global_action_reboot)));
							mItems.add(show_poweroff_to_power_menu?1:0, mRebootAction);
						}
						if(!show_poweroff_to_power_menu) mItems.remove(powerOffAction);
						if(!show_airplane_to_power_menu) mItems.remove(airplaneAction);
                    	if(!show_screenshot_to_power_menu) mItems.remove(screenshotAction);

                        BaseAdapter mAdapter = (BaseAdapter) XposedHelpers.getObjectField(param.thisObject, "mAdapter");
                        mAdapter.notifyDataSetChanged();
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}
		};
		xHookMethod(xGlobalActions, "createDialog", callback, (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_powerbutton_key), false));
	}
	
	static class RebootAction implements InvocationHandler {
        private Context mContext;
        private Drawable mRebootIcon;
        private String mRebootStr;
        public RebootAction(Drawable mRebootIcon, String mRebootStr) {
        	this.mRebootIcon = mRebootIcon;
        	this.mRebootStr = mRebootStr;
        }

        @Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("create")) {
                mContext = (Context) args[0];
                Resources res = mContext.getResources();
                LayoutInflater inflater = (LayoutInflater) args[3];
                int layoutId = res.getIdentifier("global_actions_item", "layout", "android");
                View view = inflater.inflate(layoutId, (ViewGroup) args[2], false);

                ImageView icon = (ImageView) view.findViewById(res.getIdentifier("icon", "id", "android"));
                icon.setImageDrawable(mRebootIcon);

                TextView messageView = (TextView) view.findViewById(res.getIdentifier("message", "id", "android"));
                messageView.setText(mRebootStr);

                TextView statusView = (TextView) view.findViewById(res.getIdentifier("status", "id", "android"));
                statusView.setVisibility(View.GONE);

                return view;
            } else if (methodName.equals("onPress")) {
				try {					
					String title = "";
					String message = "";
					if(mRebootStr.equals(xModuleResources.getString(R.string.global_action_reboot))){
						title = xModuleResources.getString(R.string.global_action_reboot);
						message = xModuleResources.getString(R.string.global_action_reboot_message);
						if(add_reboot_dialog_to_power_menu){
							showConfirmDialog(mContext, title, message);
						} else {
							handleReboot(title);
						}
					}else if(mRebootStr.equals(xModuleResources.getString(R.string.global_action_softreboot))){
						title = xModuleResources.getString(R.string.global_action_softreboot);
						message = xModuleResources.getString(R.string.global_action_softreboot_message);
						if(add_softreboot_dialog_to_power_menu){
							showConfirmDialog(mContext, title, message);
						} else {
							handleReboot(title);
						}
					}else if(mRebootStr.equals(xModuleResources.getString(R.string.global_action_recovery))){
						title = xModuleResources.getString(R.string.global_action_recovery);
						message = xModuleResources.getString(R.string.global_action_recovery_message);
						if(add_recovery_dialog_to_power_menu){
							showConfirmDialog(mContext, title, message);
						} else {
							handleReboot(title);
						}					
					}
				} catch (Exception e) {
					XposedBridge.log(e);
				}
                return null;
            } else if (methodName.equals("onLongPress")) {
                return true;
            } else if (methodName.equals("showDuringKeyguard")) {
                return true;
            } else if (methodName.equals("showBeforeProvisioning")) {
                return true;
            } else if (methodName.equals("isEnabled")) {
                return true;
            } else {
                return null;
            }
        }
    }
	
	static void showConfirmDialog(Context context, final String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								handleReboot(title);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		AlertDialog dialog = builder.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		dialog.show();
	}
	
	static void handleReboot(String title){
		if(title == null || title.length() == 0) return;
		try {
			if(title.equals(xModuleResources.getString(R.string.global_action_reboot))){
				final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
	            pm.reboot("now");
			}else if(title.equals(xModuleResources.getString(R.string.global_action_softreboot))){
				Process proc = Runtime.getRuntime().exec("sh");
				DataOutputStream stdin = new DataOutputStream(proc.getOutputStream());
				stdin.writeBytes(
						"setprop ctl.restart surfaceflinger\n" +
	                    "setprop ctl.restart zygote\n");
			}else if(title.equals(xModuleResources.getString(R.string.global_action_recovery))){
				Process proc = Runtime.getRuntime().exec("sh");
				DataOutputStream stdin = new DataOutputStream(proc.getOutputStream());
				stdin.writeBytes(
						"mkdir -p /cache/recovery\n" +
	                    "touch /cache/recovery/boot\n");
				final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
				pm.reboot("recovery");
			}
		} catch (IOException e) {
			XposedBridge.log(e);
		}
	}

	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + "OnReceive " + intent.getAction());
		if (intent.getAction().equals(STATE_CHANGE)) {			
			add_reboot_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_REBOOT, false);
			add_softreboot_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_SOFT_REBOOT, false);
			add_recovery_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_RECOVERY, false);
			show_poweroff_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_SHOW_POWEROFF, true);
			show_airplane_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_SHOW_AIRPLANE, true);
			show_screenshot_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_SHOW_SCREENSHOT, true);
			add_reboot_dialog_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_REBOOT_DIALOG, true);
			add_softreboot_dialog_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_SOFT_REBOOT_DIALOG, true);
			add_recovery_dialog_to_power_menu = intent.getBooleanExtra(STATE_EXTRA_ADD_RECOVERY_DIALOG, true);
		}
	}
}
