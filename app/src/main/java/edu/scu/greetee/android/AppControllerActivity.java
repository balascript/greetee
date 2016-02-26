package edu.scu.greetee.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

public class AppControllerActivity extends AppCompatActivity {
    private TextView HiTemp,LowTemp,Description,Day;
    private ImageView WeatherIcon;
    private DataReciever receiver;

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Constants.SERVICE_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,intentFilter);

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
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_WEATHER);
        startService(GreeteeService);
    }

    // TODO as of now drawer is used. Planning to switch to menu.
    private void initUIandDrawer() {

        HiTemp= (TextView) findViewById(R.id.list_item_high_textview);
        LowTemp= (TextView) findViewById(R.id.list_item_low_textview);
        Description= (TextView) findViewById(R.id.list_item_forecast_textview);
        Day= (TextView) findViewById(R.id.list_item_date_textview);
        WeatherIcon=(ImageView)findViewById(R.id.list_item_icon);

    }

    public class DataReciever extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            int response= intent.getIntExtra(Constants.SERVICE_RESPONSE,0);
            Bundle bundleExtra= intent.getBundleExtra("data");
            switch (response){
                case Constants.SERVICE_RESPONSE_WEATHER:
                    Weather weahter=(Weather) bundleExtra.getParcelable("weather");
                    HiTemp.setText(weahter.getHiTemp()+"");
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
                    break;
                case Constants.USERAUTHERROR:
                    Intent errorIntent= (Intent) intent.getExtras().get("intent");
                    startActivityForResult(
                            errorIntent, Constants.REQUEST_ACCOUNT_PICKER);

            }


        }
    }

}
