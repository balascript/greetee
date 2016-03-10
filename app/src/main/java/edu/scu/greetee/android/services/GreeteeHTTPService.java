package edu.scu.greetee.android.services;

import android.Manifest;
import android.app.IntentService;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private Geocoder geocoder;

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
                Weather report = getWeatherForHome(sharedPreferences.getFloat(Constants.HomeLatitudeString,0),sharedPreferences.getFloat(Constants.HomeLongitudeString,0));
                bundle.putParcelable("weather", report);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_WEATHER);
                break;
            case Constants.SERVICE_REQUEST_EVENT:
                Event event = getNextEventForTheUserByProvider(sharedPreferences);
                bundle.putParcelable("event", event);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_EVENT);
                break;
            case Constants.SERVICE_REQUEST_EVENTS:
                ArrayList<Event> events = getEventsForTheUserByProvider(sharedPreferences);
                bundle.putParcelableArrayList("events", events);
                bIntent.putExtra("data", bundle);
                bIntent.putExtra(Constants.SERVICE_RESPONSE,Constants.SERVICE_RESPONSE_EVENTS);
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

    private ArrayList<Event> getEventsForTheUserByProvider(SharedPreferences sharedPreferences) {
        ArrayList<Event> list= new ArrayList<Event>();
        try {
            // Run query
            if(geocoder==null)
                geocoder=new Geocoder(getApplicationContext(), Locale.getDefault());
            list.add(getWorkEvent(sharedPreferences));
            Cursor cur = null;
            ContentResolver cr = getContentResolver();
            Uri uri = CalendarContract.Events.CONTENT_URI;
            String selection = "(" +
                    "( " + CalendarContract.Events.DTSTART + " >= ?)" +
                    " AND ( "+CalendarContract.Events.DTEND + " <= ?)" +
                    ")";
            String[] selectionArgs = new String[]{
                    System.currentTimeMillis() + ""
                    ,(System.currentTimeMillis()+(1000*60*60*24))+""
            };
            // Submit the query and get a Cursor object back.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            cur = cr.query(uri, Constants.EVENTS_PROJECTION, selection, selectionArgs, CalendarContract.Events.DTSTART + " ASC");
            while(cur.moveToNext()){
                String NameofEvent = cur.getString(Constants.PROJECTION_TITLE_INDEX),
                        eventLocation = cur.getString(Constants.PROJECTION_EVENT_LOCATION_INDEX);
                long start = cur.getLong(Constants.PROJECTION_BEGIN_INDEX);
                long end = cur.getLong(Constants.PROJECTION_END_INDEX);
                Event event = new Event(NameofEvent, eventLocation, null, new Date(start).getTime(), new Date(end).getTime());
                if(event!=null){
                    List<Address> loc= geocoder.getFromLocationName(event.getLocationString(),1);
                    if(!loc.isEmpty()){
                        event.setLocation(loc.get(0));
                        event.setWeather(getWeatherForHome(event.getLocation().getLatitude(),event.getLocation().getLongitude()));
                        event.setDirection(getDirectionFromHome(event.getLocation(),sharedPreferences));
                    }

                }
                list.add(event);
            }
            return list;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    private String getUserMessage(SharedPreferences pref) {
        Geocoder geocoder;
        geocoder=new Geocoder(getBaseContext(), Locale.getDefault());
        StringBuilder builder= new StringBuilder();
        Weather current= getWeatherForHome(pref.getFloat(Constants.HomeLatitudeString,0),pref.getFloat(Constants.HomeLongitudeString,0));
        if(current!=null){
            builder.append(" Its "+current.getTemperature()+"℉ and " +current.getSummary()+" outside.");
        }
        else{
            builder.append(" I am sorry, I could't get you the weather update");
        }
        Event nextEvent= getNextEventForTheUserByProvider(pref);
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

    private Weather getWeatherForHome(double lat, double lon) {

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

            weather.setHiTemp((int) temperatureObject.getDouble("temp_max"));
            weather.setLowTemp((int) temperatureObject.getDouble("temp_min"));
            weather.setTemperature((int) temperatureObject.getDouble("temp"));

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

    private Event getWorkEvent(SharedPreferences pref){

        Weather weather;
        Direction dir;
        Event workEvent;
        if(pref.getBoolean(Constants.isWorkSelectedString,false)){
            weather=getWeatherForHome(pref.getFloat(Constants.WorkLatitudeString,0),pref.getFloat(Constants.WorkLongitudeString,0));
            dir=getDirectionFromHomeToWork(pref);
            workEvent= new Event("###Work Place ",pref.getString(Constants.WorkLocationString,""),null,-1,-1);
            workEvent.setDirection(dir);
            workEvent.setWeather(weather);
        }
        else{
            workEvent= new Event("$$$Add Your Work Place",pref.getString(Constants.WorkLocationString,""),null,-1,-1);
        }

        return workEvent;
    }


    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private Event getNextEventForTheUserByProvider(SharedPreferences sharedPreferences) {
        try {
            // Run query
            if(geocoder==null)
                geocoder=new Geocoder(getApplicationContext(), Locale.getDefault());
            Cursor cur = null;
            ContentResolver cr = getContentResolver();
            Uri uri = CalendarContract.Events.CONTENT_URI;
            String selection = "(" +
                    "( " + CalendarContract.Events.DTSTART + " >= ?)" +
                    " AND ( "+CalendarContract.Events.DTEND + " <= ?)" +
                    ")";
            String[] selectionArgs = new String[]{
                    System.currentTimeMillis() + ""
                    ,(System.currentTimeMillis()+(1000*60*60*24))+""
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
                if(event!=null){
                    List<Address> loc= geocoder.getFromLocationName(event.getLocationString(),1);
                    if(!loc.isEmpty()){
                        event.setLocation(loc.get(0));
                        event.setWeather(getWeatherForHome(event.getLocation().getLatitude(),event.getLocation().getLongitude()));
                        event.setDirection(getDirectionFromHome(event.getLocation(),sharedPreferences));
                    }

                }
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
                    .appendQueryParameter("origin",origin.getLocality()) /// hardcoded by frustration. TODO figureout the map activity soon
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
                    String distance= legs.getJSONObject("distance").getString("text");
                    return new Direction((int) timeSec,distance);

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

    private Direction getDirectionFromHome( Address event,SharedPreferences preferences)
    {
        try{
            String origin= preferences.getString(Constants.HomeLocationString,"");
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            Uri builtUri = Uri.parse(Constants.Google_MAP_URI).buildUpon()
                    .appendQueryParameter("sensor","false").appendQueryParameter("units","imperial")
                    .appendQueryParameter("mode","driving")
                    .appendQueryParameter("origin",origin)
                    .appendQueryParameter("destination",event.getLocality())
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
                    String distance= legs.getJSONObject("distance").getString("text");

                    return new Direction((int) timeSec,distance);

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
    private Direction getDirectionFromHomeToWork(SharedPreferences preferences)
    {
        try{
            String origin= preferences.getString(Constants.HomeLocationString,"");
            String destination= preferences.getString(Constants.WorkLocationString,"");
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            Uri builtUri = Uri.parse(Constants.Google_MAP_URI).buildUpon()
                    .appendQueryParameter("sensor","false").appendQueryParameter("units","imperial")
                    .appendQueryParameter("mode","driving")
                    .appendQueryParameter("origin",origin)
                    .appendQueryParameter("destination",destination)
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
                    String distance= legs.getJSONObject("distance").getString("text");

                    return new Direction((int) timeSec,distance);

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

            weather.setHiTemp((int) temperatureObject.getDouble("temp_max"));
            weather.setLowTemp((int) temperatureObject.getDouble("temp_min"));
            weather.setTemperature((int) temperatureObject.getDouble("temp"));

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
