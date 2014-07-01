package me.dawson.proxyserver.core;

public interface ChannelListener {

	// invoked on get status line
	public void onStatusLine(Channel channel);

	// invoked on get all headers
	public void onHeaders(Channel channel);

	// invoked on get any content
	public void onContent(Channel channel);

	// invoked on any side channel close
	public void onClose(Channel channel);
}
