package com.jasonsoft.softwareaudioplayer.data;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.ref.WeakReference;

import com.jasonsoft.softwareaudioplayer.LoadThumbnailTask;

/**
 * A custom Drawable that will be attached to the imageView while the work is in progress.
 * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
 * required, and makes sure that only the last started worker process can bind its result,
 * independently of the finish order.
 */
public class AsyncDrawable extends BitmapDrawable {
    private final WeakReference<LoadThumbnailTask> loadPhotoTaskTaskReference;

    public AsyncDrawable(Bitmap bitmap, LoadThumbnailTask loadPhotoTask) {
        super(bitmap);
        loadPhotoTaskTaskReference =
            new WeakReference<LoadThumbnailTask>(loadPhotoTask);
    }

    public LoadThumbnailTask getLoadThumbnailTask() {
        return loadPhotoTaskTaskReference.get();
    }
}
