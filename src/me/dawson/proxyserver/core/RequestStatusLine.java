package me.dawson.proxyserver.core;

// for parse proxy status line 
// http : GET http://ux.alipay-inc.com/ftp/h5/dawson/pure.html HTTP/1.1
// https: CONNECT ux.alipay-inc.com:443 HTTP/1.1 

public class RequestStatusLine {
	private String method;
	private String version;
	private String uri;
	private String statusLine;

	public RequestStatusLine(String text) {
		statusLine = text;
		if (text == null) {
			return;
		}

		String temp = text;

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
		if (!temp.isEmpty()) {
			version = temp;
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

	public void setVersion(String version) {
		this.version = version;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

}
