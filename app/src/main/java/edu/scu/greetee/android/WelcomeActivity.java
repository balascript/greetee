package edu.scu.greetee.android;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;

import java.util.Arrays;

import edu.scu.greetee.android.model.Constants;

public class WelcomeActivity extends AppCompatActivity {
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    SharedPreferences settings;

    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            authGoogle();
        } else {
            Constants.toastMessage(this,"Google Play Services required: " +
                    "after installing, close and relaunch this app.");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        settings = getPreferences(Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 23) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
            {

                this.requestPermissions(Constants.PERMISSIONS, Constants.CALENDAR_PERMISSION_CODE);

            } else {
                mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(Constants.SCOPES))
                        .setBackOff(new ExponentialBackOff())
                        .setSelectedAccountName(settings.getString(Constants.PREF_ACCOUNT_NAME, null));
                authGoogle();
            }

        } else {
            mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(Constants.SCOPES))
                    .setBackOff(new ExponentialBackOff())
                    .setSelectedAccountName(settings.getString(Constants.PREF_ACCOUNT_NAME, null));
            authGoogle();

        }

    }

    private void authGoogle() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
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
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.CALENDAR_PERMISSION_CODE: {
                if (grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    authGoogle();
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
            case Constants.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(Constants.PREF_ACCOUNT_NAME, accountName);
                        editor.apply();

                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Constants.toastMessage(this," User Cancelled the Request");
                    finish();
                }
                break;
            case Constants.REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), Constants.REQUEST_ACCOUNT_PICKER);
    }

}
