package edu.scu.greetee.android.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by srbkr on 2/25/2016.
 */
public class Weather implements Serializable,Parcelable{

    private String Summary;
    private double HiTemp, LowTemp,Temperature;
    private int LocationZip;
    private Location location;
    private int art,icon,id;

    public Weather(String summary, int hiTemp, int lowTemp, int locationZip) {
        Summary = summary;
        HiTemp = hiTemp;
        LowTemp = lowTemp;
        LocationZip = locationZip;
    }

    public Weather() {

    }

    protected Weather(Parcel in) {
        Summary = in.readString();
        HiTemp = in.readDouble();
        LowTemp = in.readDouble();
        Temperature = in.readDouble();
        LocationZip = in.readInt();
        location = in.readParcelable(Location.class.getClassLoader());
        art = in.readInt();
        icon = in.readInt();
        id = in.readInt();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Summary);
        dest.writeDouble(HiTemp);
        dest.writeDouble(LowTemp);
        dest.writeDouble(Temperature);
        dest.writeInt(LocationZip);
        dest.writeParcelable(location, flags);
        dest.writeInt(art);
        dest.writeInt(icon);
        dest.writeInt(id);
    }

    public static final Creator<Weather> CREATOR = new Creator<Weather>() {
        @Override
        public Weather createFromParcel(Parcel in) {
            return new Weather(in);
        }

        @Override
        public Weather[] newArray(int size) {
            return new Weather[size];
        }
    };

    public String getSummary() {
        return Summary;
    }

    public double getHiTemp() {
        return HiTemp;
    }

    public double getLowTemp() {
        return LowTemp;
    }

    public int getLocationZip() {
        return LocationZip;
    }

    public int getArt() {
        return art;
    }

    public void setArt(int art) {
        this.art = art;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setSummary(String summary) {
        Summary = summary;
    }

    public void setHiTemp(double hiTemp) {
        HiTemp = hiTemp;
    }

    public void setLowTemp(double lowTemp) {
        LowTemp = lowTemp;
    }

    public void setLocationZip(int locationZip) {
        LocationZip = locationZip;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getTemperature() {
        return Temperature;
    }

    public void setTemperature(double temperature) {
        Temperature = temperature;
    }

    @Override
    public int describeContents() {
        return 0;
    }


}

