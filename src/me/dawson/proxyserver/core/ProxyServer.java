package me.dawson.proxyserver.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

import android.util.Log;

public class ProxyServer {
	public static final String TAG = "ProxyServer";
	private static final int DEFAULT_PORT = 8964;
	private static final int MAX_PORT = 20146; // real max can be 65535

	private static volatile ProxyServer instance;

	public static ProxyServer getInstance() {
		synchronized (ProxyServer.class) {
			if (instance == null) {
				instance = new ProxyServer();
			}
		}
		return instance;
	}

	private int port;
	private boolean running;
	private Selector selector;
	private ServerSocketChannel server;

	private ProxyServer() {
		port = DEFAULT_PORT;
		running = false;
	}

	public int getPort() {
		return port;
	}

	public Selector getSeletor() {
		return selector;
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized boolean start() {
		if (running) {
			return false;
		}

		Log.d(TAG, "start proxy server");
		try {
			selector = Selector.open();
		} catch (Exception e) {
			Log.e(TAG, "create selector exception", e);
			return false;
		}

		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
		} catch (Exception e) {
			Log.e(TAG, "create server channel exception", e);
			return false;
		}

		while (true && port < MAX_PORT) {
			try {
				server.socket().bind(new InetSocketAddress(port));
			} catch (IOException e) {
				++port;
				continue;
			}
			Log.d(TAG, "proxy server listen port " + port);
			break;
		}

		if (port >= MAX_PORT) {
			return false;
		}

		try {
			server.register(selector, SelectionKey.OP_ACCEPT);
		} catch (ClosedChannelException e) {
			Log.e(TAG, "register selector exception", e);
			return false;
		}

		running = true;
		Thread t = new Thread(new Runnable() {
			public void run() {
				doProxy();
				running = false;
			}
		});
		t.setDaemon(false);
		t.setName("ProxyServer");
		t.start();
		return true;
	}

	public synchronized boolean stop() {
		if (!running) {
			return false;
		}

		Log.d(TAG, "stop proxy server");
		running = false;

		try {
			selector.wakeup();
			selector.close();
			selector = null;
		} catch (Exception e) {
			Log.e(TAG, "close selector exception.", e);
		}

		try {
			server.close();
			server = null;
		} catch (IOException e) {
			Log.e(TAG, "close server exception.", e);
		}
		return true;
	}

	private void doProxy() {
		Log.d(TAG, "do proxy server start");
		while (true) {
			if (server == null || selector == null) {
				break;
			}

			Set<SelectionKey> keys = null;
			try {
				selector.select();
				if (!selector.isOpen()) {
					break;
				}
				keys = selector.selectedKeys();
			} catch (Exception e) {
				Log.e(TAG, "selector select exception", e);
				continue;
			}

			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();

				Object attr = key.attachment();
				ChannelPair cp = null;
				if (attr instanceof ChannelPair) {
					cp = (ChannelPair) attr;
				} else {
					cp = new ChannelPair();
				}
				try {
					cp.handleKey(key);
				} catch (Exception e) {
					// catch handle key exception
				}
			}

		}
		Log.d(TAG, "do proxy server finish");
	}
}
