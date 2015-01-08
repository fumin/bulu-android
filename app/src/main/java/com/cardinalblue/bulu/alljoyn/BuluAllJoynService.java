package com.cardinalblue.bulu.alljoyn;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import com.cardinalblue.bulu.BuluApplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BuluAllJoynService extends Service {
	private static final String TAG = "bulu.BuluAllJoynService";
	
	@Override
    public IBinder onBind(Intent arg0) {
	    return null;
    }
	
	private BusAttachment mBus  = new BusAttachment(BuluApplication.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);
	private static final String OBJECT_PATH = "/buluService";
	private BuluService mBuluService;
	private BuluInterface mBuluInterface;
	private boolean mBusConnected;
	private BuluApplication mBuluApplication;
	
	public void onCreate() {
		mBuluApplication = (BuluApplication) getApplication();
		mBuluApplication.setBuluAllJoynService(this);
		
		
		mBusConnected = false;
		busConnect();
	}
	public void onDestroy() {
		mBus.disconnect();
		mBusConnected = false;
	}
	
	public synchronized void busConnect() {
		if (!mBusConnected) {
			mBuluService = new BuluService();
			Status status = mBus.registerBusObject(mBuluService, OBJECT_PATH);
			if (Status.OK != status) {
				mBuluApplication.handleRegisterBuluServiceError();
				return;
			}
			
			status = mBus.connect();
			if (Status.OK != status) {
				mBuluApplication.handleBusConnectError();
				return;
			}
			
			status = mBus.registerSignalHandlers(mBuluService);
			if (Status.OK != status) {
				mBuluApplication.handleRegisterBuluHandlerError();
				return;
			}
			status = mBus.addMatch("sessionless='t'");
			if (Status.OK != status) {
				mBuluApplication.handleBusMatchError();
				return;
			}
			
			SignalEmitter emitter = new SignalEmitter(mBuluService, 0, SignalEmitter.GlobalBroadcast.Off);
			emitter.setSessionlessFlag(true);
			mBuluInterface = emitter.getInterface(BuluInterface.class);
			if (mBuluInterface == null) {
				mBuluApplication.handleBuluInterfaceNullError();
				return;
			}
			
			mBusConnected = true;
		}
	}
	
	class BuluService implements BuluInterface, BusObject {
		public void bulu(String username, String type, String data) {}
		@BusSignalHandler(iface = BuluInterface.INTERFACE_NAME, signal = "bulu")
		public void buluHandle(String username, String type, String data) {
			mBuluApplication.receiveBulu(username, type, data);
		}
	}
	public final void sendBulu(String username, String type, String data) {
		Log.i(TAG, String.format("We're broadcasting username: %s, type: %s, data: %s", username, type, data));
		try {
	        mBuluInterface.bulu(username, type, data);
        } catch (BusException e) {
	        e.printStackTrace();
	        mBuluApplication.handleEmitSignalBusyError();
        }
	}
	
	/*
     * Load the native alljoyn_java library.  The actual AllJoyn code is
     * written in C++ and the alljoyn_java library provides the language
     * bindings from Java to C++ and vice versa.
     */
    static {
        Log.i(TAG, "System.loadLibrary(\"alljoyn_java\")");
        System.loadLibrary("alljoyn_java");
    }
	
}
