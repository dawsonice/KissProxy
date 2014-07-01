package me.dawson.proxyserver.core;

import android.text.TextUtils;
import android.util.Log;

//HTTP/1.1 200 OK

public class ResponseStatusLine {
	public static final String TAG = "ResponseStatusLine";
	private int statusCode;
	private String version;
	private String phrase;
	private String statusLine;

	public ResponseStatusLine(String text) {
		statusLine = text;
		if (text == null) {
			return;
		}

		String temp = text;

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

		try {
			setStatusCode(Integer.parseInt(temp.substring(0, index)));
		} catch (Exception e) {
			Log.e(TAG, "parse status code exception.", e);
		}
		temp = temp.substring(index + 1);
		if (!TextUtils.isEmpty(temp)) {
			setPhrase(temp);
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode
	 *            the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the phrase
	 */
	public String getPhrase() {
		return phrase;
	}

	/**
	 * @param phrase
	 *            the phrase to set
	 */
	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}

}
