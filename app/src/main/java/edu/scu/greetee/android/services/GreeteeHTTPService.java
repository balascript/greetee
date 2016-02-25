package edu.scu.greetee.android.services;

import android.app.IntentService;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.Events;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

/**
 * Created by srbkr on 2/25/2016.
 */
public class GreeteeHTTPService extends IntentService {
    RequestQueue appQueue;
    public GreeteeHTTPService(String name) {
        super(name);
        appQueue= Volley.newRequestQueue(this.getBaseContext());
    }

    public GreeteeHTTPService() {
        super("Greetee service");

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if(appQueue==null){
            appQueue= Volley.newRequestQueue(this.getBaseContext());
        }

        String operation= intent.getStringExtra(Constants.STRING_PARAM_OPERATION);
        if(operation.equalsIgnoreCase("test")){
            Bundle bundle= new Bundle();
            Weather report= getWeather(Constants.DEFAULT_LOCATION_ZIP+"");
            Intent bIntent= new Intent("edu.scu.weather.report");
            bundle.putParcelable("weather",report);
            bIntent.putExtra("weatherBundle",bundle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(bIntent);
        }
    }

    private Weather getWeather(String Location){

        try {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        //BUild the URL
        String format = "json";
        String units = "imperial";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String APPID_PARAM = "APPID";
        final String QUERY_PARAM = "zip";
        Uri builtUri = Uri.parse(Constants.OPEN_MAP_API_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, Location)
                .appendQueryParameter(FORMAT_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(APPID_PARAM, Constants.OPEN_WEATHER_MAP_API_KEY)
                .build();

        URL url = new URL(builtUri.toString());



        JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, future, future);
        appQueue.add(request);

            JSONObject response = future.get(100, TimeUnit.SECONDS); // this will block (forever) if supplied nothing

            Weather weather= new Weather();

            JSONObject weatherJson= response.getJSONArray("weather").getJSONObject(0);

            weather.setSummary(weatherJson.getString("main"));
            weather.setId(weatherJson.getInt("id"));
            weather.setArt(Utility.getArtResourceForWeatherCondition(weather.getId()));
            weather.setIcon(Utility.getIconResourceForWeatherCondition(weather.getId()));

            JSONObject temperatureObject= response.getJSONObject("main");

            weather.setHiTemp(temperatureObject.getDouble("temp_max"));
            weather.setLowTemp(temperatureObject.getDouble("temp_min"));
            weather.setTemperature(temperatureObject.getDouble("temp"));

            return weather;




        } catch (InterruptedException e) {
            e.printStackTrace();
           return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (TimeoutException e) {
            e.printStackTrace();
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }


    private Event getNextEventForTheUser(){
       com.google.api.services.calendar.Calendar mService = null;
        Exception mLastError = null;
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(Constants.SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(Constants.PREF_ACCOUNT_NAME, null));
        if(!isDeviceOnline()){
            return null;
        }
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Google Calendar API Android Quickstart")
                .build();

        DateTime now = new DateTime(System.currentTimeMillis());
        java.util.Calendar todayEOD= java.util.Calendar.getInstance();
        todayEOD.set(todayEOD.YEAR,todayEOD.MONTH,todayEOD.DAY_OF_MONTH,23,59,59);
        Events events = null;
        try {
            events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMax(new DateTime(System.currentTimeMillis()+(1000*60*60*24)))
                    .execute();
            List<com.google.api.services.calendar.model.Event> items = events.getItems();
           /* for (com.google.api.services.calendar.model.Event event : items) {

                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }


            }*/
            com.google.api.services.calendar.model.Event event=items.get(0);
            if(event==null)
                return null;
            DateTime startTime=event.getStart().getDateTime()==null?event.getStart().getDate():event.getStart().getDateTime();
            DateTime endTime=event.getEnd().getDateTime()==null?event.getEnd().getDate():event.getEnd().getDateTime();
            Event calendarEvent=new Event(event.getSummary(),event.getLocation(),null,startTime,endTime);
            return calendarEvent;
            } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

}
