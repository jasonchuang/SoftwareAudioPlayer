package com.jasonsoft.softwareaudioplayer.cache;

import android.graphics.Bitmap;
import android.util.LruCache;

public class CacheManager {
    private static final String TAG = CacheManager.class.getSimpleName();

    private static CacheManager sInstance;
    private LruCache<String, Bitmap> mMemoryCache;

    private CacheManager() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
        android.util.Log.d("jason", "LruCache sizeOf bitmap.getByteCount():" +  bitmap.getByteCount());
                return bitmap.getByteCount() / 1024;
            }
        };
        android.util.Log.d("jason", "Init mMemoryCache with size kilobytes:" +  cacheSize);

    }

    synchronized public static CacheManager getInstance() {
        if (sInstance == null) {
            sInstance = new CacheManager();
        }
        return sInstance;
    }

    public LruCache<String, Bitmap> getMemoryCache() {
        return mMemoryCache;
    }

    public void addThumbnailToMemoryCache(String key, Bitmap thumbnail) {
        if (getThumbnailFromMemCache(key) == null) {
            mMemoryCache.put(key, thumbnail);
        }
    }

    public Bitmap getThumbnailFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
}
