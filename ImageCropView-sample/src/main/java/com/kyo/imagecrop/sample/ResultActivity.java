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
