package com.cardinalblue.bulu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.cardinalblue.bulu.utils.BuluUtils;
import com.cardinalblue.bulu.utils.RetainFragment;
import com.cardinalblue.bulu.utils.Utils;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
	private static final String TAG = "bulu.MainActivity";
	
	static final int CAMERA_REQUEST = 0;
	static final int GALLERY_REQUEST = 1;
	
	private BuluApplication mBuluApplication;
	
	private GridView mGridView;
    
    private int mImageViewWidthPixels; // Adjusted upon orientation changes
    private LruCache<String, Bitmap> mMemoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mBuluApplication = (BuluApplication)getApplication();
        mBuluApplication.setMainActivity(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        // Memory cache
        RetainFragment mRetainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = mRetainFragment.mRetainedCache;
        if (mMemoryCache == null) {
        	int cacheSize = (int) ((Runtime.getRuntime().maxMemory() / 1024) / 8);
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            	@Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
            mRetainFragment.mRetainedCache = mMemoryCache;
        }
        
        // GridView and it's adapter
        ImageAdapter adapter = new ImageAdapter(mBuluApplication);
        mGridView = (GridView) findViewById(R.id.gridview);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        
        Log.i(TAG, String.format("gridview choicemode: %d", mGridView.getChoiceMode()) );
        
        // Set mImageViewWidthPixels
        DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screenWidth = metrics.widthPixels;
		int screenHeight = metrics.heightPixels;
		int numColumns;
		if (screenWidth > screenHeight) {
			numColumns = 5;
		} else {
			numColumns = 3;
		}
		mImageViewWidthPixels = screenWidth / numColumns - 60;
        mGridView.setColumnWidth(mImageViewWidthPixels);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    // ------------------------------------------------------------------------
    // Restore state
    static final String GRIDVIEW_MODE = "gridview_model";
    static final String SELECTED_ITEMS = "selected_items";
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putInt(GRIDVIEW_MODE, mGridView.getChoiceMode());
        savedInstanceState.putIntegerArrayList(SELECTED_ITEMS, Utils.sparseBooleanArrayToIntArray(mGridView.getCheckedItemPositions()));
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
       
        // Restore state members from saved instance
        int gridviewMode = savedInstanceState.getInt(GRIDVIEW_MODE);
        ArrayList<Integer> selectedItems = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS);
        if (GridView.CHOICE_MODE_MULTIPLE_MODAL == gridviewMode && selectedItems.size() > 0) {
        	enterMultipleSelectionMode();
        	for(int i : selectedItems) {
        		mGridView.setItemChecked(i, true);
        	}
        }
    }
    
    private static final int HANDLE_RECEIVE_BULU = 0;
    private static final int HANDLE_UPLOAD_S3_RESULT = 1;
    private static final int HANDLE_DOWNLOAD_S3_AND_SAVE = 2;
    private Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		Bundle b = (Bundle) msg.obj;
    		switch (msg.what) {
    		case HANDLE_RECEIVE_BULU:
    			handleReceiveBulu(b.getString("username"), b.getString("type"), b.getString("data"));
    			break;
    		case HANDLE_UPLOAD_S3_RESULT:
    			handleReceiveUploadS3Result(b.getBoolean("succeeded"), b.getString("url"));
    			break;
    		case HANDLE_DOWNLOAD_S3_AND_SAVE:
    			handleDownloadS3AndSave(b.getBoolean("succeeded"));
    			break;
    		default:
    			super.handleMessage(msg);
    		}
    	}
    };
    
    // Upload to S3 returned a result
    public void receiveUploadS3Result(BuluUtils.UploadToS3Result res) {
    	Bundle b = new Bundle();
    	b.putBoolean("succeeded", res.succeeded); b.putString("url", res.url);
    	Message msg = mHandler.obtainMessage(HANDLE_UPLOAD_S3_RESULT, b);
    	mHandler.sendMessage(msg);
    }
    private void handleReceiveUploadS3Result(boolean succeeded, String url) {
    	if (succeeded) {
			Log.i("ImageUpload", url);
		} else {
			Log.i("ImageUpload", "failed!");
			Utils.showDialog("Image sharing failed, please try again", getFragmentManager());
		}
    }
    
    // We downloaded from S3 and saved to disk
    public void receiveDownloadS3AndSave(boolean succeeded) {
    	Bundle b = new Bundle(); b.putBoolean("succeeded", succeeded);
    	Message msg = mHandler.obtainMessage(HANDLE_DOWNLOAD_S3_AND_SAVE, b);
    	mHandler.sendMessage(msg);
    }
    private void handleDownloadS3AndSave(boolean succeeded) {
    	Log.i(TAG, String.format("Download from S3 and save: %s", String.valueOf(succeeded)));
    	if (succeeded) {
    		//The below code displays the wrong grids (a bug of Android itself)
    		//  ImageAdapter adapter = (ImageAdapter) mGridView.getAdapter();
    		//  adapter.notifyDataSetChanged();
    		//Use the below hack to update our grid
    		ImageAdapter adapter = new ImageAdapter(mBuluApplication);
    		mGridView.invalidateViews();
    		mGridView.setAdapter(adapter);
    	}
    }
    
    // We received a bulu signal
    public void receiveBulu(String username, String type, String data) {
    	Bundle b = new Bundle();
    	b.putString("username", username); b.putString("type", type); b.putString("data", data);
    	Message msg = mHandler.obtainMessage(HANDLE_RECEIVE_BULU, b);
    	mHandler.sendMessage(msg);
    }
    private void handleReceiveBulu(String username, String type, String data) {
    	Log.i(TAG, String.format("Received bulu username: %s, type: %s, data: %s", username, type, data));
    	if ("image_url".equals(type)) {
    		mBuluApplication.downloadImgAndSave(data); 		
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_camera:
                startCameraIntentForResult();
                return true;
            case R.id.action_bar_from_gallery:
            	startPhotoGalleryForResult();
            	return true;
            case R.id.multiple_select_mode:
            	enterMultipleSelectionMode();
            	return true;
            case R.id.preference_settings:
            	Intent i = new Intent(this, SettingsActivity.class);
            	startActivity(i);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    // ---------------------------------------------------------------------------
    // Bulu from camera and photo gallery
    private String mCurrentPhotoPath;
    private void startCameraIntentForResult() {
    	Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	try {
	        File f = BuluUtils.createImageFile();
	        mCurrentPhotoPath = f.getAbsolutePath();
	        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
	        startActivityForResult(takePictureIntent, CAMERA_REQUEST);
        } catch (IOException e) {
	        e.printStackTrace();
        }
    }
    private void startPhotoGalleryForResult() {
    	Intent photoFromGalleryIntent = new Intent();
    	photoFromGalleryIntent.setType("image/*");
    	photoFromGalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
    	startActivityForResult(Intent.createChooser(photoFromGalleryIntent, "Select Picture"), GALLERY_REQUEST);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case CAMERA_REQUEST:
    		if (resultCode == RESULT_OK) {
    			uploadToS3FromImagePath(mCurrentPhotoPath);
    			File f = new File(mCurrentPhotoPath);
    			f.delete();
    		}
    		break;
    	case GALLERY_REQUEST:
    		if (resultCode == RESULT_OK) {
    			Uri uri = data.getData();
    			String imageFilePath = null;
    			
    			// Apparently, there're various ways of transforming a Uri to a file path in Android, sucks...
    			Cursor cursor = getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
                if (cursor != null) {
                	cursor.moveToFirst();
                    imageFilePath = cursor.getString(0);
                    cursor.close();
                } else {
                	imageFilePath = uri.getPath();
                }
                uploadToS3FromImagePath(imageFilePath);
    		}
    		break;
    	}
    }
    private void uploadToS3FromImagePath(String path) {
    	if (path != null) {
    		Bitmap correctOrientationBitmap = Utils.correctOrientationBitmapFromFile(new File(path));
        	if (correctOrientationBitmap != null) {
        		int maxWH = mBuluApplication.preferenceImageQuality();
            	Bitmap imageBitmap = Utils.bitmapWithMaxWidthHeight(correctOrientationBitmap, maxWH);
        		mBuluApplication.uploadToS3(imageBitmap);
        	} else {
        		Utils.showDialog("Sorry, there was an error getting the image with correct orientation, please try again.", getFragmentManager());
        	}
    	} else {
    		Utils.showDialog("Sorry, there was an error getting the image with correct orientation, please try again.", getFragmentManager());
    	}
    }
    
    
    private class ImageAdapter extends BaseAdapter {
    	private Context mContext;
    	
    	// Constructor
    	public ImageAdapter(Context c) {
    		mContext = c;
    	}

    	@Override
    	public int getCount() {
    		return mBuluApplication.mImageStore.size();
    	}

    	@Override
    	public Object getItem(int position) {
    		return mBuluApplication.mImageStore.getThumbnailFileAt(position).getAbsolutePath();
    	}

    	@Override
    	public long getItemId(int position) {
    		return position;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		ImageView imageView = null;
    		if (convertView == null) {
    			imageView = new ImageView(mContext);
    			imageView.setLayoutParams(new GridView.LayoutParams(mImageViewWidthPixels, mImageViewWidthPixels));
    			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);			
    		} else {
    			imageView = (ImageView) convertView;
    		}
    		
    		// For drawing multiple selections
			String path = mBuluApplication.mImageStore.getThumbnailFileAt(position).getAbsolutePath();
			Bitmap bitmap = selectedModeThumbnailFromCache(path);
			BitmapDrawable selectedDrawable = new BitmapDrawable(getResources(), bitmap);
			
			StateListDrawable states = new StateListDrawable();
			states.addState(new int[] {android.R.attr.state_activated}, selectedDrawable);
			states.addState(new int[] {}, Drawable.createFromPath(path));
			
			imageView.setImageDrawable(states);
    		
    		return imageView;
    	}
    	
    	private Bitmap selectedModeThumbnailFromCache(String path) {
    		Bitmap cacheBitmap = mMemoryCache.get(path);
    		if (cacheBitmap != null) {
    			return cacheBitmap;
    		}
    		
    		// Do the work to generate the selectedModeThumbnail
    		Bitmap originalBitmap = BitmapFactory.decodeFile(path);
			Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
			Canvas c = new Canvas(bitmap);
			Paint p = new Paint();
			p.setColorFilter(new LightingColorFilter(Color.CYAN, 1));
			c.drawBitmap(bitmap, 0, 0, p);
			
			mMemoryCache.put(path, bitmap);
			return bitmap;
    	}
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    	if (mGridView.getChoiceMode() == GridView.CHOICE_MODE_NONE) {
    		Intent i = new Intent(this, DetailActivity.class);
        	i.putExtra(DetailActivity.EXTRA_IMAGE, position);
        	
        	// Start activity
        	if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN ) {
        		ActivityOptions options = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
                startActivity(i, options.toBundle());
        	} else {
        		startActivity(i);
        	}
    	} else {
    		mGridView.setItemChecked(position, true);
    	}
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
	    if (mGridView.getChoiceMode() == GridView.CHOICE_MODE_NONE) {
	    	enterMultipleSelectionMode();
	    	mGridView.setItemChecked(position, true);
	    	return true;
	    } else {
	    	return false;
	    }
    }
    
    private void enterMultipleSelectionMode() {
    	mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
    	mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			@Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch(item.getItemId()) {
                case R.id.multi_select_menu_delete:
                	ArrayList<Integer> selectedItems = Utils.sparseBooleanArrayToIntArray(mGridView.getCheckedItemPositions());
                	for(int i : selectedItems) {
                		Log.i(TAG, String.format("deleting item: %d", i));
                		mBuluApplication.mImageStore.deleteAt(i);
                	}
                	mode.finish();
                	return true;
                default: return false;
                }
            }
			@Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multiselect_mode, menu);
                return true;
            }
			@Override
            public void onDestroyActionMode(ActionMode mode) {
				// Here you can make any necessary updates to the activity when
		        // the CAB is removed. By default, selected items are deselected/unchecked.
				ArrayList<Integer> selectedItems = Utils.sparseBooleanArrayToIntArray(mGridView.getCheckedItemPositions());
            	for(int i : selectedItems) {
            		mGridView.setItemChecked(i, false);
            	}
        		mGridView.invalidateViews();
        		
				mGridView.post(new Runnable() {
					@Override
                    public void run() {
                        mGridView.setChoiceMode(GridView.CHOICE_MODE_NONE);
                    }
				});
			}
			@Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// Here you can perform updates to the CAB due to
		        // an invalidate() request
		        return false;
            }
			@Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				// Here you can do something when items are selected/de-selected,
		        // such as update the title in the CAB
            }
    		
    	});
    }
    
    // Handle AllJoyn errors
 	public void handleRegisterBuluServiceError() {
 		Log.i(TAG, "handleRegisterBuluServiceError");
 	}
 	public void handleRegisterBuluHandlerError() {
 		Log.i(TAG, "handleRegisterBuluHandlerError");
 	}
 	public void handleBusConnectError() {
 		Log.i(TAG, "handleBusConnectError");
 	}
 	public void handleBusMatchError() {
 		Log.i(TAG, "handleBusMatchError");
 	}
    public void handleBuluInterfaceNullError() {
    	Log.i(TAG, "handleBuluInterfaceNullError");
    }
    public void handleEmitSignalBusyError() {
    	Log.i(TAG, "handleEmitSignalBusyError");
    }
}
