package me.dawson.proxyserver.core;

import android.text.TextUtils;

// for parse proxy status line 
// http : GET http://ux.alipay-inc.com/ftp/h5/dawson/pure.html HTTP/1.1
// https: CONNECT ux.alipay-inc.com:443 HTTP/1.1 

/*
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
 * Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
 * 
 */

public class RequestLine {
	public static final String TAG = "RequestLine";

	// original status line
	private String statusLine;

	// parse result
	private String method;
	private String uri;
	private String version;

	public RequestLine(String line) {
		statusLine = line;
		if (TextUtils.isEmpty(line)) {
			return;
		}

		String temp = line;

		try {
			int index = temp.indexOf(" ");
			if (index == -1) {
				return;
			}

			method = temp.substring(0, index);
			temp = temp.substring(index + 1);

			index = temp.indexOf(" ");
			if (index == -1) {
				return;
			}

			uri = temp.substring(0, index);
			temp = temp.substring(index + 1);
			if (!TextUtils.isEmpty(temp)) {
				version = temp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String v) {
		this.version = v;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String u) {
		this.uri = u;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

}
