/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jasonsoft.softwareaudioplayer;

import android.content.Context;
import android.graphics.Bitmap;

import com.jasonsoft.softwareaudioplayer.cache.CacheManager;
import com.jasonsoft.softwareaudioplayer.data.AsyncDrawable;
import com.jasonsoft.softwareaudioplayer.data.LoadThumbnailParams;
import com.jasonsoft.softwareaudioplayer.data.LoadThumbnailResult;

public class Utils {

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public Utils() {
        super();
    }

    /**
     * The number of ms in one second.
     */
    public static final long ONE_SECOND = 1000;

    /**
     * The number of ms in one minute.
     */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    /**
     * The number of ms in an hour.
     */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;

    /**
     * Returns a human-readable version of the file size, where the input
     * represents a specific number of bytes.
     *
     * @param size  the number of bytes
     * @return a human-readable display value (includes units)
     */
    public static String msToDisplayDuration(long ms) {
        if (ms / ONE_HOUR > 0) {
            long remainSeconds = (ms % ONE_HOUR) / ONE_SECOND;
            return (ms / ONE_HOUR) + "h:" + (remainSeconds / ONE_MINUTE) + "m:" + (remainSeconds % ONE_MINUTE) + "s";
        } else if (ms / ONE_MINUTE > 0) {
            long remainSeconds = (ms % ONE_MINUTE) / ONE_SECOND;
            return (ms / ONE_MINUTE) + "m:" + remainSeconds + "s";
        } else {
            return (ms / ONE_SECOND) + "s";
        }
    }

    public static void loadThumbnail(Context context, long origId, RoundedCornerImageView thumbnailView, Bitmap defaultBitmap) {
        final String imageKey = String.valueOf(origId);
        final Bitmap bitmap = CacheManager.getInstance().getMemoryCache().get(imageKey);

        if (bitmap != null) {
            thumbnailView.setImageBitmap(bitmap);
        } else if (cancelPotentialWork(origId, thumbnailView)) {
            final LoadThumbnailTask task = new LoadThumbnailTask(context, thumbnailView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(defaultBitmap, task);
            thumbnailView.setImageDrawable(asyncDrawable);
            task.execute(new LoadThumbnailParams(origId));
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no work in
     * progress on this image view.
     * Returns false if the work in progress deals with the same data. The work is not
     * stopped in that case.
     */
    public static boolean cancelPotentialWork(Object data, RoundedCornerImageView imageView) {
        final LoadThumbnailTask loadPhotoTask = LoadThumbnailTask.getLoadThumbnailTask(imageView);

        if (loadPhotoTask != null) {
            final Object bitmapData = loadPhotoTask.getData();
            if (bitmapData == null || !bitmapData.equals(data)) {
                loadPhotoTask.cancel(true);
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }
}
