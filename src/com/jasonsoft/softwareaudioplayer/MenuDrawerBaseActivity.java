package com.jasonsoft.softwareaudioplayer;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.LruCache;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import com.jasonsoft.softwareaudioplayer.adapter.MenuAdapter;
import com.jasonsoft.softwareaudioplayer.data.MenuDrawerCategory;
import com.jasonsoft.softwareaudioplayer.data.MenuDrawerItem;

import java.util.ArrayList;
import java.util.List;

public abstract class MenuDrawerBaseActivity extends FragmentActivity implements MenuAdapter.MenuListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_ACTIVE_POSITION =
            "net.simonvt.menudrawer.samples.LeftDrawerSample.activePosition";

    // These are the Contacts rows that we will retrieve.
    static final String[] AUDIO_SUMMARY_PROJECTION = new String[] {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            MediaStore.Audio.AudioColumns.ARTIST,
            MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.MediaColumns.DATA
    };

    protected MenuDrawer mMenuDrawer;

    protected MenuAdapter mAdapter;
    protected ListView mList;

    private int mActivePosition = 0;

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);

        if (inState != null) {
            mActivePosition = inState.getInt(STATE_ACTIVE_POSITION);
        }

        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.BEHIND, getDrawerPosition(), getDragMode());

        mList = new ListView(this);
        // Create an empty adapter we will use to display the loaded data.

        mAdapter = new MenuAdapter(this, null);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mItemClickListener);
//        mList.setDivider(this.getResources().getDrawable(R.drawable.list_divider));

        mMenuDrawer.setMenuView(mList);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    protected abstract void onMenuItemClicked(int position, MenuDrawerItem item);
    protected abstract void onMenuItemClicked(int position);

    protected abstract int getDragMode();

    protected abstract Position getDrawerPosition();

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);
            android.util.Log.d("jason", "onItemClick position:" + position);
            android.util.Log.d("jason", "onItemClick getItem:" + mAdapter.getItem(position));

//            mAdapter.setActivePosition(position);
//            onMenuItemClicked(position, (MenuDrawerItem) mAdapter.getItem(position));
            onMenuItemClicked(position);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
    }

    @Override
    public void onActiveViewChanged(View v) {
        mMenuDrawer.setActiveView(v, mActivePosition);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//    String[] columns = {
//                MediaStore.Audio.AudioColumns._ID,
//                        MediaStore.Audio.AudioColumns.TITLE,
//                                MediaStore.Audio.AudioColumns.ARTIST
//                                        };
//
//    String selection = MediaStore.Audio.AudioColumns.DATA + "=?";
//    String selectionArgs[] = { "/mnt/sdcard/Movies/landscapes.mp4" };
//
//    Cursor cursor = context.getContentResolver().query(uri, columns, selection, selectionArgs, null);
//
//
//
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri;
        //        if (mCurFilter != null) {
        //            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
        //                    Uri.encode(mCurFilter));
        //        } else {
        baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
            + Contacts.HAS_PHONE_NUMBER + "=1) AND ("
            + Contacts.DISPLAY_NAME + " != '' ))";
        return new CursorLoader(this, baseUri,
                AUDIO_SUMMARY_PROJECTION, null, null,
                MediaStore.Audio.AudioColumns.DURATION + " COLLATE LOCALIZED DESC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
}
