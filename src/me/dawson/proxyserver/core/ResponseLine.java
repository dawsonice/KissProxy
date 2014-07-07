package me.dawson.proxyserver.core;

import android.text.TextUtils;

//example: HTTP/1.1 200 OK
/*
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
 * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 */

public class ResponseLine {
	public static final String TAG = "ResponseLine";

	private String statusLine;

	private String version;
	private int statusCode;
	private String reason;

	public ResponseLine(String line) {
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

			version = temp.substring(0, index);
			temp = temp.substring(index + 1);

			index = temp.indexOf(" ");
			if (index == -1) {
				return;
			}

			String scText = temp.substring(0, index);
			statusCode = Integer.parseInt(scText);

			reason = temp.substring(index + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String v) {
		this.version = v;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String r) {
		this.reason = r;
	}

}
