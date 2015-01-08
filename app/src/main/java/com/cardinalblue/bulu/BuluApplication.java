package com.cardinalblue.bulu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.cardinalblue.bulu.alljoyn.BuluAllJoynService;
import com.cardinalblue.bulu.models.ImageStore;
import com.cardinalblue.bulu.utils.BuluUtils;
import com.cardinalblue.bulu.utils.Utils;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

public class BuluApplication extends Application {
	//private static final String TAG = "chat.ChatApplication";
	
	public static String PACKAGE_NAME;
	public ImageStore mImageStore;
	
	public static String s3Bucket = "TODO";
	public static String s3AccessKeyId = "TODO";
	public static String s3SecretAccessKey = "TODO";
	
	// Settings preference
	private static String USERNAME_KEY = "username";
	private static String IMAGE_QUALITY_KEY = "image_quality";
	
	public void onCreate() {
		PACKAGE_NAME = getApplicationContext().getPackageName();
		
		mImageStore = new ImageStore(this);

		Intent intent = new Intent(this, BuluAllJoynService.class);
		startService(intent);
	}
	
	// Receive and send bulu signals
	private BuluAllJoynService mBuluAllJoynService;
	public void setBuluAllJoynService(BuluAllJoynService s) {
		mBuluAllJoynService = s;
	}
	public void sendBulu(String username, String type, String data) {
		mBuluAllJoynService.sendBulu(username, type, data);
	}
	private MainActivity mMainActivity;
	public void setMainActivity(MainActivity a) {
		mMainActivity = a;
	}
	public void receiveBulu(String username, String type, String data) {
		if (mMainActivity != null) {
			mMainActivity.receiveBulu(username, type, data);
		}
	}
	
	public void uploadToS3(final Bitmap img) {
		new Thread(new Runnable() {
			@Override
            public void run() {
				String filename = Utils.randomString(8);
				BuluUtils.UploadToS3Result uploadRes = BuluUtils.upload_to_s3(img, filename);
				if (uploadRes.succeeded) {
					sendBulu("awaw", "image_url", uploadRes.url);
				}
				mMainActivity.receiveUploadS3Result(uploadRes);
            }
		}).start();
	}
	
	public void downloadImgAndSave(final String urlStr) {
		new Thread(new Runnable() {
			@Override
            public void run() {
				InputStream is = null;
				boolean succeeded = false;
				try {
		            URL url = new URL(urlStr);
		            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					is = conn.getInputStream();
					Bitmap img = BitmapFactory.decodeStream(is);
					if (img != null) {
						succeeded = mImageStore.saveImageAsThumbnailAndOriginal(img);
					}
		        } catch (MalformedURLException e) {
		            e.printStackTrace();
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		        	if (is != null) {
		        		try {
			                is.close();
		                } catch (IOException e) {
			                e.printStackTrace();
		                }
		        	}
		        	mMainActivity.receiveDownloadS3AndSave(succeeded);
		        }
            }
		}).start();
	}
	
	// Handle AllJoyn errors
	public void handleRegisterBuluServiceError() {
		mMainActivity.handleRegisterBuluServiceError();
	}
	public void handleRegisterBuluHandlerError() {
		mMainActivity.handleRegisterBuluHandlerError();
	}
	public void handleBusConnectError() {
		mMainActivity.handleBusConnectError();
	}
	public void handleBusMatchError() {
		mMainActivity.handleBusMatchError();
	}
	public void handleBuluInterfaceNullError() {
		mMainActivity.handleBuluInterfaceNullError();
	}
	public void handleEmitSignalBusyError() {
		mMainActivity.handleEmitSignalBusyError();
	}
	
	// -----------------------------------------------------------------
	// Android Settings and Preferences
	public int preferenceImageQuality() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String iq_str = sharedPref.getString(IMAGE_QUALITY_KEY, "4096");
		int i = Integer.valueOf(iq_str);
		return Math.min(Math.max(i, 256), 4096);
	}
	public String preferenceUsername() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPref.getString(USERNAME_KEY, "");
	}
}
