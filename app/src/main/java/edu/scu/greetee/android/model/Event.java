package edu.scu.greetee.android.model;

import android.location.Address;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.client.util.DateTime;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by srbkr on 2/25/2016.
 */
public class Event implements Parcelable,Serializable{
    private String Name,locationString;
    private Address location;
    private long startDate,endDate;

    public Event(String name, String locationString,Address location, long startDate, long endDate) {
        Name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.locationString=locationString;
    }


    protected Event(Parcel in) {
        Name = in.readString();
        locationString = in.readString();
        location = in.readParcelable(Location.class.getClassLoader());
        startDate = in.readLong();
        endDate = in.readLong();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public String getName() {
        return Name;
    }

    public Address getLocation() {
        return location;
    }



    public String getLocationString() {
        return locationString;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(Name);
        dest.writeString(locationString);
        dest.writeParcelable(location, flags);
        dest.writeLong(startDate);
        dest.writeLong(endDate);
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setLocation(Address location) {
        this.location = location;
    }
}
