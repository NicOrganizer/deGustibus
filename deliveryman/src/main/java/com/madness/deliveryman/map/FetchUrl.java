package com.madness.deliveryman.map;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


class FetchUrl extends AsyncTask<String, Void, String> implements PointsParser.AsyncResponse {
    public AsyncFetchResponse delegate = null;
    GoogleMap map;

    //things to do when PointParser finish
    @Override
    public void processFinish(String distance, String duration, String distanceInt) {
        //call the method processFinish of MapFragment to pass distance and duration
        delegate.processFetchFinish(distance, duration, distanceInt);

    }

    @Override
    protected String doInBackground(String... strings) {
        // For storing data from web service
        String data = "";
        try {
            // Fetching the data from web service
            data = downloadUrl(strings[0]);
        } catch (Exception e) {

        }
        return data;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        PointsParser parserTask = new PointsParser();
        //set the delegate of the parsertask as this (FetchUrl)
        parserTask.delegate = this;
        parserTask.setMap(map);
        // Invokes the thread for parsing the JSON data
        parserTask.execute(s);

    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    void setMap(GoogleMap map) {
        this.map = map;
    }

    //interface to communicate with MapFragment
    public interface AsyncFetchResponse {
        void processFetchFinish(String distance, String duration, String distanceInt);
    }
}
