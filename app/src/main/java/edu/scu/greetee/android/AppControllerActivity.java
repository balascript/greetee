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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Weather;

public class AppControllerActivity extends AppCompatActivity {
    private TextView HiTemp,LowTemp,Description,Day;
    private ImageView WeatherIcon;
    private WeatherReciever receiver;

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(
                "edu.scu.weather.report");
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
        receiver= new WeatherReciever();
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,"test");
        IntentFilter intentFilter = new IntentFilter(
                "edu.scu.weather.report");
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

    public class WeatherReciever extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle weatherBundle= intent.getBundleExtra("weatherBundle");
            Weather weahter=(Weather) weatherBundle.getParcelable("weather");

            Constants.toastMessage(getApplicationContext(),"Weather name "+ weahter.getSummary());
            Log.d("Weather ",weahter.getSummary());
        }
    }

}
