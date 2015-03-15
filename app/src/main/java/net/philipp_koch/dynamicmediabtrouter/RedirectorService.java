package net.philipp_koch.dynamicmediabtrouter;

import java.lang.String;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.SharedPreferences;
import android.os.Build;
import android.content.Intent;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.lang.Thread;
import android.util.Log;

/**
 * Created by Philipp on 12.03.2015.
 */
public class RedirectorService extends Service {


    private Global localGlobal;
    private boolean keeprunning = false;
    private static final int api = Build.VERSION.SDK_INT;
    public SharedPreferences localPreferences;

    public IBinder onBind(Intent Intent) {
        return null;
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        //Toast.makeText(this, "OnStartCommand", Toast.LENGTH_LONG).show();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        BluetoothAdapter localBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int btState = localBluetoothAdapter.getState();
        localGlobal = ((Global)getApplicationContext());
        localPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean staticredirection = localPreferences.getBoolean("staticredirection", false);
        Log.d("BTService", "static pref: " + staticredirection);

        if(btState == BluetoothAdapter.STATE_ON || btState == BluetoothAdapter.STATE_TURNING_ON) {
            Toast.makeText(this, "Service starting", Toast.LENGTH_LONG).show();
            keeprunning = true;
            localGlobal.Service = "YES";
            if(staticredirection)
            {
                AudioManager localAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                localGlobal.Audio = "no information - static redirection active";
                localAudioManager.setBluetoothScoOn(true);
                localAudioManager.startBluetoothSco();
                localAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
            else {
                localGlobal.Audio = "NO";
                new Thread() {
                    public void run() {
                        checkSound();
                    }
                }.start();
            }
        }
        else
        {
            Toast.makeText(this, "Turn on Bluetooth first", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        keeprunning = false;
        localGlobal.Audio = "";
        localGlobal.Service = "NO";
        AudioManager localAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        localAudioManager.setBluetoothScoOn(false);
        localAudioManager.stopBluetoothSco();
        localAudioManager.setMode(AudioManager.MODE_NORMAL);
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    private void checkSound() {
        Global localGlobal = ((Global)getApplicationContext());
        AudioManager localAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean wasPlayingBefore = false;
        boolean headsetConnectedBefore = false;
        BluetoothAdapter localBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (api >= 19) {
            Visualizer localVisualizer = new Visualizer(0);
            localVisualizer.setEnabled(true);
            localVisualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);
            Visualizer.MeasurementPeakRms localPeak = new Visualizer.MeasurementPeakRms();

            while (keeprunning) //Loop to detect changes on the Media Audio Stream
            {
                localVisualizer.getMeasurementPeakRms(localPeak);//get the current audio peak -9600 = silent, 0 = MAX output
                int btHeadsetState = localBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET); //get the current connection status for the handsfree profile
                if (localPeak.mPeak > -8500 && !wasPlayingBefore && btHeadsetState == BluetoothProfile.STATE_CONNECTED) {
                    //There is some audio output
                    wasPlayingBefore = true;
                    headsetConnectedBefore = true;
                    localGlobal.Audio="YES";
                    localAudioManager.setBluetoothScoOn(true);
                    localAudioManager.startBluetoothSco();
                    localAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    android.os.SystemClock.sleep(5000); //Route to BT Headset will exist min. 5 seconds ...
                }
                if (localPeak.mPeak <= -8500 && wasPlayingBefore) {
                    //output (temporary) gone
                    android.os.SystemClock.sleep(2000);//... plus this 2 seconds
                    localVisualizer.getMeasurementPeakRms(localPeak); //check again
                }
                if ((localPeak.mPeak <= -8500 && wasPlayingBefore) || (headsetConnectedBefore && btHeadsetState == BluetoothProfile.STATE_DISCONNECTED)) {
                    //Audio didn't get back in last 2 seconds...
                    wasPlayingBefore = false;
                    headsetConnectedBefore = false;
                    localGlobal.Audio="NO";
                    localAudioManager.setBluetoothScoOn(false);
                    localAudioManager.stopBluetoothSco();
                    localAudioManager.setMode(AudioManager.MODE_NORMAL);
                }
                android.os.SystemClock.sleep(100); //Slow down the loop
                Log.d("BTService", "Peak: " + String.valueOf(localPeak.mPeak)); //Debug info - Audio peak -9600 = Silent, 0 = MAX Output
            }
            localVisualizer.release();
        } else {
            while (keeprunning) {
                //No KITKAT :(
                int btHeadsetState = localBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET); //get the current connection status for the handsfree profile
                if (localAudioManager.isMusicActive() && btHeadsetState == BluetoothProfile.STATE_CONNECTED) {
                    //There is some audio output
                    wasPlayingBefore = true;
                    headsetConnectedBefore = true;
                    localAudioManager.setBluetoothScoOn(true);
                    localAudioManager.startBluetoothSco();
                    localAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    android.os.SystemClock.sleep(5000); //Route to BT Headset will exist min. 5 seconds ...
                }
                if (!localAudioManager.isMusicActive() && wasPlayingBefore) {
                    //output (temporary) gone
                    android.os.SystemClock.sleep(2000);//... plus this 2 seconds
                }
                if ((!localAudioManager.isMusicActive() && wasPlayingBefore) || (headsetConnectedBefore && btHeadsetState == BluetoothProfile.STATE_DISCONNECTED)) {
                    //Audio didn't get back in last 2 seconds...
                    wasPlayingBefore = false;
                    headsetConnectedBefore = false;
                    localAudioManager.setBluetoothScoOn(false);
                    localAudioManager.stopBluetoothSco();
                    localAudioManager.setMode(AudioManager.MODE_NORMAL);
                }
                android.os.SystemClock.sleep(100); //Slow down the loop
                Log.d("BTService", "Music active: " + String.valueOf(localAudioManager.isMusicActive())); //Debug info - Audio peak -9600 = Silent, 0 = MAX Output
            }
        }
    }
}
