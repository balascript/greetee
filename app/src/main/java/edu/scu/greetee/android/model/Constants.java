package edu.scu.greetee.android.model;

import android.content.Context;
import android.provider.CalendarContract;
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
    public static final String[] PERMISSIONS={"permission.READ_CALENDAR","android.permission.USE_CREDENTIALS","android.permission.MANAGE_ACCOUNTS","android.permission.GET_ACCOUNTS"};
    public static final String DEFAULT_LOCATION_STRING = "user_default_location";
    public static final String OPEN_MAP_API_URL="http://api.openweathermap.org/data/2.5/weather?";
    public static final String OPEN_WEATHER_MAP_API_KEY = "7525749b09649701a30c07287270b80a";
    public static final int DEFAULT_LOCATION_ZIP=53706;
    public static final String STRING_PARAM_OPERATION = "REQUEST_WEATHER";
    public static final String SERVICE_RESPONSE = "Service_response";
    public static final int SERVICE_RESPONSE_WEATHER = 6011;
    public static final int SERVICE_REQUEST_WEATHER = 6010;
    public static final int SERVICE_REQUEST_EVENT = 6020;
    public static final int SERVICE_RESPONSE_EVENT = 6021;
    public static final String SERVICE_INTENT = "edu.scu.service.intent";
    public static final int USERAUTHERROR = 9999;


    public static void toastMessage(Context ctx, String msg){
        Toast.makeText(ctx,msg,Toast.LENGTH_LONG);
    }
    // Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
    public static final String[] CALENDAR_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };
    public static final String[] EVENTS_PROJECTION = new String[]{
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ACCOUNT_NAME};
    // The indices for the projection array above.
    public static final int PROJECTION_TITLE_INDEX = 0;
    public static final int PROJECTION_EVENT_LOCATION_INDEX = 1;
    public static final int PROJECTION_BEGIN_INDEX = 2;
    public static final int PROJECTION_END_INDEX = 3;
  //  public static final int PROJECTION_ALL_DAY_INDEX = 4;
    public static final int PROJECTION_ACCOUNT_NAME_INDEX = 4;

}
