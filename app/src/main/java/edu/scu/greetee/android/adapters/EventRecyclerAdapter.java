package edu.scu.greetee.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import edu.scu.greetee.android.R;
import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.model.Event;

/**
 * Created by srbkr on 3/10/2016.
 */
public class EventRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Event> data;
    Context context;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==1){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_single, parent, false);
            WorkViewHolder vh = new WorkViewHolder(v);
            return vh;
        }
        else{
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_single, parent, false);
            EventsViewHolder vh = new EventsViewHolder(v);
            return vh;
        }


    }
    @Override
    public int getItemViewType(int position) {
        if(position==0){
        return 1;
        }
        return 2;

    }

    public EventRecyclerAdapter(List<Event> data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Event event= data.get(position);
        String eventweather_update = "";
        switch (holder.getItemViewType()) {
            case 1:
                WorkViewHolder wholder=(WorkViewHolder)holder;
                if(event.getName().startsWith("$$$")){
                    wholder.eventStart.setText("Add your work place to Greetee");
                    wholder.icon.setImageResource(R.drawable.ic_work_icon);
                }
                else if(event.getName().startsWith("###")){
                    wholder.eventStart.setText("Update for Work");
                    if(event.getLocation()!=null)
                        eventweather_update += event.getLocation().getLocality() == null ? event.getLocationString() : event.getLocation().getLocality();
                    if(event.getWeather()!=null) {
                        eventweather_update +=" (" + event.getWeather().getTemperature() + "℉ )";
                        wholder.icon.setImageResource(Utility.getIconResourceForWeatherCondition(event.getWeather().getId()));
                    }
                    else{
                        eventweather_update += " (Weather undetermined)";
                    }
                    wholder.eventWeather.setText(eventweather_update);
                    if (event.getDirection() != null) {

                        wholder.eventDistance.setText(event.getDirection().getDistance());
                        int time[] = Utility.splitToComponentTimes(BigDecimal.valueOf(event.getDirection().getDuration()));
                        wholder.eventETA.setText((time[0] > 0 ? (time[0] + "Hr") : "") + (time[1] > 0 ? (time[1] + "M") : ""));
                        wholder.eventDistance.setVisibility(View.VISIBLE);
                        wholder.eventETA.setVisibility(View.VISIBLE);
                    }
                }
                break;
            case 2:
                EventsViewHolder eholder=(EventsViewHolder)holder;
                eholder.eventStart.setText(dateFormat.format(new Date(event.getStartDate())) + " - " + event.getName());


                if (event.getLocation() != null) {
                    eventweather_update += event.getLocation().getLocality() == null ? event.getLocationString() : event.getLocation().getLocality();
                } else
                    eventweather_update += event.getLocationString();
                if (event.getWeather() != null) {
                    eventweather_update += " (" + event.getWeather().getTemperature() + "℉ )";
                    eholder.icon.setImageResource(Utility.getIconResourceForWeatherCondition(event.getWeather().getId()));
                } else
                    eventweather_update += " (Weather undetermined)";

                if (event.getDirection() != null) {
                    eholder.eventDistance.setText(event.getDirection().getDistance());
                    int time[] = Utility.splitToComponentTimes(BigDecimal.valueOf(event.getDirection().getDuration()));
                    eholder.eventETA.setText((time[0] > 0 ? (time[0] + "Hr") : "") + (time[1] > 0 ? (time[1] + "M") : ""));
                    eholder.eventDistance.setVisibility(View.VISIBLE);
                    eholder.eventETA.setVisibility(View.VISIBLE);
                }
                eholder.eventWeather.setText(eventweather_update);
                break;
        }

    }

    @Override
    public int getItemCount() {
        return (data.size());
    }
}
