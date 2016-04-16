package com.kyo.imagecrop;

import android.graphics.Rect;
import android.graphics.RectF;

public class CropObject {

	private RectF mImageRect;
	private RectF mCropRect;
	private RectF mImageOriginalRect;

	/**
	 * @param imageRect
	 * @param cropRect
	 */
	public CropObject(RectF imageRect, RectF imageOriginalRect, Rect cropRect) {
		mImageRect = imageRect;
		mImageOriginalRect = imageOriginalRect;
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

	public RectF getImageOriginalRect() {
		return new RectF(mImageOriginalRect);
	}

	public void getmImageOriginalRect(RectF r) {
		r.set(mImageOriginalRect);
	}
}
