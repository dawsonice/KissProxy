package me.dawson.proxyserver.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

public class Channel {
	public static final String TAG = "Channel";

	private static final int BUFFER_SIZE = 8192;

	private static Pattern HTTPS_PATTERN = Pattern.compile("(.*):([\\d]+)");
	private static Pattern HTTP_PATTERN = Pattern
			.compile("(https?)://([^:/]+)(:[\\d]+])?/.*");
	private static int INDEX = 0;

	public enum Status {
		STATUS_LINE, HEADERS, CONTENT
	}

	private SocketChannel socket;
	private SelectionKey selectionKey;
	private long lastActive;
	private ByteBuffer socketBuffer;
	private Status status;
	private char[] readBuf;
	private int readOffset;
	private int contentLen;
	private String statusLine;
	private Map<String, String> headers;
	private boolean request;
	private String method;
	private int port;
	private String host;
	private ChannelListener listener;
	private int statusCode;
	private String url;
	private RequestLine sl;
	private String channelName;

	public Channel(boolean req) {
		readBuf = new char[1024];
		headers = new HashMap<String, String>();
		request = req;
		channelName = "channel" + (INDEX++);
		readOffset = 0;
		reset();
	}

	public void reset() {
		lastActive = System.currentTimeMillis();
		if ("CONNECT".equals(method)) {
			status = Status.CONTENT;
		} else {
			status = Status.STATUS_LINE;
		}
		headers.clear();
	}

	public void setSocket(SocketChannel socket) {
		this.socket = socket;
	}

	public SocketChannel getSocket() {
		return socket;
	}

	public void setSelectionKey(SelectionKey selectionKey) {
		this.selectionKey = selectionKey;
	}

	public SelectionKey getSelectionKey() {
		return selectionKey;
	}

	public long getLastActive() {
		return lastActive;
	}

	public void setLastActive(long active) {
		this.lastActive = active;
	}

	public boolean getRequest() {
		return request;
	}

	public Status getStep() {
		return status;
	}

	public void setStatus(Status step) {
		this.status = step;
	}

	public void read() {
		int count = 0;
		getSocketBuffer();
		socketBuffer.clear();
		try {
			count = socket.read(socketBuffer);
		} catch (IOException e) {
			Log.e(TAG, channelName + " read exception.", e);
		}

		socketBuffer.flip();
		int datasize = socketBuffer.limit() - socketBuffer.position();
		String r = request ? "request" : "response";
		Log.d(TAG, r + " " + channelName + " read count " + count
				+ " datasize " + datasize);
		if (count == -1) {
			if (listener != null) {
				listener.onClose(this);
			}
			return;
		}

		if (datasize == 0) {
			return;
		}

		if (status == Status.CONTENT) {
			if (listener != null) {
				listener.onContent(this);
			}
			return;
		}

		String lineText = readLine();
		while (lineText != null) {
			if (status == Status.STATUS_LINE) {
				setStatusLine(lineText);
				status = Status.HEADERS;

				if (listener != null) {
					listener.onStatusLine(this);
				}
			} else if (status == Status.HEADERS) {
				if (TextUtils.isEmpty(lineText)) {
					status = Status.CONTENT;
					if (listener != null) {
						listener.onHeaders(this);
					}
					break;
				} else {
					addHeader(lineText);
				}
			}
			lineText = readLine();
		}

		if (status == Status.CONTENT) {
			if (listener != null) {
				listener.onContent(this);
			}
		}
	}

	private void setStatusLine(String line) {
		Log.d(TAG, channelName + " setStatusLine " + line);
		statusLine = line;
		if (request) {
			sl = new RequestLine(line);
			method = sl.getMethod();
		} else {
			ResponseLine sl = new ResponseLine(line);
			statusCode = sl.getStatusCode();
		}
	}

	public String getStatusLine() {
		return statusLine;
	}

	private void addHeader(String line) {
		if (TextUtils.isEmpty(line)) {
			return;
		}

		int index = line.indexOf(":");
		if (index <= 0 || index >= line.length()) {
			Log.w(TAG, channelName + " invalid header content " + line);
			return;
		}

		String name = line.substring(0, index);
		String value = line.substring(index + 1).trim();
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) {
			Log.w(TAG, channelName + " +invalie header value");
			return;
		}

		Log.d(TAG, channelName + " addHeader [name] " + name + " [value] "
				+ value);
		headers.put(name, value);
	}

	// header line end with \r\n
	// header and body separated with \r\n

	private String readLine() {
		int ch;
		if (socketBuffer.remaining() <= 0) {
			return null;
		}

		while (socketBuffer.remaining() > 0) {
			ch = socketBuffer.get();

			if (ch == -1 || ch == '\n') {
				break;
			}

			if (ch != '\r') {
				if (readOffset == readBuf.length) {
					char tempBuffer[] = readBuf;
					readBuf = new char[tempBuffer.length * 2];
					System.arraycopy(tempBuffer, 0, readBuf, 0, readOffset);
				}
				readBuf[readOffset++] = (char) ch;
			}
		}

		String line = String.copyValueOf(readBuf, 0, readOffset);
		readOffset = 0;
		return line;
	}

	public int write(ByteBuffer b) {
		int count = 0;
		try {
			count = socket.write(b);
		} catch (IOException e) {
			Log.e(TAG, channelName + "  write exception.", e);
		}
		return count;
	}

	public int getContentLen() {
		return contentLen;
	}

	public void setContentLen(int contentLen) {
		this.contentLen = contentLen;
	}

	static int index = 0;

	public void close() {
		try {
			Log.d(TAG, channelName + " close socket " + (index++));
			selectionKey.cancel();
			socket.close();
		} catch (Exception e) {
			Log.e(TAG, channelName + " close exception", e);
		}
	}

	public String getMethod() {
		return method;
	}

	public ChannelListener getListener() {
		return listener;
	}

	public void setListener(ChannelListener listener) {
		this.listener = listener;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getHeader(String key) {
		if (headers == null || TextUtils.isEmpty(key)) {
			return null;
		}

		return headers.get(key);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public boolean isRequest() {
		return request;
	}

	public String getHost() {
		if (!request) {
			return null;
		}

		String u = getUrl();
		if ("CONNECT".equals(method)) {
			Matcher m = HTTPS_PATTERN.matcher(u);
			if (m.matches()) {
				host = m.group(1);
				port = Integer.parseInt(m.group(2));
			}
		} else {
			Matcher m = HTTP_PATTERN.matcher(u);
			if (m.matches()) {
				host = m.group(2);
				if (m.group(3) != null) {
					Integer.parseInt(m.group(3).substring(1));
				} else {
					if ("https".equals(m.group(1))) {
						port = 443;
					} else {
						port = 80;
					}
				}
			}
		}
		return host;
	}

	public int getPort() {
		if (port == 0) {
			getHost();
		}

		return port;
	}

	public void setPort(int p) {
		this.port = p;
	}

	public String getUrl() {
		String uri = sl.getUri();
		if (uri != null && !uri.startsWith("/")) {
			url = uri;
		} else {
			String h = getHeaders().get("Host");
			url = h + uri;
		}
		return url;
	}

	public ByteBuffer getSocketBuffer() {
		if (socketBuffer == null) {
			socketBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		}
		return socketBuffer;
	}

	public String getName() {
		return channelName;
	}

	public String getProtocol() {
		if (sl == null) {
			return null;
		}
		return sl.getVersion();
	}
}