package me.dawson.proxyserver.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import me.dawson.proxyserver.core.Channel.Status;
import android.util.Log;

public class ChannelPair implements ChannelListener {
	public static final String TAG = "ChannelPair";

	private static final String CRLF = "\r\n";
	private static final String CONNECT_OK = "HTTP/1.0 200 Connection Established"
			+ CRLF + "Proxy-agent: KissProxy" + CRLF + CRLF;

	private Channel requestChannel;
	private Channel responseChannel;

	public ChannelPair() {
		//
	}

	public void handleKey(SelectionKey key) {
		if (key == null) {
			return;
		}

		if (!key.isValid()) {
			Log.d(TAG, "close invalid socket.");
			close();
			return;
		}

		if (key.isAcceptable()) {
			connRequest(key);
			return;
		}

		if (requestChannel != null
				&& key.equals(requestChannel.getSelectionKey())) {
			requestChannel.read();
		} else if (responseChannel != null
				&& key.equals(responseChannel.getSelectionKey())) {
			responseChannel.read();
			requestChannel.reset();
		}
	}

	private void connRequest(SelectionKey key) {
		try {
			if (key == null || !key.isAcceptable()) {
				Log.w(TAG, "invalid accept key");
				return;
			}

			ServerSocketChannel serverChannel = (ServerSocketChannel) key
					.channel();
			SocketChannel socketChannel = serverChannel.accept();
			Log.d(TAG, "connRequest " + socketChannel.socket().getInetAddress());

			requestChannel = new Channel(true);
			requestChannel.setListener(this);
			requestChannel.setSocket(socketChannel);
			socketChannel.configureBlocking(false);
			Selector selector = ProxyServer.getInstance().getSeletor();
			SelectionKey sk = socketChannel.register(selector,
					SelectionKey.OP_READ, this);
			requestChannel.setSelectionKey(sk);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean connResponse(Channel channel) {
		try {
			String host = null;
			int port;
			String method = channel.getMethod();

			if (responseChannel == null) {
				SocketChannel socketChannel = null;
				if ("CONNECT".equals(method)) {
					host = channel.getHost();
					port = channel.getPort();
					socketChannel = connect(host, port);
				} else {
					host = channel.getHost();
					port = channel.getPort();
					socketChannel = connect(host, port);
				}

				if (socketChannel == null) {
					return false;
				}

				responseChannel = new Channel(false);
				responseChannel.setListener(this);
				responseChannel.setSocket(socketChannel);

				Selector selector = ProxyServer.getInstance().getSeletor();
				SelectionKey sk = socketChannel.register(selector,
						SelectionKey.OP_READ, this);

				responseChannel.setSelectionKey(sk);
			} else {
				Log.d(TAG, "reuse socket " + responseChannel.getName());
				responseChannel.reset();
			}

			StringBuffer stringBuffer = new StringBuffer();
			if ("CONNECT".equals(method)) {
				stringBuffer.append(CONNECT_OK);
				responseChannel.setStatus(Status.CONTENT);
				byte[] sendBytes = stringBuffer.toString().getBytes();
				ByteBuffer byteBuffer = ByteBuffer.wrap(sendBytes);
				requestChannel.write(byteBuffer);
			} else {
				stringBuffer.append(method + " ");
				String url = channel.getUrl();

				if (!url.startsWith("/")) {
					// remove scheme and host
					url = url.substring(url.indexOf('/', 8));
				}
				Log.d(TAG, "connResponse " + url);
				stringBuffer.append(url).append(" ")
						.append(channel.getProtocol()).append(CRLF);
				Map<String, String> headers = channel.getHeaders();
				Iterator<String> iterator = headers.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					stringBuffer.append(key).append(": ")
							.append(headers.get(key)).append(CRLF);
				}
				stringBuffer.append(CRLF);
				String text = stringBuffer.toString();
				Log.d(TAG, text);
				byte[] sendBytes = text.getBytes();
				ByteBuffer byteBuffer = ByteBuffer.wrap(sendBytes);
				responseChannel.write(byteBuffer);
			}
		} catch (Exception e) {
			Log.e(TAG, "establish response exception", e);
			return false;
		}
		return true;
	}

	private SocketChannel connect(String host, int port) {
		Log.d(TAG, "connect " + host + ":" + port);
		SocketChannel channel = null;
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			SocketAddress address = new InetSocketAddress(
					InetAddress.getByName(host), port);
			if (channel.connect(address)) {
				Log.e(TAG, "connect channel failed");
				return null;
			}

			int waitTimes = 0;
			while (true) {
				if (waitTimes >= 200) {
					Log.w(TAG, "abort connection for timeout");
					return null;
				}
				Thread.sleep(50);
				if (channel.finishConnect()) {
					break;
				}
				++waitTimes;
			}
		} catch (Exception e) {
			return null;
		}
		return channel;
	}

	public void close() {
		Log.d(TAG, "close pair socket " + this);

		if (requestChannel != null) {
			requestChannel.close();
		}

		if (responseChannel != null) {
			responseChannel.close();
		}
	}

	@Override
	public void onStatusLine(Channel channel) {
		Log.d(TAG, "onStatusLine " + channel.getStatusLine());
	}

	@Override
	public void onHeaders(Channel channel) {
		Log.d(TAG, "onHeaders");
		if (channel.isRequest()) {
			if (!connResponse(channel)) {
				// close pair if can't connect to target
				close();
			}
		} else {
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(channel.getStatusLine()).append(CRLF);
			for (Entry<String, String> h : channel.getHeaders().entrySet()) {
				stringBuffer.append(h.getKey()).append(": ")
						.append(h.getValue()).append(CRLF);
			}
			stringBuffer.append(CRLF);
			byte[] sendBytes = stringBuffer.toString().getBytes();
			ByteBuffer buffer = ByteBuffer.wrap(sendBytes);
			requestChannel.write(buffer);
		}
	}

	@Override
	public void onContent(Channel channel) {
		if (channel.isRequest() && responseChannel != null) {
			ByteBuffer buffer = channel.getSocketBuffer();
			responseChannel.write(buffer);
		} else if (!channel.isRequest() && requestChannel != null) {
			ByteBuffer buffer = channel.getSocketBuffer();
			requestChannel.write(buffer);
		}
	}

	@Override
	public void onClose(Channel channel) {
		Log.d(TAG, "onClose " + channel);
		close();
	}
}
