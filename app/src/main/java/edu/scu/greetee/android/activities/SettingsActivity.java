package edu.scu.greetee.android.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.io.IOException;
import java.nio.charset.Charset;

import edu.scu.greetee.android.R;
import edu.scu.greetee.android.Utility;
import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.services.GreeteeHTTPService;

public class SettingsActivity extends AppCompatActivity {

  View Home,Work,NFC;
    TextView home,work,nfc;
    private final int RequestHomePlace=11;
    private final int RequestWorkPlace=12;
    SharedPreferences sharedpreferences;
    boolean isLaunchedByWelcomeActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        isLaunchedByWelcomeActivity=getIntent().getBooleanExtra("welcome",false);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Configuration");
        }
        sharedpreferences= PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        Home=  findViewById(R.id.home_card);
        home=(TextView)Home.findViewById(R.id.home_location_text);
        Work=  findViewById(R.id.work_card);
        work=(TextView)Work.findViewById(R.id.work_location_text);

        NFC=  findViewById(R.id.nfc_card);
        nfc=(TextView)NFC.findViewById(R.id.nfc_text);
        final PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        View.OnClickListener lis=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.home_card:

                        try {
                            startActivityForResult(builder.build(SettingsActivity.this), RequestHomePlace);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.work_card:
                        try {
                            startActivityForResult(builder.build(SettingsActivity.this), RequestWorkPlace);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.nfc_card:
                        String nfcMessage = "maindoor";
                        // When an NFC tag comes into range, call onNewIntent which handles writing the data to the tag
                        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(SettingsActivity.this);
                        Intent nfcIntent = new Intent(SettingsActivity.this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        nfcIntent.putExtra("nfcMessage", nfcMessage);
                        PendingIntent pi = PendingIntent.getActivity(SettingsActivity.this, 0, nfcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                        nfcAdapter.enableForegroundDispatch((Activity)SettingsActivity.this, pi, new IntentFilter[] {tagDetected}, null);
                        Toast.makeText(SettingsActivity.this,"Approach a writable NFC Tag",Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(SettingsActivity.this," Clicked "+v.getId(),Toast.LENGTH_LONG).show();
                }

            }
        };
        Home.setOnClickListener(lis);
        Work.setOnClickListener(lis);
        NfcManager manager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            NFC.setOnClickListener(lis);
            if(sharedpreferences.getBoolean(Constants.isNFCConfiguredString,false)){
                nfc.setText("Configured atleast 1 Tag(s)");
            }
        }
        else{
            nfc.setText("Nfc not enabled/supported");
        }

        if(sharedpreferences.getBoolean(Constants.isHomeSelectedString,false)){
            home.setText(sharedpreferences.getString(Constants.HomeLocationString,"Un-named"));
        }
        if(sharedpreferences.getBoolean(Constants.isWorkSelectedString,false)){
            work.setText(sharedpreferences.getString(Constants.WorkLocationString,"Un-named"));
        }



    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        switch (requestCode){
            case RequestHomePlace:
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(data, this);
                    if(place.getAddress()!=null)
                        editor.putString(Constants.HomeLocationString,place.getAddress().toString());
                    else
                        editor.putString(Constants.HomeLocationString,place.getName().toString());
                    editor.putBoolean(Constants.isHomeSelectedString,true);
                    editor.putFloat(Constants.HomeLatitudeString, (float) place.getLatLng().latitude);
                    editor.putFloat(Constants.HomeLongitudeString, (float) place.getLatLng().longitude);
                    editor.putBoolean(Constants.SettingsChanged,true);
                    editor.commit();
                    home.setText(sharedpreferences.getString(Constants.HomeLocationString,"Un-named"));


                }
                break;
            case RequestWorkPlace:
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(data, this);
                    if(place.getAddress()!=null)
                        editor.putString(Constants.WorkLocationString,place.getAddress().toString());
                    else
                        editor.putString(Constants.WorkLocationString,place.getName().toString());
                    editor.putBoolean(Constants.isWorkSelectedString,true);
                    editor.putFloat(Constants.WorkLatitudeString, (float) place.getLatLng().latitude);
                    editor.putFloat(Constants.WorkLongitudeString, (float) place.getLatLng().longitude);
                    editor.putBoolean(Constants.SettingsChanged,true);
                    editor.commit();
                    work.setText(sharedpreferences.getString(Constants.WorkLocationString,"Un-named"));

                }
                break;

        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
                if(isLaunchedByWelcomeActivity && Utility.isNecessarySettingsDone(this)){
                    Intent main= new Intent(this,GreeteeMainActivity.class);
                    startActivity(main);
                }else{
                    finish();
                }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(isLaunchedByWelcomeActivity && Utility.isNecessarySettingsDone(this)){
            Intent main= new Intent(this,GreeteeMainActivity.class);
            startActivity(main);
        }else{
            finish();
        }
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // When an NFC tag is being written, call the write tag function when an intent is
        // received that says the tag is within range of the device and ready to be written to
        SharedPreferences.Editor editor = sharedpreferences.edit();
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String nfcMessage = intent.getStringExtra("nfcMessage");
        Toast.makeText(this,"Starting to configure NFC Tag...",Toast.LENGTH_SHORT).show();
        if(nfcMessage != null) {
           if(writeTag(this, tag, nfcMessage)){
               Toast.makeText(this,"Done Successfully...",Toast.LENGTH_SHORT).show();
               editor.putBoolean(Constants.isNFCConfiguredString,true);
               editor.commit();
           }
            else{
               Toast.makeText(this,"NFC configuration failed...",Toast.LENGTH_SHORT).show();
           }

        }
    }
    public static boolean writeTag(Context context, Tag tag, String data) {
        // Record to launch Play Store if app is not installed
        NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());

        // Record with actual data we care about
        NdefRecord relayRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                new String("application/" + context.getPackageName()).getBytes(Charset.forName("US-ASCII")),
                null, data.getBytes());

        // Complete NDEF message with both records
        NdefMessage message = new NdefMessage(new NdefRecord[] {relayRecord, appRecord});

        try {
            // If the tag is already formatted, just write the message to it
            Ndef ndef = Ndef.get(tag);
            if(ndef != null) {
                ndef.connect();

                // Make sure the tag is writable
                if(!ndef.isWritable()) {
                    return false;
                }

                // Check if there's enough space on the tag for the message
                int size = message.toByteArray().length;
                if(ndef.getMaxSize() < size) {
                    return false;
                }

                try {
                    // Write the data to the tag
                    ndef.writeNdefMessage(message);

                    return true;
                } catch (TagLostException tle) {
                    return false;
                } catch (IOException ioe) {
                    return false;
                } catch (FormatException fe) {
                    return false;
                }
                // If the tag is not formatted, format it with the message
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if(format != null) {
                    try {
                        format.connect();
                        format.format(message);

                        return true;
                    } catch (TagLostException tle) {
                        return false;
                    } catch (IOException ioe) {
                        return false;
                    } catch (FormatException fe) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch(Exception e) {
        }

        return false;
    }


}
