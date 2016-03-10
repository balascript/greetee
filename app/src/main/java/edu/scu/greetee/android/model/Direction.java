package edu.scu.greetee.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by srbkr on 2/26/2016.
 */
public class Direction implements Parcelable,Serializable {
    private int duration;
    private String distance;

    public Direction(int duration, String distance) {
        this.duration = duration;
        this.distance = distance;
    }

    protected Direction(Parcel in) {
        duration = in.readInt();
        distance = in.readString();
    }

    public static final Creator<Direction> CREATOR = new Creator<Direction>() {
        @Override
        public Direction createFromParcel(Parcel in) {
            return new Direction(in);
        }

        @Override
        public Direction[] newArray(int size) {
            return new Direction[size];
        }
    };

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(duration);
        dest.writeString(distance);
    }

    public int getDuration() {
        return duration;
    }

    public String getDistance() {
        return distance;
    }
}
