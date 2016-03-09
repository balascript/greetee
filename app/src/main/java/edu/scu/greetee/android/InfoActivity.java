package edu.scu.greetee.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.scu.greetee.android.model.Constants;
import edu.scu.greetee.android.model.Direction;
import edu.scu.greetee.android.model.Event;
import edu.scu.greetee.android.model.Weather;

public class InfoActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener{
    TextView InfoToUser;
    ImageView Mute;
    //BC Receiver
    private DataReciever receiver;
    private TextToSpeech speaker;
    private boolean speakEnabled=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Intent intent = getIntent();
        if(intent.getType() != null && intent.getType().equals("application/" + getPackageName())) {
            // Read the first record which contains the NFC data
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefRecord relayRecord = ((NdefMessage)rawMsgs[0]).getRecords()[0];
            String nfcData = new String(relayRecord.getPayload());

            // Display the data on the tag
            Toast.makeText(this, nfcData, Toast.LENGTH_SHORT).show();
        }
        receiver= new DataReciever();
        InfoToUser=(TextView)findViewById(R.id.DialogText);
        speaker= new TextToSpeech(this,this);

    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Constants.SERVICE_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,intentFilter);
        startAlertService();
    }

    private void startAlertService() {
        Intent GreeteeService= new Intent(this,edu.scu.greetee.android.services.GreeteeHTTPService.class);
        GreeteeService.putExtra(Constants.STRING_PARAM_OPERATION,Constants.SERVICE_REQUEST_ALERT);
        Toast.makeText(this,"Updating.. Please wait!",Toast.LENGTH_SHORT);
        startService(GreeteeService);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = speaker.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                this.speakEnabled=true;
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }


    public class DataReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            int response= intent.getIntExtra(Constants.SERVICE_RESPONSE,0);
            Bundle bundleExtra= intent.getBundleExtra("data");
            switch (response){
                case Constants.SERVICE_RESPONSE_ALERT:
                    String UserMessage= intent.getStringExtra(Constants.SERVICE_REQUEST_ALERT_STRING);
                    InfoToUser.setText(UserMessage);
                    speakOut(UserMessage);
                    break;

                case Constants.USERAUTHERROR:
                    Intent errorIntent= (Intent) intent.getExtras().get("intent");
                    startActivityForResult(
                            errorIntent, Constants.REQUEST_ACCOUNT_PICKER);

            }


        }
    }
    private void speakOut(String msg) {

        if(!this.speakEnabled) return;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            speaker.speak(text, TextToSpeech.QUEUE_FLUSH,null, null);
//        }
        speaker.speak(msg, TextToSpeech.QUEUE_FLUSH,null);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Don't forget to shutdown tts!
        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
