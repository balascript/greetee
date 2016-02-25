package edu.scu.greetee.android.model;

import android.location.Location;

import com.google.api.client.util.DateTime;

import java.util.Date;

/**
 * Created by srbkr on 2/25/2016.
 */
public class Event {
    private String Name,locationString;
    private Location location;
    private DateTime startDate,endDate;

    public Event(String name, String locationString,Location location, DateTime startDate, DateTime endDate) {
        Name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.locationString=locationString;
    }

    public String getName() {
        return Name;
    }

    public Location getLocation() {
        return location;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public String getLocationString() {
        return locationString;
    }
}
