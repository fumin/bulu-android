package com.cardinalblue.bulu.alljoyn;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface (name = BuluInterface.INTERFACE_NAME)
public interface BuluInterface {
	static final String INTERFACE_NAME = "com.cardinalblue.bulu";
	
	@BusSignal
	public void bulu(String username, String type, String data) throws BusException;

}
