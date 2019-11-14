package com.danielogbuti.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "ogbuti.photogallery";
    GridView gridView;
    ArrayList<GalleryItem> items;
    public static int pages = 1;
    ThumbnailDownloader<ImageView> thumbnailThread;

    @Override
    public void onCreate(@Nullable  Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("FlickrFetchr",""+new FlickrFetcher().getPAGER());

        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        //the constructor works as a handler that that works with the UI thread
        thumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        thumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
               //check if the imageview is visible
                if (isVisible()){
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        thumbnailThread.start();
        thumbnailThread.getLooper();
        Log.i("FlickrFetchr","Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            //Pull out of searchview
            MenuItem menuItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView)menuItem.getActionView();


            //Get the data from our searchable.xml as a SearchableInfo
            SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo info = searchManager.getSearchableInfo(name);

            searchView.setSearchableInfo(info);




        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()){
           case R.id.menu_item_search:
               String query = PreferenceManager.getDefaultSharedPreferences(getActivity())
                       .getString(FlickrFetcher.PREF_SEARCH_QUERY,null);
               getActivity().startSearch(query,true,null,false);
               return true;
           case R.id.menu_item_clear:
               PreferenceManager.getDefaultSharedPreferences(getActivity())
                       .edit()
                       .putString(FlickrFetcher.PREF_SEARCH_QUERY,null)
                       .apply();
               updateItems();
               return true;

           case R.id.menu_item_toggle_polliing:
               //check if alarm is running
               boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
               PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
               getActivity().invalidateOptionsMenu();
               return true;

           default:
               return super.onOptionsItemSelected(item);
       }



    }

    //use this method to update the menu bar text in real time
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polliing);
        if (PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        gridView = (GridView)v.findViewById(R.id.gridView);
        Log.i("FlickrFetchr",""+new FlickrFetcher().getPAGER());

        setupAdapter();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GalleryItem item = items.get(position);

                Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
                Intent i = new Intent(getActivity(),PhotoPageActivity.class);
                i.setData(photoPageUri);

                startActivity(i);

            }
        });
        
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thumbnailThread.quit();
        Log.i("FlickrFetchr","Thumbnail thread exited");
    }

    public void updateItems(){
        new FetchItemTask().execute(pages);
    }

    void setupAdapter(){

        if (getActivity() == null || gridView == null){
            return ;
        }
        if (items!= null){
            gridView.setAdapter(new GalleryItemAdapter(getActivity(),R.layout.gallery_item,items));
//            if (items.get(items.size()-1 ) != null){

                FlickrFetcher flickrFetcher = new FlickrFetcher();
                    //pages++;
                    int page = 1;
    //                new FetchItemTask().execute(pages);
  //          }
        }else {
            gridView.setAdapter(null);
        }

    }

    public class FetchItemTask extends AsyncTask<Integer,Void,ArrayList<GalleryItem>>{


        @Override
        protected ArrayList<GalleryItem> doInBackground(Integer... voids) {
            Activity activity = getActivity();
            if (activity == null){
                return new ArrayList<GalleryItem>();
            }

            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetcher.PREF_SEARCH_QUERY,null);

            Log.d(TAG,"The query is "+query);


            if (query != null){
                return new FlickrFetcher().search(query,getActivity());
            }else {
                return new FlickrFetcher().fetchItems(voids[0],getActivity());
            }



        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            items = galleryItems;

            String total = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(FlickrFetcher.PREF_SEARCH_RESULTS,null);
            Toast.makeText(getActivity(),"There are "+total+" results",Toast.LENGTH_SHORT).show();

           // items.addAll(galleryItems);
            //to update the gridview after flickrFetchr finishes running
            setupAdapter();

        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem>{
        public GalleryItemAdapter( Context context, int resource,  List<GalleryItem> objects) {
            super(getActivity(), 0, objects);
        }

        @Override public View getView(int position,  View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item,parent,false);
            }

            ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.ic_launcher_background);
            //get the next ten bitmaps and store them in caches
            for (int i = position; i<position+10;i++){
                try {
                    GalleryItem save_item = getItem(i);
                    thumbnailThread.saveBitmap(save_item.getUrl());
                }catch (IndexOutOfBoundsException e){
                    break;
                }
            }
            GalleryItem item = getItem(position);

            thumbnailThread.queueThumbnail(imageView,item.getUrl());

            return convertView;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        thumbnailThread.clearQueue();
    }
}
