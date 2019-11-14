package com.danielogbuti.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.sip.SipSession;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ogbuti.photogallery";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int BITMAP_DOWNLOAD = 1;

    Handler handler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
    Handler responseHandler;
    Listener<Token> listener;
    int cacheSize = 4 * 1024 * 1024;
    final LruCache<String, Bitmap> bitmapCache = new LruCache<>(cacheSize);


    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listener) {
        this.listener = listener;
    }


    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        this.responseHandler = responseHandler;
    }

    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got a url " + url);
        requestMap.put(token, url);

        handler
                //build the message
                .obtainMessage(MESSAGE_DOWNLOAD, token)
                //send to target handler
                .sendToTarget();
    }

    public void saveBitmap(String url) {
        handler
                .obtainMessage(BITMAP_DOWNLOAD, url)
                .sendToTarget();
    }

    //use suppress lint annotstion to tell android studio to ignore error
    //caused by putting handleMessage override method in another method
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        //initiate the target handler class
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //get the value of"what" for the message obtained
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    //get the value of "obj" passed and put it in a variable
                            Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for url " + requestMap.get(token));
                    //call the method that handles the creation of the image
                    handleRequest(token);
                }
                if (msg.what == BITMAP_DOWNLOAD) {
                    String url =(String) msg.obj;
                    handleCache(url);
                }


            }

        };
    }




    private void handleRequest(final Token token){
        final Bitmap bitmap;
        try {
            final String url = requestMap.get(token);

            if (url == null){return;}
            synchronized (bitmapCache){
                if (bitmapCache.get(url) == null){
                    //get the image bytes
                    byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
                    //turn it into a bitmap
                    bitmap= BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
                    Log.i(TAG,"BitmapCreated");
                    bitmapCache.put(url,bitmap);
                    Log.e(TAG,"The url is not from cache"+bitmapCache.get(url));
                }else {
                     bitmap = bitmapCache.get(url);
                     Log.i(TAG,"The bitmap is from cache");
                }

            }
            //set the messgase that works with the UI thread
            responseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url)
                        return;

                    requestMap.remove(token);
                    listener.onThumbnailDownloaded(token,bitmap);
                }

            });

        }catch (IOException e){
            Log.e(TAG,"Error downloading image");
        }
    }

    private void handleCache(final String url){
            final Bitmap bitmap;
            if (url == null) {
                return;
            }
            try {
                synchronized (bitmapCache) {
                    if (bitmapCache.get(url) == null) {
                        //get the image bytes
                        byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
                        //turn it into a bitmap
                        bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                        Log.i(TAG, "BitmapCreated");
                        //put the bitmap into the cache
                        bitmapCache.put(url, bitmap);
                        Log.d(TAG,"Bitmap has been saved into cache");

                    }
                }
            } catch (IOException io) {
                Log.e(TAG, "Error occured in cache");
            }
    }


    public void clearQueue(){
        handler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}
