package com.atrainingtracker.trainingtracker.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class LapTimer implements LocationListener {

    private MqttHandler mqttHandler;
    private static final String BROKER_URL = "tcp://mqtt.monashhumanpower.org:1883";
    private static final String CLIENT_ID = "lap_timer";

    private final Context context;
    private LocationManager locationManager;
    private static final int SAMPLING_TIME = 500;
    private static final int MIN_DISTANCE = 0;
    private static final int MAX_DISTANCE = 10;
    private static final int LAP_DELAY = 10;
    private Location startLocation, lastLocation;

    private boolean is_paused = false;
    private float pauseTime = 0;
    private long pauseStart = 0;


    private long lastLapTime = -1;
    private float totalDistance = 0;
    private float lastLapDistance;
    private int lapNumber = 1;


    public static final String LAP_DATA_TOPIC = "trike/lap/data";
    public static final String LAP_TRIGGER_TOPIC = "trike/lap/trigger";


    public LapTimer(Context context) {
        this.context = context;
    }


    public void lap(){
        Log.i("LAP", "Lap "+ lapNumber + " started ");
        try {
            mqttHandler.publish(LAP_TRIGGER_TOPIC, "lap");
            if (lastLapTime != -1) // If a lap has been completed
            {
                // Calculate lap information
                long lapTimeLong = (System.currentTimeMillis() - lastLapTime);
                float lapTimeFloat = (float) lapTimeLong;
                float lapTime =  lapTimeFloat/1000 - pauseTime;
                float lapDistance = totalDistance - lastLapDistance;
                float lapSpeed = lapDistance/lapTime;

                // Put information in Json Object
                JSONObject lapData = new JSONObject();
                lapData.put("lapNumber", lapNumber - 1 );
                lapData.put("time", lapTime);
                lapData.put("distance", lapDistance);
                lapData.put("speed", lapSpeed);

                // Publish to MQTT
                mqttHandler.publish(LAP_DATA_TOPIC, lapData.toString());
                Log.i("LAP", lapData.toString());

                // Update last_lap information
                lastLapDistance = lapDistance;
                lastLapTime = System.currentTimeMillis();
                lapNumber = lapNumber + 1;
                Log.i("LAP", Float.toString(lapTime));
                pauseTime = 0;

            }
            else { // First lap started
                lastLapTime = System.currentTimeMillis();
                lapNumber = lapNumber + 1;
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    @SuppressLint("MissingPermission")
    public void start()
    {
        // Create MQTT client and connect to broker
        mqttHandler = new MqttHandler();
        mqttHandler.connect(BROKER_URL, CLIENT_ID);
        Log.i("LAP", "mqtt connected");

        // Create reference to GPS
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, SAMPLING_TIME, MIN_DISTANCE, this);

        lap();
    }

    public void stop()
    {
        // Disconnect to MQTT
        mqttHandler.disconnect();
        Log.i("LAP", "mqtt disconnected");

        // Remove location callback
        locationManager.removeUpdates(this);

    }

    public void pause()
    {
        is_paused = true;
        pauseStart = System.currentTimeMillis();
    }

    public void resume()
    {
        is_paused = false;
        long pauseTimeLong = System.currentTimeMillis() - pauseStart;
        pauseTime = pauseTime + ((float) pauseTimeLong)/1000;
        Log.i("LAP", "Paused for " + pauseTime);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (!is_paused)
        {
            long lapTime = (System.currentTimeMillis() - lastLapTime) / 1000;
            if (location != null) {
                if (startLocation == null) // Set start location
                {
                    startLocation = location;
                } else {
                    totalDistance = totalDistance + location.distanceTo(lastLocation);

                    Log.i("LAP", "Location update, distance from start is: " + location.distanceTo(startLocation));
                    if (location.distanceTo(startLocation) < MAX_DISTANCE) {
                        if (lapTime > LAP_DELAY) {
                            lap();
                        }
                    }
                }

                //Update last location
                lastLocation = location;
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d("STATUS", "Status changed");
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d("PROVIDER_ENABLED", "Provider has been enabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d("PROVIDER_DISABLED", "Provider has been disabled");
    }

}
