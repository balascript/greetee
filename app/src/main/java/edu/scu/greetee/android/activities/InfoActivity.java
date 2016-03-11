package edu.scu.greetee.android.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import edu.scu.greetee.android.R;
import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

public class InfoActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener{
    private TextView HiTemp,LowTemp,Description,Day;
    private ImageView WeatherIcon;
    //widgets for EventL
    private TextView EventName_Time,Event_Weather_Lcoation,Event_Distance, Event_ETA;
    private ImageView EventWeatherIcon,MuteIcon,AlertBg;
    private View EventL, WeatherL,WaitMsg;
    ImageView Mute;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
    //BC Receiver
    private DataReciever receiver;
    private TextToSpeech speaker;
    private boolean speakEnabled=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_forecast);
        Intent intent = getIntent();
        if(intent.getType() != null && intent.getType().equals("application/" + getPackageName())) {
            // Read the first record which contains the NFC data
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefRecord relayRecord = ((NdefMessage)rawMsgs[0]).getRecords()[0];
            String nfcData = new String(relayRecord.getPayload());
          //  Toast.makeText(this, nfcData, Toast.LENGTH_SHORT).show();
        }
        receiver= new DataReciever();

        initUIandDrawer();


        speaker= new TextToSpeech(this,this);

    }
    private void initUIandDrawer() {
        WeatherL =findViewById(R.id.weatheralertlayout);
        WaitMsg=findViewById(R.id.waitmsg);
        EventL =findViewById(R.id.eventupdateAlertLayout);
        HiTemp= (TextView) findViewById(R.id.list_item_high_textview);
        LowTemp= (TextView) findViewById(R.id.list_item_low_textview);
        Description= (TextView) findViewById(R.id.list_item_forecast_textview);
        Day= (TextView) findViewById(R.id.list_item_date_textview);
        WeatherIcon=(ImageView)findViewById(R.id.list_item_icon);
        AlertBg=(ImageView)findViewById(R.id.alert_bg);
        EventName_Time=(TextView) findViewById(R.id.list_item_event_start_textview);
        Event_Weather_Lcoation=(TextView) findViewById(R.id.list_item_weather_textview);
        Event_Distance=(TextView) findViewById(R.id.list_item_distance_text_view);
        Event_ETA=(TextView) findViewById(R.id.list_item_eta);
        EventWeatherIcon=(ImageView)findViewById(R.id.list_item_icon_work_weather);
    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Constants.SERVICE_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,intentFilter);
        startAlertService();
    }

    private void startAlertService() {
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_ALERT);
        Toast.makeText(this,"Updating.. Please wait!",Toast.LENGTH_SHORT);
        startService(GreeteeService);
    }
    private void changeBg(final int resId){
        Picasso.with(this).load(resId).into(AlertBg);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = speaker.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                this.speakEnabled=true;
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }


    public class DataReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            int response= intent.getIntExtra(Constants.SERVICE_RESPONSE,0);
            Bundle bundleExtra= intent.getBundleExtra("data");
            switch (response){
                case Constants.SERVICE_RESPONSE_ALERT:
                    Event event = bundleExtra.getParcelable("event");
                    Weather weather=bundleExtra.getParcelable("weather");
                    String UserMessage= intent.getStringExtra(Constants.SERVICE_REQUEST_ALERT_STRING);
                    UpdateUI(event,weather);
                    speakOut(UserMessage);
                    break;

            }


        }
    }

    private void UpdateUI(Event event, Weather weather) {
        if(weather!=null) {
            WaitMsg.setVisibility(View.GONE);
            WeatherL.setVisibility(View.VISIBLE);
            changeBg(Utility.getAlertBgResourceForWeatherCondition(weather.getId()));
            HiTemp.setText( weather.getTemperature() + "℉");
            LowTemp.setText(weather.getLowTemp() + "℉");
            Day.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().getTime()));
            WeatherIcon.setImageResource(weather.getArt());
            Description.setText(weather.getSummary());
        }
        if(event!=null){
            EventL.setVisibility(View.VISIBLE);
            if(event.getName().startsWith("$$$")){
                EventName_Time.setText("Work place not added to Greetee!");
            }
            else if(event.getName().startsWith("###")){
                EventName_Time.setText("Work ");
            }
            else {
                EventName_Time.setText(dateFormat.format(new Date(event.getStartDate())) + " - " + event.getName());
            }
            String eventweather_update = "";
            if (event.getLocation() != null) {
                eventweather_update += event.getLocation().getLocality() == null ? event.getLocationString() : event.getLocation().getLocality();
            } else
                eventweather_update += event.getLocationString();
            if (event.getWeather() != null) {
                eventweather_update += " (" + event.getWeather().getTemperature() + "℉ )";
                EventWeatherIcon.setImageResource(Utility.getIconResourceForWeatherCondition(event.getWeather().getId()));
            } else
                eventweather_update += " (Weather undetermined)";

            if (event.getDirection() != null) {
                //Event_Weather_Lcoation,Event_Distance, Event_ETA;
                Event_Distance.setText(event.getDirection().getDistance());
                int time[] = Utility.splitToComponentTimes(BigDecimal.valueOf(event.getDirection().getDuration()));
                Event_ETA.setText((time[0] > 0 ? (time[0] + "Hr") : "") + (time[1] > 0 ? (time[1] + "M") : ""));
                Event_Distance.setVisibility(View.VISIBLE);
                Event_ETA.setVisibility(View.VISIBLE);
            }
            Event_Weather_Lcoation.setText(eventweather_update);
        }
    }

    private void speakOut(String msg) {
        //TODO improve speech
        if(!this.speakEnabled) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speaker.speak(msg, TextToSpeech.QUEUE_FLUSH,null, null);
        }
        else
        {
            speaker.speak(msg, TextToSpeech.QUEUE_FLUSH,null);
        }

    }
    @Override
    protected void onPause() {
        super.onPause();
        // Don't forget to shutdown tts!
        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
