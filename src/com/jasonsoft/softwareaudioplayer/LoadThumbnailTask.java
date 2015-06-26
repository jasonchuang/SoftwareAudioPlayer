package com.jasonsoft.softwareaudioplayer;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.jasonsoft.softwareaudioplayer.cache.CacheManager;
import com.jasonsoft.softwareaudioplayer.data.AsyncDrawable;
import com.jasonsoft.softwareaudioplayer.data.LoadThumbnailParams;
import com.jasonsoft.softwareaudioplayer.data.LoadThumbnailResult;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class LoadThumbnailTask extends AsyncTask<LoadThumbnailParams, Void, LoadThumbnailResult> {
    private Context mContext;
    private final WeakReference<ImageView> thumbnailViewReference;
    private long data = 0;

    // From aosp/packages/providers/MediaProvider
    private static final Uri ALBUMART_URI = Uri.parse("content://media/external/audio/albumart");

    static final String[] AUDIO_ALBUM_PROJECTION = new String[] {
            MediaStore.Audio.AlbumColumns.ALBUM_ART,
            MediaStore.Audio.AlbumColumns.ARTIST,
            MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
    };

    public LoadThumbnailTask(Context context, ImageView thumbnailView) {
        this.mContext = context;
        this.thumbnailViewReference = new WeakReference<ImageView>(thumbnailView);
    }

    @Override
    protected LoadThumbnailResult doInBackground(LoadThumbnailParams... params) {
        data = params[0].origId;
//        Bitmap bitmap = Thumbnails.getThumbnail(mContext.getContentResolver(), data,
//                Thumbnails.MINI_KIND, new Options());
        Bitmap bitmap = getAlbumArtThumbnail(data);
        android.util.Log.d("jason", "doInBackground data:" + data);
        android.util.Log.d("jason", "doInBackground bitmap:" + bitmap);

        if (this.isCancelled()) { return null; }

        if (data > 0 && bitmap != null) {
            CacheManager.getInstance().addThumbnailToMemoryCache(String.valueOf(data), bitmap);
            return new LoadThumbnailResult(bitmap);
        }

        return null;
    }

    private Bitmap getAlbumArtThumbnail(long albumId) {
        Uri albumArtUri = ContentUris.withAppendedId(ALBUMART_URI, albumId);
        Bitmap bitmap = null;

        try {
            bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), albumArtUri);
            bitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true);
        } catch (FileNotFoundException exception) {
        } catch (IOException e) {
        }

        return bitmap;
    }

    @Override
    protected void onPostExecute(LoadThumbnailResult result) {
        if (isCancelled() || null == result) {
            return;
        }

        final ImageView thumbnailView = thumbnailViewReference.get();
        final LoadThumbnailTask loadThumbnailTask = getLoadThumbnailTask(thumbnailView);
        if (this == loadThumbnailTask && thumbnailView != null) {
            setThumbnail(result.bitmap, thumbnailView);
        }
    }

    public long getData() {
        return data;
    }
    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with this imageView.
     * null if there is no such task.
     */
    public static LoadThumbnailTask getLoadThumbnailTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getLoadThumbnailTask();
            }
        }
        return null;
    }

    void setThumbnail(Bitmap bitmap, ImageView thumbnailView) {
        thumbnailView.setImageBitmap(bitmap);
        thumbnailView.setVisibility(View.VISIBLE);
    }

}
