package com.atrainingtracker.trainingtracker.fragments;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class SensorDataToJSON {
    private JSONArray sensorsArray = new JSONArray();

    public void addSensorData(String sensorType, String sensorValue){
        // Create a new JSON object for the sensor data
        JSONObject sensorData = new JSONObject();
        try{
            sensorData.put("type", sensorType);
            sensorData.put("value", sensorValue);
            sensorsArray.put(sensorData);

        } catch (JSONException e){
            e.printStackTrace();
        }


        // Add the new sensor data to the sensors array

    }

    public JSONObject createJsonFromSensors() {
        // Create the JSON object that will contain the "sensors" array
        JSONObject rootObject = new JSONObject();

        try{
            rootObject.put("sensors", sensorsArray);

        } catch (JSONException e){
            e.printStackTrace();
        }



        return rootObject;
    }
}
