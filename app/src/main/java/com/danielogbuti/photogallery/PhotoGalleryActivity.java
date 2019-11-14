package com.danielogbuti.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    String query;
    private static final String TAG = "ogbuti.photogallery";
    @Override
    protected Fragment createFragment() {
        return new PhotoGalleryFragment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        PhotoGalleryFragment fragment = (PhotoGalleryFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "The search query is " + query);


            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(FlickrFetcher.PREF_SEARCH_QUERY, query)
                    .apply();
        }
        fragment.updateItems();
    }


}
