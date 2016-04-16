package com.kyo.imagecrop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

public final class ImageLoader {

	public static final String JPEG_MIME_TYPE = "image/jpeg";
	public static final int DEFAULT_COMPRESS_QUALITY = 95;
	private static final int BITMAP_LOAD_BACKOUT_ATTEMPTS = 5;

	private ImageLoader() {
	}

	/**
	 * Returns the Mime type for a Url. Safe to use with Urls that do not come
	 * from Gallery's content provider.
	 */
	public static String getMimeType(Uri src) {
		String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
		String ret = null;
		if (postfix != null) {
			ret = MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
		}
		return ret;
	}

	public static String getLocalPathFromUri(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver()
				.query(uri, new String[] { MediaStore.Images.Media.DATA },
						null, null, null);
		if (cursor == null) {
			return null;
		}
		int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(index);
	}

	/**
	 * Returns the bounds of the bitmap stored at a given Url.
	 */
	public static Rect loadBitmapBounds(Context context, Uri uri) {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		loadBitmap(context, uri, o);
		return new Rect(0, 0, o.outWidth, o.outHeight);
	}

	/**
	 * Loads a bitmap that has been downsampled using sampleSize from a given
	 * url.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static Bitmap loadDownsampledBitmap(Context context, Uri uri,
			int sampleSize) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		if (Build.VERSION.SDK_INT >= 11) {
			options.inMutable = true;
		}
		options.inSampleSize = sampleSize;
		return loadBitmap(context, uri, options);
	}

	/**
	 * Returns the bitmap from the given uri loaded using the given options.
	 * Returns null on failure.
	 */
	public static Bitmap loadBitmap(Context context, Uri uri,
			BitmapFactory.Options o) {
		if (uri == null || context == null) {
			throw new IllegalArgumentException("bad argument to loadBitmap");
		}
		InputStream is = null;
		try {
			is = context.getContentResolver().openInputStream(uri);
			return BitmapFactory.decodeStream(is, null, o);
		} catch (FileNotFoundException e) {
			// Log.e(LOGTAG, "FileNotFoundException for " + uri, e);
		} finally {
			CropUtils.closeSilently(is);
		}
		return null;
	}

	/**
	 * Loads a bitmap at a given URI that is downsampled so that both sides are
	 * smaller than maxSideLength. The Bitmap's original dimensions are stored
	 * in the rect originalBounds.
	 * 
	 * @param uri
	 *            URI of image to open.
	 * @param context
	 *            context whose ContentResolver to use.
	 * @param maxSideLength
	 *            max side length of returned bitmap.
	 * @param originalBounds
	 *            If not null, set to the actual bounds of the stored bitmap.
	 * @param useMin
	 *            use min or max side of the original image
	 * @return downsampled bitmap or null if this operation failed.
	 */
	public static Bitmap loadConstrainedBitmap(Uri uri, Context context,
			int maxSideLength, Rect originalBounds, boolean useMin) {
		if (maxSideLength <= 0 || uri == null || context == null) {
			throw new IllegalArgumentException(
					"bad argument to getScaledBitmap");
		}
		// Get width and height of stored bitmap
		Rect storedBounds = loadBitmapBounds(context, uri);
		if (originalBounds != null) {
			originalBounds.set(storedBounds);
		}
		int w = storedBounds.width();
		int h = storedBounds.height();

		// If bitmap cannot be decoded, return null
		if (w <= 0 || h <= 0) {
			return null;
		}

		// Find best downsampling size
		int imageSide = 0;
		if (useMin) {
			imageSide = Math.min(w, h);
		} else {
			imageSide = Math.max(w, h);
		}
		int sampleSize = 1;
		while (imageSide > maxSideLength) {
			imageSide >>>= 1;
			sampleSize <<= 1;
		}

		// Make sure sample size is reasonable
		if (sampleSize <= 0 || 0 >= (int) (Math.min(w, h) / sampleSize)) {
			return null;
		}
		return loadDownsampledBitmap(context, uri, sampleSize);
	}

	/**
	 * Loads a bitmap that is downsampled by at least the input sample size. In
	 * low-memory situations, the bitmap may be downsampled further.
	 */
	public static Bitmap loadBitmapWithBackouts(Context context, Uri sourceUri,
			int sampleSize) {
		boolean noBitmap = true;
		int num_tries = 0;
		if (sampleSize <= 0) {
			sampleSize = 1;
		}
		Bitmap bmap = null;
		while (noBitmap) {
			try {
				// Try to decode, downsample if low-memory.
				bmap = loadDownsampledBitmap(context, sourceUri, sampleSize);
				noBitmap = false;
			} catch (OutOfMemoryError e) {
				// Try with more downsampling before failing for good.
				if (++num_tries >= BITMAP_LOAD_BACKOUT_ATTEMPTS) {
					throw e;
				}
				bmap = null;
				System.gc();
				sampleSize *= 2;
			}
		}
		return bmap;
	}

	/**
	 * Loads bitmap from a resource that may be downsampled in low-memory
	 * situations.
	 */
	public static Bitmap decodeResourceWithBackouts(Resources res,
			BitmapFactory.Options options, int id) {
		boolean noBitmap = true;
		int num_tries = 0;
		if (options.inSampleSize < 1) {
			options.inSampleSize = 1;
		}
		// Stopgap fix for low-memory devices.
		Bitmap bmap = null;
		while (noBitmap) {
			try {
				// Try to decode, downsample if low-memory.
				bmap = BitmapFactory.decodeResource(res, id, options);
				noBitmap = false;
			} catch (OutOfMemoryError e) {
				// Retry before failing for good.
				if (++num_tries >= BITMAP_LOAD_BACKOUT_ATTEMPTS) {
					throw e;
				}
				bmap = null;
				System.gc();
				options.inSampleSize *= 2;
			}
		}
		return bmap;
	}

	/**
	 * @return 0, 90, 180 or 270. 0 could be returned if there is no data about
	 *         rotation
	 */
	public static int getImageOrientation(Context context, Uri imageUri) {
		try {
			ExifInterface exif = new ExifInterface(CropFileUtils.getPath(
					context, imageUri));
			int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_UNDEFINED);

			if (rotation == ExifInterface.ORIENTATION_UNDEFINED)
				return getOrientationFromMediaStore(context, imageUri);
			else
				return exifToDegrees(rotation);
		} catch (IOException e) {
			return 0;
		}
	}

	private static int getOrientationFromMediaStore(Context context,
			Uri imageUri) {
		String[] columns = { MediaStore.Images.Media.DATA,
				MediaStore.Images.Media.ORIENTATION };
		Cursor cursor = context.getContentResolver().query(imageUri, columns,
				null, null, null);
		if (cursor == null)
			return 0;

		cursor.moveToFirst();

		int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
		return cursor.getInt(orientationColumnIndex);
	}

	private static int exifToDegrees(int exifOrientation) {
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
			return 90;
		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
			return 180;
		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
			return 270;
		} else {
			return 0;
		}
	}

}
