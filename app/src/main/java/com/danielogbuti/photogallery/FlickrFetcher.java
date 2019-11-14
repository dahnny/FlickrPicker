package com.danielogbuti.photogallery;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FlickrFetcher {
    public static final String TAG = "ogbuti.photogallery";
    public static final String PREF_SEARCH_QUERY = "searchQuery";
    public static final String PREF_SEARCH_RESULTS = "result";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";

    public  String PAGER;
    private int PAGE;

    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "66aebd65ab48da02a828c19c14187148";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text";

    //include the url for the small version of the picture if avaialble
    private static final String EXTRA_SMALL_URL = "url_s";
    private static final String XML_PHOTO = "photo";
    private static final String PHOTOS = "photos";

    Page p =new Page();

    public String getPAGER() {
        if (p.getPage() == null){return "0";}
        return p.getPage();
    }

    byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){return null;}

            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while ((bytesRead = inputStream.read(buffer))> 0 ){
                outputStream.write(buffer,0,bytesRead);
            }
            outputStream.close();
            return outputStream.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrl(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

   public ArrayList<GalleryItem> downloadGalleryItems(String url,Context context){

        ArrayList<GalleryItem> items = new ArrayList<>();

        try {

            Log.i(TAG,url);
            String xmlString = getUrl(url);
            Log.i(TAG,"Received xml "+xmlString);
            //getting an instance of XmlPullParser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));
            //calling the method
            parseItems(items,context,parser);
        }catch (IOException e){
            Log.e(TAG,"Failed to fetch items", e);
        }catch (XmlPullParserException xppe){
            Log.e(TAG,"Failed to parse items",xppe);
        }
        return items;
    }
    public ArrayList<GalleryItem>  fetchItems(int value,Context context){
        //build the adequate  url
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method",METHOD_GET_RECENT)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter("page",""+value)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .build().toString();
        return downloadGalleryItems(url,context);
    }

    public ArrayList<GalleryItem> search(String query,Context context){
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method",METHOD_SEARCH)
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter(PARAM_EXTRAS,EXTRA_SMALL_URL)
                .appendQueryParameter(PARAM_TEXT,query)
                .build().toString();
        return downloadGalleryItems(url,context);
    }

    void parseItems(ArrayList<GalleryItem> galleryItems, Context context,XmlPullParser parser)throws XmlPullParserException,IOException {
        int eventType = parser.next();
        //if it is not yet the end of the document keep looping
        while(eventType !=XmlPullParser.END_DOCUMENT){
            //if it is in the start tag and name of the start tag equals to photo
            if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())){
                //get the attributes
                String id = parser.getAttributeValue(null,"id");
                String caption = parser.getAttributeValue(null,"title");
                String url = parser.getAttributeValue(null,EXTRA_SMALL_URL);
                String owner = parser.getAttributeValue(null,"owner");
                //create an instance of the GalleryItem class
                GalleryItem item = new GalleryItem();
                //set the attributes to the class methods
                item.setId(id);
                item.setCaption(caption);
                item.setOwner(owner);
                item.setUrl(url);
                //add the items to the arraylist
                galleryItems.add(item);
            }
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(PHOTOS)){
                String total = parser.getAttributeValue(null,"total");
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(PREF_SEARCH_RESULTS,total)
                        .apply();

            }

            //move to the next tag
            eventType = parser.next();
        }

    }
}
