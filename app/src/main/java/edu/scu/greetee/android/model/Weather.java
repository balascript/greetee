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
    private int HiTemp, LowTemp,Temperature;
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
        HiTemp = in.readInt();
        LowTemp = in.readInt();
        Temperature = in.readInt();
        LocationZip = in.readInt();
        location = in.readParcelable(Location.class.getClassLoader());
        art = in.readInt();
        icon = in.readInt();
        id = in.readInt();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Summary);
        dest.writeInt(HiTemp);
        dest.writeInt(LowTemp);
        dest.writeInt(Temperature);
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

    public int getHiTemp() {
        return HiTemp;
    }

    public int getLowTemp() {
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

    public void setHiTemp(int hiTemp) {
        HiTemp = hiTemp;
    }

    public void setLowTemp(int lowTemp) {
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

    public int getTemperature() {
        return Temperature;
    }

    public void setTemperature(int temperature) {
        Temperature = temperature;
    }

    @Override
    public int describeContents() {
        return 0;
    }


}

