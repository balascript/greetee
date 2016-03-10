package edu.scu.greetee.android;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import edu.scu.greetee.android.model.Event;

/**
 * Created by srbkr on 2/5/2016.
 */
public class EventsListAdapter extends BaseAdapter {
    List<Event> events;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
    private Activity activity;
    private LayoutInflater inflater;
    public EventsListAdapter(List<Event> ens, Activity act) {
        activity=act;
        events = ens;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View RowItemView, ViewGroup parent) {
        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RowItemView = inflater.inflate(R.layout.event_single, null);
        ImageView icon= (ImageView) RowItemView.findViewById(R.id.list_item_icon_event_weather);
        TextView eventStart=(TextView)RowItemView.findViewById(R.id.list_item_event_start_textview);
        TextView eventWeather=(TextView)RowItemView.findViewById(R.id.list_item_weather_textview);
        TextView eventDistance=(TextView)RowItemView.findViewById(R.id.list_item_distance_text_view);
        TextView eventETA=(TextView)RowItemView.findViewById(R.id.list_item_eta);
        Event event= events.get(position);
        eventStart.setText( dateFormat.format(new Date(event.getStartDate()))+" - "+event.getName());
        String eventweather_update="";
        if(event.getLocation()!=null){
            eventweather_update+=event.getLocation().getLocality()==null?event.getLocationString():event.getLocation().getLocality();
        }
        else
         eventweather_update+=event.getLocationString();
        if(event.getWeather()!=null){
            eventweather_update+=" ("+ event.getWeather().getTemperature()+"℉ )";
            icon.setImageResource(Utility.getIconResourceForWeatherCondition(event.getWeather().getId()));
        }
        else
            eventweather_update+=" (Weather undetermined)";

        if(event.getDirection()!=null){
            eventDistance.setText(event.getDirection().getDistance());
            int time[]= Utility.splitToComponentTimes(BigDecimal.valueOf(event.getDirection().getDuration()));
            eventETA.setText((time[0]>0?(time[0]+"Hr"):"")+(time[1]>0?(time[1]+"M"):""));
            eventDistance.setVisibility(View.VISIBLE);
            eventETA.setVisibility(View.VISIBLE);
        }
        eventWeather.setText(eventweather_update);
        return RowItemView;

    }


}
