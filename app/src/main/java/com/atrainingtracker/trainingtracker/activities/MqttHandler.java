package com.atrainingtracker.trainingtracker.activities;

import android.os.AsyncTask;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client;
    private static final int CONNECTION_TIMEOUT = 5;

    public void connectAsync(final String brokerUrl, final String clientId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                connect(brokerUrl, clientId);
                return null;
            }
        }.execute();
    }
    public void connect(String brokerUrl, String clientId) {
        try {
            // Set up the persistence layer
            MemoryPersistence persistence = new MemoryPersistence();

            // Initialize the MQTT client
            client = new MqttClient(brokerUrl, clientId, persistence);

            // Set up the connection options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            char[] newpassword = {'z','9', '0', 'H', 'u', 'N', '9', 'F', '&', 'H', '3', 'r','d','d', '8', 'b', 'w', 'i', '&', 'b'};
            connectOptions.setPassword(newpassword);
            connectOptions.setUserName("WM1");
            // Connect to the broker
            client.connect(connectOptions);

            System.out.println("Successfully connected to MQTT broker");
        } catch (MqttException e) {
            System.err.println("Error connecting to mqtt broker: " + e.getMessage());

        }
    }

    public boolean isConnected(){
        return client != null && client.isConnected();
    }

    public void disconnectAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                disconnect();
                return null;
            }
        }.execute();
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishAsync(final String topic, final String message) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                publish(topic, message);
                return null;
            }
        }.execute();
    }
    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribeAsync(final String topic) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                subscribe(topic);
                return null;
            }
        }.execute();
    }

    public boolean nullClient(){
        if (client == null){
            return true;
        }
        return false;
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
