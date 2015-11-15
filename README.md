# CropView

Crop large photo with translation, scale and rotation (without distortion and OOM).

![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/0.png) ![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/1.png) ![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/2.png)
![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/0.png) ![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/3.png) ![](https://github.com/KyoSherlock/CropView/raw/master/screenshots/4.png)

# Usage

Below is an example of a CropView.

```java
	mCropLayout = (CropLayout) this.findViewById(R.id.crop);
	mCropLayout.setOnCropListener(new OnCropListener() {

		@Override
		public void onCropSuccess(Uri data) {
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
	});
	mCropLayout.startCropImage(sourceUri, outputX, outputY);
```

# Changelog

### Version: 1.0

  * Initial Build

# License

    Copyright 2015, KyoSherlock
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
