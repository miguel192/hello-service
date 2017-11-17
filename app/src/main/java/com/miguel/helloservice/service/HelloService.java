package com.miguel.helloservice.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HelloService extends Service {

    private static final String TAG = "HelloService";

    final int id = 1;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;


    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        Log.i(TAG, "Service onStartCommand");

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {

                DownloaderTask downloaderTask = new DownloaderTask();
                downloaderTask.execute();

                //Stop service once it finishes its task
                stopSelf();
            }
        }).start();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "Service onDestroy");
    }

    private class DownloaderTask extends AsyncTask<URL, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.i("DEBUG", "Executing pre execute");

            mBuilder.setContentTitle("Picture Download")
                    .setContentText("Download in progress")
                    .setSmallIcon(R.drawable.ic_notification)
                    .build();

            mBuilder.setProgress(100, 0, false);
            // Displays the progress bar for the first time.
            mNotifyManager.notify(id, mBuilder.build());
            // When the loop is finished, updates the notification
        }

        @Override
        protected String doInBackground(URL... params) {
            Log.i("DEBUG", "Doing background work");

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            //File myDir = getFilesDir();

            String imageURL = "https://www.hdwallpapers.in/walls/green_beach_big_island-normal.jpg";

            try {
                URL url = new URL(imageURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d("DEBUG", "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                //output = new FileOutputStream(myDir + "/AppPhotoLesson/MyDownloadedPhotoIsHere.jpg");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {

                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;

                    // publishing the progress....
                    if (fileLength > 0) {// only if total length is known

                        mBuilder.setProgress(100, (int) (total * 100 / fileLength), false);
                        mNotifyManager.notify(id, mBuilder.build());
                        Thread.sleep(50);
                    }
                    //output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("DEBUG", "Executing post execute");

            mBuilder.setContentText("Download complete")
                    // Removes the progress bar
                    .setProgress(0,0,false);
            mNotifyManager.notify(id, mBuilder.build());
        }
    }
}