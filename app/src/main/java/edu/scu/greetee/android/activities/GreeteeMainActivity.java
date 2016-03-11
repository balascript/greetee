package edu.scu.greetee.android.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.scu.greetee.android.adapters.DividerItemDecoration;
import edu.scu.greetee.android.adapters.EventRecyclerAdapter;
import edu.scu.greetee.android.R;
import edu.scu.greetee.android.adapters.RecyclerItemClickListener;
import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.adapters.WrappingLinearLayoutManager;
import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

public class GreeteeMainActivity extends AppCompatActivity  implements AppBarLayout.OnOffsetChangedListener{
   /*####------*/
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR  = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS     = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION              = 200;

    private boolean mIsTheTitleVisible          = false;
    private boolean mIsTheTitleContainerVisible = true;

    private LinearLayout mTitleContainer;
    private TextView mTitle;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    /*------*/
    //Widgets for Just Weather
    private TextView HiTemp,LowTemp,Description,Day;
    private ImageView WeatherIcon,WeatherBG;
    //BC Receiver
    private DataReciever receiver;
    private View welcomeMsg,weatherInfo;

    //Listview operations

    private RecyclerView EventsListView;
    private EventRecyclerAdapter adapter;
    private List<Event> events;

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
           Uri packageURI = Uri.parse("package:"+this.getApplication().getPackageName());
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            startActivity(uninstallIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
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

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greetee_main);

        bindActivity();

        mToolbar.setTitle("");
        mAppBarLayout.addOnOffsetChangedListener(this);

        setSupportActionBar(mToolbar);
        startAlphaAnimation(mTitle, 0, View.INVISIBLE);
        initUI();
        receiver= new DataReciever();
        startWeatherIntent();
        startEventsIntent();
        events=new ArrayList<Event>();
        adapter= new EventRecyclerAdapter(events,this);
        EventsListView=(RecyclerView)findViewById(R.id.events_list_view);
        EventsListView.setLayoutManager(new WrappingLinearLayoutManager(this));
        EventsListView.setAdapter(adapter);
        EventsListView.setHasFixedSize(true);
        EventsListView.setNestedScrollingEnabled(false);
        EventsListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        EventsListView.setAdapter(adapter);
        EventsListView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                       if(position==0&& !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.isWorkSelectedString,false)){
                           Intent settingsActivity= new Intent(GreeteeMainActivity.this, SettingsActivity.class);
                           startActivity(settingsActivity);

                       }
                    }
                })
        );



    }

    private void startEventsIntent() {
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_EVENTS);
        startService(GreeteeService);
    }

    public void startWeatherIntent(){
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_WEATHER);
        startService(GreeteeService);
    }
    private void initUI() {
        welcomeMsg=findViewById(R.id.welcome_message);
        weatherInfo=findViewById(R.id.weather_info_layout);
        HiTemp= (TextView) findViewById(R.id.list_item_high_textview);
        LowTemp= (TextView) findViewById(R.id.list_item_low_textview);
        Description= (TextView) findViewById(R.id.list_item_forecast_textview);
        Day= (TextView) findViewById(R.id.list_item_date_textview);
        WeatherIcon=(ImageView)findViewById(R.id.list_item_icon);
        WeatherBG=(ImageView)findViewById(R.id.main_weather_bg);
    }

    private void bindActivity() {
        mToolbar        = (Toolbar) findViewById(R.id.main_toolbar);
        mTitle          = (TextView) findViewById(R.id.main_textview_title);
        mTitleContainer = (LinearLayout) findViewById(R.id.main_linearlayout_title);
        mAppBarLayout   = (AppBarLayout) findViewById(R.id.main_appbar);
    }



    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if(!mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
                mToolbar.setBackground(getResources().getDrawable(R.drawable.today_touch_selector));
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mToolbar.setBackground(getResources().getDrawable(R.drawable.today_touch_selector_transparent));
                mIsTheTitleVisible = false;

            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if(mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;

            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(mTitleContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;

            }
        }
    }

    public static void startAlphaAnimation (View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    public class DataReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int response= intent.getIntExtra(Constants.SERVICE_RESPONSE,0);
            Bundle bundleExtra= intent.getBundleExtra("data");
            switch (response){
                case Constants.SERVICE_RESPONSE_WEATHER:
                    Weather weahter=(Weather) bundleExtra.getParcelable("weather");
                    weatherInfo.setVisibility(View.VISIBLE);
                    welcomeMsg.setVisibility(View.GONE);
                    HiTemp.setText((int)weahter.getTemperature()+" ℉");
                    LowTemp.setText(weahter.getLowTemp()+" ℉");
                    Day.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().getTime()));
                    WeatherIcon.setImageResource(weahter.getArt());
                    Picasso.with(GreeteeMainActivity.this).load(Utility.getBgResourceForWeatherCondition(weahter.getId())).into(WeatherBG);
                    Description.setText(weahter.getSummary());
                    break;
                case Constants.SERVICE_RESPONSE_EVENTS:
                    ArrayList<Event> eventsFromService= bundleExtra.getParcelableArrayList("events");
                    events.clear();
                    events.addAll(eventsFromService);
                    adapter.notifyDataSetChanged();
                    break;
            }


        }
    }
}
