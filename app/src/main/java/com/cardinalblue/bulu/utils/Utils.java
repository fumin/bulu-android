package com.cardinalblue.bulu.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;

public class Utils {
	public static String currentTimeInHTTPFormat() {
		Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}
	
	// --------------------------------------------------------------------
	// Bitmap related
	//
	public static byte[] bitmapToJPEGBytes(Bitmap bitmap) {
		ByteArrayOutputStream blob = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 100, blob);
		byte[] bitmapdata = blob.toByteArray();
		return bitmapdata;
	}
	public static boolean bitmapToJPEGFile(Bitmap img, File f) {
		BufferedOutputStream out = null;
		try {
	        out = new BufferedOutputStream(new FileOutputStream(f));
	        img.compress(CompressFormat.JPEG, 100, out);
	        return true;
        } catch (FileNotFoundException e) {
	        e.printStackTrace();
	        return false;
        } finally {
        	if (out != null) {
        		try {
	                out.close();
                } catch (IOException e) {
	                e.printStackTrace();
                }
        	}
        }
	}
	public static Bitmap bitmapWithMaxWidthHeight(Bitmap img, double maxWH) {
		double ratio = (maxWH / (img.getWidth() > img.getHeight() ? img.getWidth() : img.getHeight()));
		ratio = (ratio > 1.0 ? 1.0 : ratio);
		int dstWidth = (int) (img.getWidth()*ratio); int dstHeight = (int) (img.getHeight()*ratio);
		Bitmap resizedBitmap = Bitmap.createScaledBitmap(img, dstWidth, dstHeight, false);
		return resizedBitmap;
	}
	
	// --------------------------------------------------------------------
	// Cryptography related
	//
	public static byte[] HMAC_SHA1(byte[] data, byte[] access_secret) {
		Mac mac;
        try {
	        mac = Mac.getInstance("HmacSHA1");
	        SecretKeySpec secret = new SecretKeySpec(access_secret,"HmacSHA1");
	        mac.init(secret);
	        byte[] digest = mac.doFinal(data);
	        return digest;
        } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	        return null;
        } catch (InvalidKeyException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return null;
        }
	}
	public static String MD5_Hex(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
	
	public static byte[] readIt(InputStream is) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		
		try {
	        while ((nRead = is.read(data)) != -1) {
	        	buffer.write(data, 0, nRead);
	        }
	        byte[] res = buffer.toByteArray();
	        buffer.close();
	        return res;
        } catch (IOException e) {
	        e.printStackTrace();
	        return null;
        }
	}
	
	public static String randomString(int randomLength) {
	    Random generator = new Random();
	    StringBuilder randomStringBuilder = new StringBuilder();
	    char tempChar;
	    for (int i = 0; i < randomLength; i++){
	    	int charInt = generator.nextInt(62);
            if (charInt < 26) {
                    tempChar = (char) (charInt + 65);
            } else if (charInt < 52) {
                    tempChar = (char) (charInt - 26 + 97);
            } else {
                    tempChar = (char) (charInt - 52 + 48);
            }
	        randomStringBuilder.append(tempChar);
	    }
	    return randomStringBuilder.toString();
	}
	
	// --------------------------------------------------------------------
	// Camera and photo library related
	//
	public static File getAlbumDir(String albumName) {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v("Utils", "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}
	public static File createImageFile(String albumName) throws IOException {
    	File storageDir = getAlbumDir(albumName);  
    	
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }
	
	
	// --------------------------------------------------------------------
	// Create dialog
	//
	public static void showDialog(String title, FragmentManager manager) {
		OKDialogFragment f = new OKDialogFragment();
		
		Bundle args = new Bundle();
		args.putString(OKDialogFragment.TITLE, title);
		f.setArguments(args);
		
		// Android bug, http://stackoverflow.com/questions/14262312/java-lang-illegalstateexception-can-not-perform-this-action-after-onsaveinstanc
		//f.show(manager, "OKDialog");
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.add(f, title);
		transaction.commitAllowingStateLoss();
	}
	public static class OKDialogFragment extends DialogFragment {
		public static final String TITLE = "title";
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(getArguments().getString(TITLE))
	               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // FIRE ZE MISSILES!
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	
	public static ArrayList<Integer> sparseBooleanArrayToIntArray(SparseBooleanArray sba) {
		if (sba == null) {
			return new ArrayList<Integer>();
		}
		
		ArrayList<Integer> res = new ArrayList<Integer>();
		int size = sba.size();
		
		for(int i = 0; i != size; ++i) {
			if (sba.valueAt(i)) {
				res.add(sba.keyAt(i));
			}
		}
		
		return res;
	}
	
	public static Bitmap correctOrientationBitmapFromFile(Context ctx, Uri f) {
		ExifInterface exif;
		FileInputStream is = null;
        try {
//	        exif = new ExifInterface(f.getPath());
//	        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//	        int angle = 0;
//
//	        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
//	            angle = 90;
//	        }
//	        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
//	            angle = 180;
//	        }
//	        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
//	            angle = 270;
//	        }
//
//	        Matrix mat = new Matrix();
//	        mat.postRotate(angle);
//
//	        is = new FileInputStream(f);
//	        Bitmap bmp = BitmapFactory.decodeStream(is, null, null);
//	        Bitmap correctBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
//	        return correctBmp;
            return BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(f));
        } catch (IOException e) {
	        e.printStackTrace();
	        return null;
        } finally {
        	if (is != null) {
        		try {
	                is.close();
                } catch (IOException e) {
	                e.printStackTrace();
                }
        	}
        }
	}

}
