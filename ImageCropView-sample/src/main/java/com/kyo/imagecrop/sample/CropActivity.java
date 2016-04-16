package com.kyo.imagecrop.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.kyo.imagecrop.CropLayout;
import com.kyo.imagecrop.CropUtils;


public class CropActivity extends Activity {

	private CropLayout mCropLayout;
	private Button mDoneButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_crop);

		Intent intent = getIntent();
		if (intent == null) {
			return;
		}

		Uri sourceUri = intent.getData();
		int outputX = intent
			.getIntExtra("outputX", CropUtils.dip2px(this, 200));
		int outputY = intent
			.getIntExtra("outputY", CropUtils.dip2px(this, 200));
		String outputFormat = intent.getStringExtra("outputFormat");

		mDoneButton = (Button) this.findViewById(R.id.done);
		mDoneButton.setOnClickListener(mOnClickListener);

		// bellow
		mCropLayout = (CropLayout) this.findViewById(R.id.crop);
		mCropLayout.setOnCropListener(mOnCropListener);
		mCropLayout.startCropImage(sourceUri, outputX, outputY);
		mCropLayout.setOutputFormat(outputFormat);
	}

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.done: {
					mCropLayout.requestCropResult();
					break;
				}
				default:
					break;
			}
		}
	};

	private CropLayout.OnCropListener mOnCropListener = new CropLayout.OnCropListener() {

		@Override
		public void onCropResult(Uri data) {
			Intent intent = new Intent(CropActivity.this, ResultActivity.class);
			intent.setData(data);
			startActivity(intent);
		}

		@Override
		public void onCropFailed(String errmsg) {

		}

		@Override
		public void onLoadingStateChanged(boolean isLoading) {
			if (mDoneButton != null) {
				mDoneButton.setEnabled(!isLoading);
			}
		}
	};
}
