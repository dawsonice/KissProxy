package me.dawson.proxyserver.ui;

import me.dawson.proxyserver.core.ProxyServer;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class ProxyService extends Service {
	public static final String TAG = "ProxyService";

	@Override
	public IBinder onBind(Intent binder) {
		return new IProxyControl.Stub() {
			@Override
			public boolean start() throws RemoteException {
				return doStart();
			}

			@Override
			public boolean stop() throws RemoteException {
				return doStop();
			}

			@Override
			public boolean isRunning() throws RemoteException {
				return ProxyServer.getInstance().isRunning();
			}

			@Override
			public int getPort() throws RemoteException {
				return ProxyServer.getInstance().getPort();
			}

		};
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private boolean doStart() {
		ProxyServer proxyServer = ProxyServer.getInstance();
		if (proxyServer.isRunning()) {
			return false;
		}

		return proxyServer.start();
	}

	private boolean doStop() {
		ProxyServer proxyServer = ProxyServer.getInstance();
		if (!proxyServer.isRunning()) {
			return false;
		}

		return proxyServer.stop();
	}

}
