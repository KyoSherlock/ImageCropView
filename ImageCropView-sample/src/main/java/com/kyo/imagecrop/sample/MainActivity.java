/**
 * Copyright 2015, KyoSherlock
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kyo.imagecrop.sample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.kyo.imagecrop.CropFileUtils;
import com.kyo.imagecrop.CropUtils;


public class MainActivity extends Activity {

	public static final int REQUEST_CODE_CAMERA = 1;
	public static final int REQUEST_CODE_ALBUM = 2;
	private Uri mCameraImageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.findViewById(R.id.from_camera)
			.setOnClickListener(mOnClickListener);
		this.findViewById(R.id.from_album).setOnClickListener(mOnClickListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mCameraImageUri != null) {
			outState.putParcelable("camera_image_uri", mCameraImageUri);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("camera_image_uri")) {
				mCameraImageUri = savedInstanceState
					.getParcelable("camera_image_uri");
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
			case REQUEST_CODE_CAMERA:
				startCropper(REQUEST_CODE_CAMERA, data);
				break;
			case REQUEST_CODE_ALBUM:
				startCropper(REQUEST_CODE_ALBUM, data);
				break;
			default:
				break;
		}
	}

	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.from_camera: {
					onSelectCamera();
					break;
				}
				case R.id.from_album: {
					onSelectAlbum();
					break;
				}
				default:
					break;
			}
		}
	};

	private void onSelectCamera() {
		if (!getPackageManager()
			.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			Toast.makeText(this, R.string.camera_not_supported,
				Toast.LENGTH_SHORT).show();
			return;
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (intent.resolveActivity(getPackageManager()) != null) {
			intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);

			mCameraImageUri = CropFileUtils
				.createRandomFileUri(getApplicationContext(), "ImageCropSample");
			if (mCameraImageUri == null) {
				return;
			}

			intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri);
			startActivityForResult(intent, REQUEST_CODE_CAMERA);
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void onSelectAlbum() {


		if (Build.VERSION.SDK_INT <= 19) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, REQUEST_CODE_ALBUM);
		} else {
			Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			intent.setType("image/*");
			startActivityForResult(intent, REQUEST_CODE_ALBUM);
		}

//		if (Build.VERSION.SDK_INT < 19) {
//			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//			intent.setType("image/*");
//			startActivityForResult(intent, REQUEST_CODE_ALBUM);
//		} else {
//			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//			intent.addCategory(Intent.CATEGORY_OPENABLE);
//			intent.setType("image/*");
//			startActivityForResult(intent, REQUEST_CODE_ALBUM);
//		}
	}

	/**
	 * start crop
	 *
	 * @param requestCode
	 * @param data
	 */
	private void startCropper(int requestCode, Intent data) {
		Uri uri = null;
		if (requestCode == REQUEST_CODE_CAMERA) {
			uri = mCameraImageUri;
		} else if (data != null && data.getData() != null) {
			uri = data.getData();
		} else {
			return;
		}
		Intent intent = new Intent(this, CropActivity.class);
		intent.setData(uri);
		intent.putExtra("outputX", CropUtils.dip2px(this, 150));
		intent.putExtra("outputY", CropUtils.dip2px(this, 150));
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		startActivity(intent);
	}
}
