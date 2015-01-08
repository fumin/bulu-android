package com.cardinalblue.bulu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class DetailActivity extends FragmentActivity {
	public static final String EXTRA_IMAGE = "extra_image";
	
	private BuluApplication mBuluApplication;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail_pager);
		
		mBuluApplication = (BuluApplication)getApplication();
		
		DetailPagerAdapter detailPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(detailPagerAdapter);
		int primary_position = getIntent().getIntExtra(EXTRA_IMAGE, -1);
		viewPager.setCurrentItem(primary_position);
	}
	
	private class DetailPagerAdapter extends FragmentStatePagerAdapter {
		public DetailPagerAdapter(FragmentManager fragmentManager) {
	        super(fragmentManager);
        }

		@Override
        public Fragment getItem(int position) {
			String imageUrl = "file://" + mBuluApplication.mImageStore.getOriginalFileAt(position).getAbsolutePath();
			
			DetailWebViewFragment f = new DetailWebViewFragment();
			Bundle args = new Bundle();
	        args.putString(DetailWebViewFragment.IMAGE_DATA_EXTRA, imageUrl);
	        f.setArguments(args);
	        return f;
        }

		@Override
        public int getCount() {
	        return mBuluApplication.mImageStore.size();
        }
	}
	
	public static class DetailWebViewFragment extends SupportV4WebViewFragment {
		private static final String IMAGE_DATA_EXTRA = "extra_image_data";
		
		@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v = super.onCreateView(inflater, container, savedInstanceState);
			
			WebView webView = getWebView();
			String htmlStr = "<html><body style='background-color: black;'><img style='margin: auto; display: block;' src='" + getArguments().getString(IMAGE_DATA_EXTRA) + "'></body></html>";
			webView.loadDataWithBaseURL(null, htmlStr, "text/html", "utf-8", null);
			webView.getSettings().setLoadWithOverviewMode(true);
		    webView.getSettings().setUseWideViewPort(true);
			webView.getSettings().setSupportZoom(true);
			webView.getSettings().setBuiltInZoomControls(true);
			
			return v;
		}
	}

}
