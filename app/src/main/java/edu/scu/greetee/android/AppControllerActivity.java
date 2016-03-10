package edu.scu.greetee.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Direction;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

public class AppControllerActivity extends AppCompatActivity {
    //Widgets for Just Weather
    private TextView HiTemp,LowTemp,Description,Day;
    private ImageView WeatherIcon;
    //widgets for Event
    private TextView EventName_Time,Event_Weather_Lcoation,Event_Distance, Event_ETA;
    private ImageView EventWeatherIcon;
    private Geocoder geocoder;
    //BC Receiver
    private DataReciever receiver;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.controllermenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsActivity= new Intent(this, SettingsActivity.class);
            startActivity(settingsActivity);
        }
        if (id == R.id.menu_uninstall) {
            Intent greetee= new Intent(this, GreeteeMainActivity.class);
            startActivity(greetee);

        /*    Uri packageURI = Uri.parse("package:"+this.getApplication().getPackageName());
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            startActivity(uninstallIntent);*/
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Constants.SERVICE_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,intentFilter);
        startWeatherIntent();
        startEventsIntent();
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
       // this.unregisterReceiver(receiver);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_forecast);
        initUIandDrawer();
        receiver= new DataReciever();

    }

    public void startWeatherIntent(){
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_WEATHER);
        Toast.makeText(this,"Updating.. Please wait!",Toast.LENGTH_SHORT);
        startService(GreeteeService);
    }
    public void startEventsIntent(){
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_EVENT);
        startService(GreeteeService);
    }


    private void initUIandDrawer() {
        geocoder=new Geocoder(getBaseContext(), Locale.getDefault());
        HiTemp= (TextView) findViewById(R.id.list_item_high_textview);
        LowTemp= (TextView) findViewById(R.id.list_item_low_textview);
        Description= (TextView) findViewById(R.id.list_item_forecast_textview);
        Day= (TextView) findViewById(R.id.list_item_date_textview);
        WeatherIcon=(ImageView)findViewById(R.id.list_item_icon);

        EventName_Time=(TextView) findViewById(R.id.list_item_event_start_textview);
        Event_Weather_Lcoation=(TextView) findViewById(R.id.list_item_weather_textview);
        Event_Distance=(TextView) findViewById(R.id.list_item_distance_text_view);
        Event_ETA=(TextView) findViewById(R.id.list_item_eta);
        EventWeatherIcon=(ImageView)findViewById(R.id.list_item_icon_work_weather);



    }

    public class DataReciever extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            int response= intent.getIntExtra(Constants.SERVICE_RESPONSE,0);
            Bundle bundleExtra= intent.getBundleExtra("data");
            switch (response){
                case Constants.SERVICE_RESPONSE_WEATHER:
                    Weather weahter=(Weather) bundleExtra.getParcelable("weather");
                    HiTemp.setText((int)weahter.getHiTemp()+"");
                    LowTemp.setText(weahter.getLowTemp()+"");
                    Day.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().getTime()));
                    WeatherIcon.setImageResource(weahter.getArt());
                    Description.setText(weahter.getSummary());
                    Constants.toastMessage(AppControllerActivity.this,"Weather name "+ weahter.getSummary());
                    Log.d("Weather ",weahter.getSummary());
                    break;
                case Constants.SERVICE_RESPONSE_EVENT:
                    Event event=(Event) bundleExtra.getParcelable("event");
                    Log.d("Event", event.getName());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                    EventName_Time.setText(event.getName()+" at "+ dateFormat.format(new Date(event.getStartDate())));
                    try {
                        List<Address> loc= geocoder.getFromLocationName(event.getLocationString(),1);
                        if(!loc.isEmpty()){
                            event.setLocation(loc.get(0));
                            updateEventAddressinUI(event);
                        }
                        else {
                            updateEventAddressinUI(event);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case  Constants.SERVICE_RESPONSE_WEATHER_EVENT:
                    Weather eventweather=(Weather) bundleExtra.getParcelable("weather");
                    if(eventweather==null) return;
                    EventWeatherIcon.setImageResource(Utility.getIconResourceForWeatherCondition(eventweather.getId()));
                    Event_Weather_Lcoation.setText("Its "+eventweather.getTemperature()+"\u2109 at "+Event_Weather_Lcoation.getText().toString());
                    break;
                case Constants.SERVICE_RESPONSE_DIRECTION:
                    Direction direction=(Direction)bundleExtra.getParcelable("direction");
                    if(direction==null) return;
                    int time[]= Utility.splitToComponentTimes(BigDecimal.valueOf(direction.getDuration()));
                    Event_ETA.setText((time[0]>0?(time[0]+"Hr"):"")+(time[1]>0?(time[1]+"M"):""));
                    break;

                case Constants.USERAUTHERROR:
                    Intent errorIntent= (Intent) intent.getExtras().get("intent");
                    startActivityForResult(
                            errorIntent, Constants.REQUEST_ACCOUNT_PICKER);

            }


        }
    }

    private void updateEventAddressinUI(Event event) {
        Event_Weather_Lcoation.setVisibility(View.VISIBLE);
        if(event.getLocation()==null){
            Event_Weather_Lcoation.setText(event.getLocationString() +"(Weather undetermined)");
            return;
        }

        Event_Weather_Lcoation.setText(event.getLocation().getLocality()==null?event.getLocationString():event.getLocation().getLocality());
        updateWeatherForEvent(event);
    }

    private void updateWeatherForEvent(Event event) {
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_WEATHER_EVENT);
        GreeteeService.putExtra(Constants.SERVICE_REQUEST_WEATHER_EVENT_ADDRESS,event.getLocation());
        startService(GreeteeService);
        updateDirections(event);

    }


    private void updateDirections(Event event) {
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_DIRECTION);
        GreeteeService.putExtra(Constants.SERVICE_REQUEST_DIRECTION_DESTINATION,event.getLocation());
        Address dest= new Address(Locale.getDefault());
        dest.setLatitude(Constants.SOURCE_LOCATION[0]);
        dest.setLongitude(Constants.SOURCE_LOCATION[1]);
        GreeteeService.putExtra(Constants.SERVICE_REQUEST_DIRECTION_ORIGIN,dest);
        startService(GreeteeService);
    }
}
