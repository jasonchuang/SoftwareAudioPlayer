/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import com.jasonsoft.softwareaudioplayer.data.MenuDrawerItem;
import android.provider.MediaStore.Video.VideoColumns;
/**
 * Displays an Android spinner widget backed by data in an array. The
 * array is loaded from the strings.xml resources file.
 */
public class SoftwareAudioPlayerActivity extends MenuDrawerBaseActivity {
    private VideoSurfaceView mVideoSurfaceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMenuDrawer.setContentView(R.layout.main);
        mVideoSurfaceView = (VideoSurfaceView) findViewById(R.id.surface_view);
    }

    /**
     * Since onResume() is always called when an Activity is starting, even if it is re-displaying
     * after being hidden, it is the best place to restore state.
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onMenuItemClicked(int position, MenuDrawerItem item) {
        mMenuDrawer.closeMenu();
    }

    @Override
    protected void onMenuItemClicked(int position) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        long origId = cursor.getInt(cursor.getColumnIndex(VideoColumns._ID));
        String data = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        if (data != null) {
            stopVideo();
            playVideo(data);
        }
    }

    private void stopVideo() {
        mVideoSurfaceView.stopPlayback();
    }

    private void playVideo(String path) {
//        path = "/sdcard/ImYours.m4a";
        mVideoSurfaceView.setVideoPath(path);
        mVideoSurfaceView.setMediaController(new MediaController(this));
        mVideoSurfaceView.start();
    }

    @Override
    protected int getDragMode() {
        return MenuDrawer.MENU_DRAG_CONTENT;
    }

    @Override
    protected Position getDrawerPosition() {
        // START = left, END = right
        return Position.START;
    }

}
