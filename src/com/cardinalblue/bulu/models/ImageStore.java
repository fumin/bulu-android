package com.cardinalblue.bulu.models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.cardinalblue.bulu.utils.BuluUtils;
import com.cardinalblue.bulu.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

public class ImageStore {
	
	public int mThumbnailWidth;
	
	private Context mContext;
	private ArrayList<String> mKeys;
	public File mOriginalDir;
	public File mThumbnailDir;
	
	public ImageStore(Context context) {
		mContext = context;
		mThumbnailWidth = 256;
		
		File albumDir = BuluUtils.getAlbumDir();
		mOriginalDir = new File(albumDir, "original");
		mOriginalDir.mkdirs();
		mThumbnailDir = new File(albumDir, "thumbnail");
		mThumbnailDir.mkdirs();
		
		// Prevent files under mThumbnail from being scanned by gallery using the empty file ".nomedia"
		// http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
		File noMediaFile = new File(mThumbnailDir, ".nomedia");
		try {
	        noMediaFile.createNewFile();
        } catch (IOException e) {
	        e.printStackTrace();
        }
		
		// initialize mKeys
		File[] files = mOriginalDir.listFiles();
		Arrays.sort(files);
		mKeys = new ArrayList<String>();
		for (File f : files) {
			mKeys.add(0, f.getName());
		}
	}
	
	public boolean saveImageAsThumbnailAndOriginal(Bitmap img) {
		long unixTime = System.currentTimeMillis() / 1000L;
		String key = String.valueOf(unixTime) + "_" + Utils.randomString(8) + ".jpg";
		Bitmap thumbnail = Bitmap.createScaledBitmap(img, mThumbnailWidth, mThumbnailWidth, false);
		
		File originalFile = new File(mOriginalDir, key);
		boolean originalOK = Utils.bitmapToJPEGFile(img, originalFile);
		File thumbnailFile = new File(mThumbnailDir, key);
		boolean thumbnailOK = Utils.bitmapToJPEGFile(thumbnail, thumbnailFile);
		
		if (originalOK && thumbnailOK) {
			mKeys.add(0, key);
			galleryAddPic(originalFile);
			return true;
		} else {
			originalFile.delete();
			thumbnailFile.delete();
			return false;
		}
	}
	
	public int size() {
		return mKeys.size();
	}
	
	public File getOriginalFileAt(int position) {
		return new File(mOriginalDir, mKeys.get(position));
	}
	public File getThumbnailFileAt(int position) {
		return new File(mThumbnailDir, mKeys.get(position));
	}
	
	public void deleteAt(int position) {
		String key = mKeys.get(position);
		File originalFile = new File(mOriginalDir, key);
		originalFile.delete();
		File thumbnailFile = new File(mThumbnailDir, key);
		thumbnailFile.delete();
		mKeys.remove(position);
	}
	
	private void galleryAddPic(File f) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    mContext.sendBroadcast(mediaScanIntent);
	}
}
