package me.dawson.proxyserver.ui;

interface IProxyControl {
	boolean start();
	
	boolean stop();
	
	boolean isRunning();
	
	int getPort();
}