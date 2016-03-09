package edu.scu.greetee.android;

import android.Manifest;
import android.app.Dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;



import edu.scu.greetee.android.model.Constants;

public class WelcomeActivity extends AppCompatActivity {

    SharedPreferences settings;

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if (Build.VERSION.SDK_INT >= 23) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            {

                this.requestPermissions(Constants.PERMISSIONS, Constants.CALENDAR_PERMISSION_CODE);

            }
            else if(Utility.isNecessarySettingsDone(this))
                launchMain();
            else
                launchSettings();


        }
        else if(Utility.isNecessarySettingsDone(this))
            launchMain();
        else
            launchSettings();

    }

    private void launchSettings() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run(){
                Intent i = new Intent(WelcomeActivity.this, SettingsActivity.class);
                i.putExtra("FirstTime",true);
                startActivity(i);
                finish();
            }
        }, 1000);
    }

    private void launchMain(){
        if (isDeviceOnline()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run(){
                    Intent i = new Intent(WelcomeActivity.this, AppControllerActivity.class);
                    startActivity(i);
                    finish();
                }
            }, 2000);
        } else {
            Constants.toastMessage(this,"No network connection available.");
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.CALENDAR_PERMISSION_CODE: {
                if (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    if(Utility.isNecessarySettingsDone(this))
                        launchMain();
                    else
                        launchSettings();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission NOT Granted to Greetee", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                WelcomeActivity.this,
                Constants.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case Constants.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


}
