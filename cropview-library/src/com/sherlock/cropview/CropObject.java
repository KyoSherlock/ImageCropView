package com.sherlock.cropview;

import android.graphics.Rect;
import android.graphics.RectF;

public class CropObject {

	private RectF mImageRect;
	private RectF mCropRect;

	/**
	 * @param imageRect
	 * @param cropRect
	 * @param rotation
	 */
	public CropObject(RectF imageRect, Rect cropRect, int rotation) {
		mImageRect = imageRect;
		mCropRect = new RectF(cropRect);
	}

	public void getImageRect(RectF r) {
		r.set(mImageRect);
	}

	public RectF getImageRect() {
		return new RectF(mImageRect);
	}

	public void getCropRect(RectF r) {
		r.set(mCropRect);
	}

	public RectF getCropRect() {
		return new RectF(mCropRect);
	}
}
