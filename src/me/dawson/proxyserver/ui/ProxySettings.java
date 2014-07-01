package me.dawson.proxyserver.ui;

import me.dawson.proxyserver.R;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ProxySettings extends Activity implements ServiceConnection,
		OnCheckedChangeListener {
	public static final String TAG = "ProxySettings";

	protected static final String KEY_PREFS = "proxy_pref";
	protected static final String KEY_ENABALE = "proxy_enable";

	private static int NOTIFICATION_ID = 20140701;

	private IProxyControl proxyControl = null;

	private TextView tvInfo;
	private CheckBox cbEnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.proxy_settings);

		tvInfo = (TextView) findViewById(R.id.tv_info);
		cbEnable = (CheckBox) findViewById(R.id.cb_enable);
		cbEnable.setOnCheckedChangeListener(this);

		Intent intent = new Intent(this, ProxyService.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onServiceConnected(ComponentName cn, IBinder binder) {
		proxyControl = (IProxyControl) binder;
		if (proxyControl != null) {
			updateProxy();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName cn) {
		proxyControl = null;
	}

	@Override
	protected void onDestroy() {
		unbindService(this);
		super.onDestroy();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		SharedPreferences sp = getSharedPreferences(KEY_PREFS, MODE_PRIVATE);
		sp.edit().putBoolean(KEY_ENABALE, isChecked).commit();
		updateProxy();
	}

	private void updateProxy() {
		if (proxyControl == null) {
			return;
		}

		boolean isRunning = false;
		try {
			isRunning = proxyControl.isRunning();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		boolean shouldRun = getSharedPreferences(KEY_PREFS, MODE_PRIVATE)
				.getBoolean(KEY_ENABALE, false);
		if (shouldRun && !isRunning) {
			startProxy();
		} else if (!shouldRun && isRunning) {
			stopProxy();
		}

		try {
			isRunning = proxyControl.isRunning();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (isRunning) {
			tvInfo.setText(R.string.proxy_on);
			cbEnable.setChecked(true);
		} else {
			tvInfo.setText(R.string.proxy_off);
			cbEnable.setChecked(false);
		}
	}

	private void startProxy() {
		boolean started = false;
		try {
			started = proxyControl.start();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!started) {
			return;
		}

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Context context = getApplicationContext();

		Notification notification = new Notification();
		notification.icon = R.drawable.icon_launcher;
		notification.tickerText = getResources().getString(R.string.proxy_on);
		notification.when = System.currentTimeMillis();

		CharSequence contentTitle = getResources().getString(R.string.app_name);
		;
		CharSequence contentText = getResources().getString(
				R.string.service_text);
		Intent intent = new Intent(this, ProxySettings.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				pendingIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		manager.notify(NOTIFICATION_ID, notification);

		Toast.makeText(this, getResources().getString(R.string.proxy_started),
				Toast.LENGTH_SHORT).show();
	}

	private void stopProxy() {
		boolean stopped = false;

		try {
			stopped = proxyControl.stop();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!stopped) {
			return;
		}

		tvInfo.setText(R.string.proxy_off);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
		Toast.makeText(this, getResources().getString(R.string.proxy_stopped),
				Toast.LENGTH_SHORT).show();
	}
}