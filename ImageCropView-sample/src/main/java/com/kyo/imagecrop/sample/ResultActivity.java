package com.kyo.imagecrop.sample;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.kyo.imagecrop.CropUtils;


public class ResultActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_result);

		Intent intent = getIntent();

		InputStream input = null;
		try {
			if (intent != null && intent.getData() != null) {
				input = getContentResolver().openInputStream(intent.getData());
				ImageView imageView = (ImageView) this.findViewById(R.id.image);
				imageView.setImageBitmap(BitmapFactory.decodeStream(input));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			CropUtils.closeSilently(input);
		}

	}
}
