package edu.scu.greetee.android.model;

import android.content.Context;
import android.widget.Toast;

import com.google.api.services.calendar.CalendarScopes;

/**
 * Created by srbkr on 2/24/2016.
 */
public class Constants {
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };
    public static final int CALENDAR_PERMISSION_CODE=6000;
    public static final String[] PERMISSIONS={"android.permission.USE_CREDENTIALS","android.permission.MANAGE_ACCOUNTS","android.permission.GET_ACCOUNTS"};
    public static final String DEFAULT_LOCATION_STRING = "user_default_location";
    public static final String OPEN_MAP_API_URL="http://api.openweathermap.org/data/2.5/weather?";
    public static final String OPEN_WEATHER_MAP_API_KEY = "7525749b09649701a30c07287270b80a";
    public static final int DEFAULT_LOCATION_ZIP=95050;
    public static final String STRING_PARAM_OPERATION = "REQUEST_WEATHER";

    public static void toastMessage(Context ctx, String msg){
        Toast.makeText(ctx,msg,Toast.LENGTH_SHORT);
    }
}
