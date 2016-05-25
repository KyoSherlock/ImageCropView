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
package com.kyo.imagecrop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.kyo.imagecropview.R;

public class CropLayout extends FrameLayout {

	private static final String LOGTAG = "CropLayout";
	private static final int DEFAULT_COMPRESS_QUALITY = 90;
	private LoadBitmapTask mLoadBitmapTask;
	private CropBitmapTask mCropBitmapTask;
	private Uri mSourceUri;
	private Bitmap mOriginalBitmap;
	private RectF mOriginalBounds;
	private Rect mCropRect;
	private int mRotation;
	private String mOutputFormat = null;
	private boolean mIsAttachedToWindow;
	private CropView mCropView;
	private ProgressBar mProgressBar;
	private OnCropListener mOnCropListener;

	public CropLayout(Context context) {
		super(context);
		this.setup(context);
	}

	public CropLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setup(context);
	}

	public CropLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.setup(context);
	}

	@TargetApi(21)
	public CropLayout(Context context, AttributeSet attrs, int defStyleAttr,
					  int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.setup(context);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mIsAttachedToWindow = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mIsAttachedToWindow = false;
		performLoadingStateChanged(false);
		if (mLoadBitmapTask != null) {
			mLoadBitmapTask.cancel(false);
		}
		if (mCropBitmapTask != null) {
			mCropBitmapTask.cancel(false);
		}
	}

	private void setup(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		mCropView = (CropView) inflater.inflate(R.layout.cropview, this, false);
		mProgressBar = (ProgressBar) inflater.inflate(
			R.layout.cropview_progressbar, this, false);
		mProgressBar.setVisibility(View.GONE);
		LayoutParams lp = (LayoutParams) mProgressBar
			.getLayoutParams();
		lp.gravity = Gravity.CENTER;

		this.addView(mCropView);
		this.addView(mProgressBar);
	}

	public void setOnCropListener(OnCropListener l) {
		mOnCropListener = l;
	}

	/**
	 * @param format jpg/png
	 */
	public void setOutputFormat(String format) {
		mOutputFormat = format;
	}

	/**
	 * Method that loads a bitmap in an async task.
	 */
	public void startCropImage(Uri uri, int outputX, int outputY) {
		if (uri == null) {
			cannotLoadImage("uri is null");
			return;
		} else if (outputX <= 0 || outputY <= 0) {
			cannotLoadImage("outputX/outputY is invalid");
			return;
		}
		mSourceUri = uri;
		performLoadingStateChanged(true);
		mCropRect = new Rect(0, 0, outputX, outputY);
		mLoadBitmapTask = new LoadBitmapTask();
		mLoadBitmapTask.execute(uri);
	}

	public void requestCropResult() {
		if (mProgressBar.getVisibility() == View.INVISIBLE) {
			return;
		}
		this.performLoadingStateChanged(true);
		CropView.Result result = mCropView.getCropResult();
		Uri destUri = CropFileUtils.createRandomFileUri(getContext(), "ImageCropSample");
		mCropBitmapTask = new CropBitmapTask(mSourceUri, destUri,
			mOutputFormat, result, mOriginalBounds, mCropRect.width(),
			mCropRect.height());
		mCropBitmapTask.execute();
	}

	/**
	 * AsyncTask for loading a bitmap into memory.
	 */
	private class LoadBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
		int mBitmapSize;
		Context mContext;
		Rect mOriginalBounds;
		int mOrientation;

		public LoadBitmapTask() {
			mBitmapSize = getScreenImageSize();
			mContext = CropLayout.this.getContext().getApplicationContext();
			mOriginalBounds = new Rect();
			mOrientation = 0;
		}

		@Override
		protected Bitmap doInBackground(Uri... params) {
			Uri uri = params[0];
			Bitmap bmap = ImageLoader.loadConstrainedBitmap(uri, mContext,
				mBitmapSize, mOriginalBounds, false);
			mOrientation = ImageLoader.getImageOrientation(mContext, uri);
			return bmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			doneLoadBitmap(result, new RectF(mOriginalBounds), mOrientation);
		}
	}

	/**
	 * Method called on UI thread with loaded bitmap.
	 */
	private void doneLoadBitmap(Bitmap bitmap, RectF bounds, int orientation) {
		performLoadingStateChanged(false);
		if (!mIsAttachedToWindow) {
			Log.w(LOGTAG, "doneLoadBitmap, view is not attached to window!");
			return;
		}

		mOriginalBitmap = bitmap;
		mOriginalBounds = bounds;
		mRotation = orientation;
		if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
			notifyDisplay();
		} else {
			cannotLoadImage("could not load image for cropping");
		}
	}

	/**
	 * AsyncTask for cropping a bitmap to file.
	 */
	private class CropBitmapTask extends AsyncTask<Void, Void, Boolean> {
		CropView.Result mResult = null;
		RectF mOriginalBounds = null;
		InputStream mInStream = null;
		OutputStream mOutStream = null;
		String mOutputFormat = null;
		Uri mOutUri = null;
		Uri mInUri = null;
		int mOutputX = 0;
		int mOutputY = 0;

		public CropBitmapTask(Uri sourceUri, Uri destUri, String outputFormat,
							  CropView.Result result, RectF originalBounds, int outputX,
							  int outputY) {
			mOutputFormat = outputFormat;
			mOutStream = null;
			mOutUri = destUri;
			mInUri = sourceUri;
			mOutputX = outputX;
			mOutputY = outputY;
			mResult = result;
			mOriginalBounds = originalBounds;
			try {
				mOutStream = getContext().getContentResolver()
					.openOutputStream(mOutUri);
			} catch (FileNotFoundException e) {
				Log.w(LOGTAG, "cannot make file: " + mInUri.toString(), e);
				e.printStackTrace();
			}

			try {
				mInStream = getContext().getContentResolver().openInputStream(
					mInUri);
			} catch (FileNotFoundException e) {
				Log.w(LOGTAG, "cannot read file: " + mInUri.toString(), e);
				e.printStackTrace();
			}
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		protected Boolean doInBackground(Void... params) {
			boolean failure = false;

			RectF returnRect = new RectF(0, 0, mOutputX, mOutputY);
			Bitmap canvasBitmap = Bitmap.createBitmap((int) returnRect.width(),
				(int) returnRect.height(), Bitmap.Config.ARGB_8888);
			canvasBitmap.eraseColor(Color.WHITE);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setFilterBitmap(true);
			Canvas canvas = new Canvas(canvasBitmap);
			Bitmap cropBitmap = null;
			if (mResult != null) {
				RectF trueCrop = CropMath.getScaledCropBounds(
					mResult.rawIntersectionRect, mResult.rawImageRect,
					mOriginalBounds);
				if (trueCrop == null) {
					Log.w(LOGTAG, "cannot find crop for full size image");
					failure = true;
					return false;
				}

				Rect roundedTrueCrop = new Rect();
				trueCrop.roundOut(roundedTrueCrop);
				if (roundedTrueCrop.width() <= 0
					|| roundedTrueCrop.height() <= 0) {
					Log.w(LOGTAG, "crop has bad values for full size image");
					failure = true;
					return false;
				}

				// Attempt to open a region decoder
				BitmapRegionDecoder decoder = null;
				try {
					decoder = BitmapRegionDecoder.newInstance(mInStream, true);
				} catch (IOException e) {
					Log.w(LOGTAG, "cannot open region decoder for file: "
						+ mInUri.toString(), e);
					failure = true;
					return false;
				}

				cropBitmap = null;
				if (decoder != null) {
					// Do region decoding to get crop bitmap
					BitmapFactory.Options options = new BitmapFactory.Options();
					if (Build.VERSION.SDK_INT >= 11) {
						options.inMutable = true;
					}

					if (mOutputX != 0 && mOutputY != 0) {
						options.inSampleSize = CropMath.calculateInSampleSize(
							roundedTrueCrop.right - roundedTrueCrop.left,
							roundedTrueCrop.bottom - roundedTrueCrop.top,
							mOutputX, mOutputY);
					}
					cropBitmap = decoder.decodeRegion(roundedTrueCrop, options);
					decoder.recycle();
				}

				if (cropBitmap == null) {
					Log.w(LOGTAG, "cannot decode file: " + mInUri.toString());
					failure = true;
					return false;
				}

				Matrix drawMatrix = new Matrix();
				RectF cropRect = new RectF(0, 0, cropBitmap.getWidth(),
					cropBitmap.getHeight());
				drawMatrix.setRectToRect(cropRect, mResult.rawIntersectionRect,
					Matrix.ScaleToFit.FILL);
				drawMatrix.postConcat(mResult.displayImageMatrix);
				Matrix m = new Matrix();
				m.setRectToRect(mResult.displayCropRect, returnRect,
					Matrix.ScaleToFit.FILL);
				drawMatrix.postConcat(m);

				canvas.drawBitmap(cropBitmap, drawMatrix, paint);
			}
			cropBitmap = canvasBitmap;

			// Get output compression format
			CompressFormat cf = CropFileUtils
				.convertExtensionToCompressFormat(CropFileUtils
					.getImageFileExtension(mOutputFormat));

			// If we only need to output to a URI, compress straight to file
			if (mOutStream == null
				|| !cropBitmap.compress(cf, DEFAULT_COMPRESS_QUALITY,
				mOutStream)) {
				Log.w(LOGTAG,
					"failed to compress bitmap to file: "
						+ mOutUri.toString());
				failure = true;
			}
			return !failure; // True if any of the operations failed
		}

		@Override
		protected void onPostExecute(Boolean result) {
			CropUtils.closeSilently(mOutStream);
			CropUtils.closeSilently(mInStream);
			doneCropBitmap(result.booleanValue(), mOutUri);
		}
	}

	private void doneCropBitmap(boolean success, Uri data) {
		performLoadingStateChanged(false);
		if (success) {
			performCropResult(data);
		} else {
			performCropFail(null);
		}
	}

	private void notifyDisplay() {
		if (mOriginalBitmap != null) {
			mCropView.initialize(mOriginalBitmap, mOriginalBounds, mCropRect, mRotation);
		}
	}

	private void cannotLoadImage(String msg) {
		performCropFail(msg);
	}

	private void performCropResult(Uri data) {
		if (mOnCropListener != null) {
			mOnCropListener.onCropResult(data);
		}
	}

	private void performCropFail(String errmsg) {
		Log.w(LOGTAG, errmsg);
		if (mOnCropListener != null) {
			mOnCropListener.onCropFailed(errmsg);
		}
	}

	private void performLoadingStateChanged(boolean isLoading) {
		if (isLoading) {
			mProgressBar.setVisibility(View.VISIBLE);
		} else {
			mProgressBar.setVisibility(View.GONE);
		}

		if (mOnCropListener != null) {
			mOnCropListener.onLoadingStateChanged(isLoading);
		}
	}

	/**
	 * Gets screen size metric.
	 */
	private int getScreenImageSize() {
		DisplayMetrics outMetrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) this.getContext().getSystemService(
			Service.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(outMetrics);
		return (int) Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
	}

	public interface OnCropListener {
		void onCropResult(Uri data);

		void onCropFailed(String errmsg);

		void onLoadingStateChanged(boolean isLoading);
	}
}
