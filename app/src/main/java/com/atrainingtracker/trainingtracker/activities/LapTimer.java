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
    private static final String BROKER_URL = "";
    private static final String CLIENT_ID = "";

    private final Context context;
    private LocationManager locationManager;
    private static final int SAMPLING_TIME = 500;
    private static final int MIN_DISTANCE = 0;
    private static final int MAX_DISTANCE = 10;
    private Location startLocation, lastLocation;


    private long lastLapTime = -1;
    private float totalDistance = 0;
    private float lastLapDistance;
    private int lapNumber = 1;


    public static final String LAP_DATA_TOPIC = "trike/lap/data";
    public static final String LAP_TRIGGER_TOPIC = "trike/lap/trigger";
    public static final int MS_TO_S = 1000;
    public static final int M_TO_KM = 1000;

    public LapTimer(Context context) {
        this.context = context;
    }


    public void lap(){
        try {
            mqttHandler.publish(LAP_TRIGGER_TOPIC, "lap");
            if (lastLapTime != -1) // If a lap has been completed
            {
                // Calculate lap information
                long lapTime =  System.currentTimeMillis()*MS_TO_S - lastLapTime;
                float lapDistance = totalDistance - lastLapDistance;
                float lapSpeed = lapDistance/lapTime;

                // Put information in Json Object
                JSONObject lapData = new JSONObject();
                lapData.put("lapNumber", lapNumber);
                lapData.put("time", lapTime);
                lapData.put("distance", lapDistance);
                lapData.put("speed", lapSpeed);

                // Publish to MQTT
                mqttHandler.publish(LAP_DATA_TOPIC, lapData.toString());

                // Update last_lap information
                lastLapDistance = lapDistance;
                lastLapTime = System.currentTimeMillis()*MS_TO_S;
                lapNumber = lapNumber +1;

            }
            else { // First lap started
                lastLapTime = System.currentTimeMillis()*MS_TO_S;
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

        // Create reference to GPS
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, SAMPLING_TIME, MIN_DISTANCE, this);

        lap();
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
        {
            if (startLocation == null) // Set start location
            {
                startLocation = location;
            }
            else
            {
                totalDistance = totalDistance + location.distanceTo(lastLocation);

                if (haversineDistance(location.getLatitude(), location.getLongitude(),startLocation.getLatitude(), startLocation.getLongitude()) < MAX_DISTANCE)
                {
                    lap();
                }
            }

            //Update last location
            lastLocation = location;
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

    public double haversineDistance(double lat1, double long1, double lat2, double long2)
    {
        double earthRadius = 6371;
        double latChangeRad = Math.toRadians(lat2 - lat1);
        double lonChangeRad = Math.toRadians(long2 - long1);

        double a = Math.pow(Math.sin(latChangeRad / 2), 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.pow(Math.sin(lonChangeRad / 2), 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * angularDistance; // In km
        return distance/M_TO_KM;
    }
}
