package edu.scu.greetee.android.services;

import android.Manifest;
import android.app.IntentService;
import android.app.usage.UsageEvents;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Direction;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

/**
 * Created by srbkr on 2/25/2016.
 */
public class GreeteeHTTPService extends IntentService {
    RequestQueue appQueue;

    public GreeteeHTTPService(String name) {
        super(name);
        appQueue = Volley.newRequestQueue(this.getBaseContext());
    }

    public GreeteeHTTPService() {
        super("Greetee service");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if (appQueue == null) {
            appQueue = Volley.newRequestQueue(this.getBaseContext());
        }

        int operation = intent.getIntExtra(Constants.STRING_PARAM_OPERATION, 0);
        Bundle bundle = new Bundle();
        Intent bIntent = new Intent(Constants.SERVICE_INTENT);
        switch (operation) {
            case Constants.SERVICE_REQUEST_ALERT:
                String userMessage= getUserMessage(sharedPreferences);
                bIntent.putExtra(Constants.SERVICE_REQUEST_ALERT_STRING,userMessage);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_ALERT );
                break;
            case Constants.SERVICE_REQUEST_WEATHER:
                Weather report = getWeather(sharedPreferences.getFloat(Constants.HomeLatitudeString,0),sharedPreferences.getFloat(Constants.HomeLongitudeString,0));
                bundle.putParcelable("weather", report);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_WEATHER);
                break;
            case Constants.SERVICE_REQUEST_EVENT:
                Event event = getNextEventForTheUserByProvider();
                bundle.putParcelable("event", event);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_EVENT);
                break;
            case Constants.SERVICE_REQUEST_WEATHER_EVENT:
                Address query= intent.getExtras().getParcelable(Constants.SERVICE_REQUEST_WEATHER_EVENT_ADDRESS);
                Weather reportEvent= getWeatherForAddress(query);
                bundle.putParcelable("weather", reportEvent);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_WEATHER_EVENT);
                break;
            case Constants.SERVICE_REQUEST_DIRECTION:
                Address origin= intent.getExtras().getParcelable(Constants.SERVICE_REQUEST_DIRECTION_ORIGIN);
                Address destination= intent.getExtras().getParcelable(Constants.SERVICE_REQUEST_DIRECTION_DESTINATION);
                Direction result= getDirection(origin,destination);
                bundle.putParcelable("direction", result);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_DIRECTION);
                break;

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(bIntent);


    }

    private String getUserMessage(SharedPreferences pref) {
        Geocoder geocoder;
        geocoder=new Geocoder(getBaseContext(), Locale.getDefault());
        StringBuilder builder= new StringBuilder();
        Weather current=getWeather(pref.getFloat(Constants.HomeLatitudeString,0),pref.getFloat(Constants.HomeLongitudeString,0));
        if(current!=null){
            builder.append(" Its "+current.getTemperature()+"℉ and " +current.getSummary()+" outside.");
        }
        else{
            builder.append(" I am sorry, I could't get you the weather update");
        }
        Event nextEvent= getNextEventForTheUserByProvider();
        if(nextEvent==null){
            builder.append(" I couldn't find any event from your calendar in next 24 hours ! Have a fun Day :)");
        }
        else {
            Address source = null;
            long diff = nextEvent.getStartDate() - java.util.Calendar.getInstance().getTimeInMillis();
            if (diff <= (1000 * 60 * 60 * 24)) {          // find if there's any event for next 3 hours // time being //TODO its 24 hr set now
                try {


                    List<Address> destlist = geocoder.getFromLocationName(nextEvent.getLocationString(), 1);
                    if (!destlist.isEmpty()) {
                        nextEvent.setLocation(destlist.get(0));
                    }

                    List<Address> sourcelist = geocoder.getFromLocation(Constants.SOURCE_LOCATION[0], Constants.SOURCE_LOCATION[1], 1);
                    if (!sourcelist.isEmpty()) {
                        source = (sourcelist.get(0));
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                    String logical_location=nextEvent.getLocation().getAddressLine(0);
                    builder.append(" I found '"+nextEvent.getName()+"' at "+(logical_location==null?nextEvent.getLocationString():logical_location) +" at "+dateFormat.format(new Date(nextEvent.getStartDate())) );

                    Weather eventWeather = getWeatherForAddress(nextEvent.getLocation());
                    Direction eventDirection = getDirection(source, nextEvent.getLocation());

                    if(eventWeather!=null){
                        builder.append("\n\n Its "+eventWeather.getTemperature()+"℉ and " +eventWeather.getSummary()+" at "+(logical_location==null?nextEvent.getLocationString():logical_location)+".");

                    }
                    if(eventDirection!=null){
                        int time[]= Utility.splitToComponentTimes(BigDecimal.valueOf(eventDirection.getDuration())); // TODO find miles and add them too
                        builder.append(" Hey ! you can reach there in " + (time[0]>0?(time[0]+" Hours "):"")+(time[1]>0?(time[1]+" Minutes."):"."));
                    }




                } catch (IOException e) {

                    e.printStackTrace();
                    return " Error getting you the updates";
                }
            }
            else {  // no event in next 3 hours
                builder.append(" I couldn't find any event from your calendar in next 3 hours ! Have fun! Good Bye");
            }

        }


        return builder.toString();

    }

    private Weather getWeather(double lat, double lon) {

        try {
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            //BUild the URL
            String format = "json";
            String units = "imperial";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String APPID_PARAM = "APPID";
            final String LAT_PARAM = "lat";
            final String LON_PARAM = "lon";
            Uri builtUri = Uri.parse(Constants.OPEN_MAP_API_URL).buildUpon()
                    .appendQueryParameter(LAT_PARAM, String.valueOf(lat))
                    .appendQueryParameter(LON_PARAM, String.valueOf(lon))
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(APPID_PARAM, Constants.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());


            JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, future, future);
            appQueue.add(request);

            JSONObject response = future.get(100, TimeUnit.SECONDS); // this will block (forever) if supplied nothing

            Weather weather = new Weather();

            JSONObject weatherJson = response.getJSONArray("weather").getJSONObject(0);

            weather.setSummary(weatherJson.getString("main"));
            weather.setId(weatherJson.getInt("id"));
            weather.setArt(Utility.getArtResourceForWeatherCondition(weather.getId()));
            weather.setIcon(Utility.getIconResourceForWeatherCondition(weather.getId()));

            JSONObject temperatureObject = response.getJSONObject("main");

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



/*    private Event getNextEventForTheUser() {
        com.google.api.services.calendar.Calendar mService = null;
        Exception mLastError = null;
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(Constants.SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(Constants.PREF_ACCOUNT_NAME, null));
        if (!isDeviceOnline()) {
            return null;
        }
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(this.getPackageName())
                .build();

        DateTime now = new DateTime(System.currentTimeMillis());
        java.util.Calendar todayEOD = java.util.Calendar.getInstance();
        todayEOD.set(todayEOD.YEAR, todayEOD.MONTH, todayEOD.DAY_OF_MONTH, 23, 59, 59);
        Events events = null;
        try {
            events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .setTimeMax(new DateTime(System.currentTimeMillis() + (1000 * 60 * 60 * 24)))
                    .execute();
            List<com.google.api.services.calendar.model.Event> items = events.getItems();
           *//* for (com.google.api.services.calendar.model.Event event : items) {

                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }


            }*//*
            com.google.api.services.calendar.model.Event event = items.get(0);
            if (event == null)
                return null;
            DateTime startTime = event.getStart().getDateTime() == null ? event.getStart().getDate() : event.getStart().getDateTime();
            DateTime endTime = event.getEnd().getDateTime() == null ? event.getEnd().getDate() : event.getEnd().getDateTime();
            Event calendarEvent = new Event(event.getSummary(), event.getLocation(), null, startTime.getValue(), endTime.getValue());
            return calendarEvent;
        } catch (UserRecoverableAuthIOException e) {
            Intent errorIntent = new Intent(Constants.SERVICE_INTENT);
            errorIntent.putExtra(Constants.SERVICE_RESPONSE, Constants.USERAUTHERROR);
            errorIntent.putExtra("intent", e.getIntent());
            LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }*/

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private Event getNextEventForTheUserByProvider() {
        try {
            // Run query
            Cursor cur = null;
            ContentResolver cr = getContentResolver();
            Uri uri = CalendarContract.Events.CONTENT_URI;
            String selection = "(" +
                    "( " + CalendarContract.Events.DTSTART + " >= ?)" +
                    //" AND ( "+CalendarContract.Events.DTEND + " <= ?)" +
                    ")";
            java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            String[] selectionArgs = new String[]{
                    System.currentTimeMillis() + ""
                    // ,(System.currentTimeMillis()+(1000*60*60*24))+""
            };
            // Submit the query and get a Cursor object back.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            cur = cr.query(uri, Constants.EVENTS_PROJECTION, selection, selectionArgs, CalendarContract.Events.DTSTART + " ASC");
            if (cur.moveToFirst()) {
                String NameofEvent = cur.getString(Constants.PROJECTION_TITLE_INDEX),
                        eventLocation = cur.getString(Constants.PROJECTION_EVENT_LOCATION_INDEX);
                long start = cur.getLong(Constants.PROJECTION_BEGIN_INDEX);
                long end = cur.getLong(Constants.PROJECTION_END_INDEX);
                Event event = new Event(NameofEvent, eventLocation, null, new Date(start).getTime(), new Date(end).getTime());
                return event;
            }
            else return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    private Direction getDirection( Address origin, Address destination)
    {
        try{
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            Uri builtUri = Uri.parse(Constants.Google_MAP_URI).buildUpon()
                    .appendQueryParameter("sensor","false").appendQueryParameter("units","imperial")
                    .appendQueryParameter("mode","driving")
                    .appendQueryParameter("origin","santaclara") /// hardcoded by frustration. TODO figureout the map activity soon
                    .appendQueryParameter("destination",destination.getLocality())
                    .appendQueryParameter("key",Constants.GOOGLE_API_KEY)
                    .build();
            URL url = new URL(builtUri.toString());


            JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, future, future);
            appQueue.add(request);

            JSONObject response = future.get(100, TimeUnit.SECONDS); // this will block (forever) if supplied nothing

            JSONArray routesArray = response.getJSONArray("routes");

            if (routesArray.length() > 0) {
                JSONObject routeDict = routesArray.getJSONObject(0);
                JSONArray legsArray = routeDict.getJSONArray("legs");

                if (legsArray.length() > 0) {

                    JSONObject legs;

                    if (legsArray.length() > 1)
                        legs = legsArray.getJSONObject(1);
                    else
                        legs = legsArray.getJSONObject(0);

                    double timeSec = legs.getJSONObject("duration").getDouble("value");

                    return new Direction((int) timeSec,10);

                }
            }
            return null;


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
    private Weather getWeatherForAddress(Address location) {

        try {
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            //BUild the URL
            String format = "json";
            String units = "imperial";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String APPID_PARAM = "APPID";
            final String LAT_PARAM = "lat";
            final String LON_PARAM = "lon";
            Uri builtUri = Uri.parse(Constants.OPEN_MAP_API_URL).buildUpon()
                    .appendQueryParameter(LAT_PARAM, String.valueOf(location.getLatitude()))
                    .appendQueryParameter(LON_PARAM, String.valueOf(location.getLongitude()))
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(APPID_PARAM, Constants.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());


            JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, future, future);
            appQueue.add(request);

            JSONObject response = future.get(100, TimeUnit.SECONDS); // this will block (forever) if supplied nothing

            Weather weather = new Weather();

            JSONObject weatherJson = response.getJSONArray("weather").getJSONObject(0);

            weather.setSummary(weatherJson.getString("main"));
            weather.setId(weatherJson.getInt("id"));
            weather.setArt(Utility.getArtResourceForWeatherCondition(weather.getId()));
            weather.setIcon(Utility.getIconResourceForWeatherCondition(weather.getId()));

            JSONObject temperatureObject = response.getJSONObject("main");

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

}
