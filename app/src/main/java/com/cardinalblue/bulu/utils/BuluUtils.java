package com.cardinalblue.bulu.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.util.EncodingUtils;

import com.cardinalblue.bulu.BuluApplication;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

public class BuluUtils {
	private static final String TAG = "bulu.BuluUtils";
	
	public static class UploadToS3Result {
		public boolean succeeded;
		public String url;
	}
	private static UploadToS3Result FailedS3Upload() {
		UploadToS3Result r = new UploadToS3Result();
		r.succeeded = false;
		return r;
	}
	private static UploadToS3Result SucceededS3Upload(String url) {
		UploadToS3Result r = new UploadToS3Result();
		r.succeeded = true;
		r.url = url;
		return r;
	}
	public static UploadToS3Result upload_to_s3(Bitmap img, String filename) {
		Log.i(TAG, String.format("%d, %d", img.getWidth(), img.getHeight()));
		byte[] jpegImg = Utils.bitmapToJPEGBytes(img);
		
	    
		String bucketName = BuluApplication.s3Bucket;
		String access_key = BuluApplication.s3AccessKeyId;
		String secret_access = BuluApplication.s3SecretAccessKey;
		
		// Prepare authorization signature
		String dateStr = Utils.currentTimeInHTTPFormat();
		String strToSign = String.format("PUT\n\nimage/jpeg\n%s\nx-amz-acl:public-read\n/%s/expires_in_days/7/bulu/%s.jpg", dateStr, bucketName, filename);
		String signature = Base64.encodeToString(Utils.HMAC_SHA1(EncodingUtils.getAsciiBytes(strToSign), EncodingUtils.getAsciiBytes(secret_access)), Base64.NO_WRAP);
		
		InputStream is;
		try {
			String urlStr = String.format("http://s3.amazonaws.com/%s/expires_in_days/7/bulu/%s.jpg", bucketName, filename);
	        URL url = new URL(urlStr);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setDoInput(true);
	        conn.setRequestMethod("PUT");
	        conn.setRequestProperty("Content-Length", String.format("%d", jpegImg.length));
	        conn.setRequestProperty("Content-Type", "image/jpeg");
	        conn.setRequestProperty("Date", dateStr);
	        conn.setRequestProperty("Authorization", String.format("AWS %s:%s", access_key, signature));
	        conn.setRequestProperty("x-amz-acl", "public-read");
	        
	        // prepare request body
	        conn.setDoOutput(true);
	        OutputStream os = conn.getOutputStream();
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        baos.write(jpegImg);
	        baos.writeTo(os);
	        baos.close();
	        os.close();
	        
	        //conn.connect();
	        
	        int respCode = conn.getResponseCode();
	        is = conn.getInputStream();
	        String respStr = EncodingUtils.getString(Utils.readIt(is), "UTF-8");
	        is.close();
	        conn.disconnect();
	        Log.i(TAG, String.format("%d %s", respCode, respStr));
	        
	        String cdnUrlStr = String.format("http://doodlescan-test.s3.amazonaws.com/expires_in_days/7/bulu/%s.jpg", filename);
	        return SucceededS3Upload(cdnUrlStr);
        } catch (MalformedURLException e) {
	        e.printStackTrace();
	        return FailedS3Upload();
        } catch (IOException e) {
	        e.printStackTrace();
	        return FailedS3Upload();
        }
	}
	
	public static final String ALBUM_NAME = "bulu_album";
	public static File getAlbumDir() {
		return Utils.getAlbumDir(ALBUM_NAME);
	}
	public static File createImageFile() throws IOException {
		return Utils.createImageFile(ALBUM_NAME);
	}

}
