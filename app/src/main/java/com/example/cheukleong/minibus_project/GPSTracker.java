package com.example.cheukleong.minibus_project;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import javax.security.auth.login.LoginException;

public class GPSTracker extends Service
{
    public static String CAR_ID;
    private static final String TAG = "Gash";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private int count=0;
    public static double endlocation_x;
    public static double startlocation_x;
    public static double endlocation_y;
    public static double startlocation_y;
    public static double update_location_x;
    public static double update_location_y;
    public static String journeyid;
    public static String route;
    public Date localTime_date;
    public Long start_time;
    public Long end_time;
    public Long update_time;
    public static int arr_station=-2;
    public static int go_back=3;
    public static int init=0;
    public static int send_stationinfo=0;
    public static int send_i=0;
    public static int dans=30;
    public int Bat_info=100;
    private static Timer timer = new Timer();
    private Context ctx;
    public double go_station[][]={
            {22.2837,114.1588},
            {22.2841445,114.1392645},
            {22.2836933,114.1366914},
            {22.26823162,114.12865509},
            {22.26642942,114.12825444},
            {22.2619,114.1319}
    };

    double back_station[][]={
            {22.2619,114.1319},
            {22.266572,114.128184},
            {22.269442,114.129753},
            {22.2843794,114.13428},
            {22.2837,114.1588}
    };

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            Calendar cal = Calendar.getInstance();
            Date currentLocalTime = cal.getTime();
            Log.e("DEBUG FOR TIMESTAMP:",String.valueOf((currentLocalTime.getTime())));
            if(count==0)
            {
                journeyid = String.valueOf(currentLocalTime.getTime())+CAR_ID;
                send_journey_data(1);
                count++;
            }


            //udate location
            update_location_x=location.getLatitude();
            update_location_y=location.getLongitude();
            update_time=currentLocalTime.getTime();
            send_location();




            if(init==0)
            {
                endlocation_x = location.getLatitude();
                endlocation_y = location.getLongitude();
                Log.e(TAG,"location:"+location.getLatitude()+" , "+location.getLongitude());
                Log.e(TAG, "Not init");
                init(location);
            }
            else {
                Log.e(TAG,"Init");
                Calendar now = Calendar.getInstance();
                Date now_currentLocalTime = now.getTime();
                localTime_date = now_currentLocalTime;

                //location
                endlocation_x = location.getLatitude();
                endlocation_y = location.getLongitude();


                if(arr_station>30) {
                    Log.e(TAG, "Enter check arrive which station ");
                    arrive(location);
                }
                else if(arr_station<30){
                    Log.e(TAG, "Enter check quit with station" + arr_station);
                    check_quit(location);
                }

                if (send_stationinfo==1) {
                    end_time = currentLocalTime.getTime();
                    show_stations();
                    send_data();
                    arr_station=100;
                    send_stationinfo=0;
                }
            }
        }




        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Calendar cal = Calendar.getInstance();
            Date currentLocalTime = cal.getTime();
            DateFormat date = new SimpleDateFormat("dd-mm-yy hh:mm:ss");
            String localTime = date.format(currentLocalTime);
            Log.e(TAG, "onStatusChanged: " + provider);
            Log.e(TAG, "time " + localTime);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        Log.e(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        super.onCreate();
        ctx = this;
        startService();
        initializeLocationManager();
        try {

            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }




    public void init(Location cur_location){

        double station[][]={
                {22.2837,114.1588},
                {22.2619,114.1319}
        };
        Location dest = new Location(cur_location);
        Location cur = new Location(cur_location);
        Log.e(TAG, "init_x_inside location: "+endlocation_x);
        Log.e(TAG, "init_y_inside location: "+endlocation_y);

            for(int i=0; i<2;i++) {
                if(i==0)
                {
                    dans=200;
                }
                dest.setLatitude(station[i][0]);
                dest.setLongitude(station[i][1]);
                cur.setLatitude(endlocation_x);
                cur.setLongitude(endlocation_y);
                float distance = cur.distanceTo(dest);

                Log.e(TAG, "distance: "+ distance);
                if(distance<dans)
                {
                    go_back=i;
                    Log.e(TAG,"Debug go_back = "+go_back);
                    init=1;
                    arr_station=0;
                    send_i=0;
                    Calendar cal = Calendar.getInstance();
                    Date currentLocalTime = cal.getTime();
                    localTime_date = currentLocalTime;
                    start_time = currentLocalTime.getTime();
                    startlocation_x = cur_location.getLatitude();
                    startlocation_y = cur_location.getLongitude();
                    Log.e(TAG, "init=" + init);
                    dans=30;
                    break;
                }
                else
                {
                    dans=30;
                    Log.e(TAG,"Not init");
                }
                dans=30;
            }
    }


    public void arrive(Location cur_location){
        Location dest = new Location(cur_location);
        Location cur = new Location(cur_location);
        cur.setLatitude(endlocation_x);
        cur.setLongitude(endlocation_y);

        if(go_back==0)
        {
            for(int i=0; i<6;i++) {
                dest.setLatitude(go_station[i][0]);
                dest.setLongitude(go_station[i][1]);

                if(i==0){
                    dans=200;
                }

                float distance = cur.distanceTo(dest);

                if(distance<dans && arr_station!=i && send_i!=i)
                {
                    Calendar cal = Calendar.getInstance();
                    Date currentLocalTime = cal.getTime();
                    localTime_date = currentLocalTime;
                    start_time = currentLocalTime.getTime();
                    arr_station=i;
                    Log.e(TAG, "arrive: station"+ i +" Go");
                    Log.e(TAG, "arrive: starttime"+start_time);
                    if(i==5) {
                        send_endpoint_data(5);
                        journeyid = String.valueOf(currentLocalTime.getTime()) + CAR_ID;
                        show_journey();
                        send_journey_data(2);
                        arr_station = 0;
                        go_back = 1;
                    }
                    break;
                }
                dans=30;
            }
        }
        else
        {
            for(int i=0; i<5;i++) {
                dest.setLatitude(back_station[i][0]);
                dest.setLongitude(back_station[i][1]);
                float distance = cur.distanceTo(dest);

                if(i==4){
                    dans=200;
                }
                if(distance<dans && arr_station!=i && send_i!=i)
                {
                    Calendar cal = Calendar.getInstance();
                    Date currentLocalTime = cal.getTime();
                    localTime_date = currentLocalTime;
                    start_time = currentLocalTime.getTime();
                    arr_station=i;
                    Log.e(TAG, "arrive: station"+ i +" Back");
                    Log.e(TAG, "arrive: starttime"+start_time);

                    if(i==4)
                    {
                        send_endpoint_data(4);
                        journeyid = String.valueOf(currentLocalTime.getTime())+CAR_ID;
                        show_journey();
                        send_journey_data(1);
                        arr_station=0;
                        go_back=0;
                    }

                    dans=30;
                    break;
                }

                dans=30;
            }
        }
    }




    public void check_quit(Location cur_location){

        Location dest = new Location(cur_location);
        Location cur = new Location(cur_location);
        cur.setLatitude(endlocation_x);
        cur.setLongitude(endlocation_y);

        if(go_back==0)
        {
            for(int i=0; i<6;i++) {
                dest.setLatitude(go_station[i][0]);
                dest.setLongitude(go_station[i][1]);

                if(i==0){
                    dans=250;
                }

                float distance = cur.distanceTo(dest);

                if(distance>dans && arr_station==i)
                {
                    Calendar cal = Calendar.getInstance();
                    Date currentLocalTime = cal.getTime();
                    localTime_date = currentLocalTime;
                    end_time = currentLocalTime.getTime();
                    Log.e(TAG, "quit: station"+ i +" Go");
                    if(arr_station==5)
                    {
                        go_back=1;
                    }
                    send_i=i;
                    send_stationinfo=1;
                    arr_station=100;
                    dans=30;
                    break;
                }

                dans=30;

            }
        }
        else
        {
            for(int i=0; i<5;i++) {
                dest.setLatitude(back_station[i][0]);
                dest.setLongitude(back_station[i][1]);
                float distance = cur.distanceTo(dest);

                if(i==4){
                    dans=250;
                }

                if(distance>dans && arr_station==i)
                {
                    Log.e(TAG, "quit: station"+ i +" Back");
                    if(arr_station==4)
                    {
                        go_back=0;
                    }
                    send_i=i;
                    send_stationinfo=1;
                    arr_station=100;
                    dans=30;
                    break;
                }
                dans=30;
            }
        }
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent info) {
            // TODO Auto-generated method stub
            if(Intent.ACTION_BATTERY_CHANGED.equals(info.getAction())){
                int level = info.getIntExtra("level", 0);
                Bat_info=level;
            }
        }
    };



    private void send_data(){
    //To Do: Insert journey table


    Log.d(TAG, "send_station_data: 0");
    Map< String, Object > jsonValues = new HashMap< String, Object >();
    jsonValues.put("x", endlocation_x);
    jsonValues.put("y", endlocation_y);
    Log.d(TAG, "send_station_data: 1");
    JSONObject endlocation = new JSONObject(jsonValues);


    Log.d(TAG, "send_station_data: 2");
    DefaultHttpClient client = new DefaultHttpClient();
    Log.d(TAG, "send_station_data: 3");
    HttpPost httppost = new HttpPost("http://128.199.88.79:3001/api/v1/minibus/insertStation");
    Log.d(TAG, "send_station_data: 4");


    Log.d(TAG, "send_station_data: 5");
    try {
        int temp_i = send_i+5*go_back;
        // Add your data
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("stationid", journeyid+temp_i));
        nameValuePairs.add(new BasicNameValuePair("location", endlocation.toString()));
        nameValuePairs.add(new BasicNameValuePair("startTime", start_time.toString()));
        nameValuePairs.add(new BasicNameValuePair("endTime", end_time.toString()));
        nameValuePairs.add(new BasicNameValuePair("journeyid", journeyid));

        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        Log.d("httppost: ",httppost.toString());

        try {
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            Log.d(TAG, "Before send message");
            HttpResponse response =client.execute(httppost);
            Log.d(TAG, "After send message");
            Log.d("myapp", "response " + response.getEntity());

        } catch (ClientProtocolException e) {
            Log.d("Error:","ClientProtocol");
        } catch (IOException e) {
            Log.d("Error:","IOException");
        }
    } catch (UnsupportedEncodingException e) {
        Log.d("Error:","UnsupportedEncodingException");
    }
    return ;
    }

    private void send_endpoint_data(int station){
        //To Do: Insert journey table


        Log.d(TAG, "send_station_data: 0");
        Map< String, Object > jsonValues = new HashMap< String, Object >();
        jsonValues.put("x", startlocation_x);
        jsonValues.put("y", startlocation_y);
        Log.d(TAG, "send_station_data: 1");
        JSONObject endlocation = new JSONObject(jsonValues);


        Log.d(TAG, "send_station_data: 2");
        DefaultHttpClient client = new DefaultHttpClient();
        Log.d(TAG, "send_station_data: 3");
        HttpPost httppost = new HttpPost("http://128.199.88.79:3001/api/v1/minibus/insertStation");
        Log.d(TAG, "send_station_data: 4");


        Log.d(TAG, "send_station_data: 5");
        try {
            int temp_i = send_i+5*go_back;
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("stationid", journeyid+station));
            nameValuePairs.add(new BasicNameValuePair("location", endlocation.toString()));
            nameValuePairs.add(new BasicNameValuePair("startTime", start_time.toString()));
            nameValuePairs.add(new BasicNameValuePair("endTime", start_time.toString()));
            nameValuePairs.add(new BasicNameValuePair("journeyid", journeyid));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("httppost: ",httppost.toString());

            try {
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }
                Log.d(TAG, "Before send message");
                HttpResponse response =client.execute(httppost);
                Log.d(TAG, "After send message");
                Log.d("myapp", "response " + response.getEntity());

            } catch (ClientProtocolException e) {
                Log.d("Error:","ClientProtocol");
            } catch (IOException e) {
                Log.d("Error:","IOException");
            }
        } catch (UnsupportedEncodingException e) {
            Log.d("Error:","UnsupportedEncodingException");
        }
        return ;
    }
















    private void send_location(){
        //To Do: Insert journey table


        Log.d(TAG, "send_station_data: 0");
        Map< String, Object > jsonValues = new HashMap< String, Object >();
        jsonValues.put("x", update_location_x);
        jsonValues.put("y", update_location_y);
        Log.d(TAG, "send_station_data: 1");
        JSONObject update_location = new JSONObject(jsonValues);


        Log.d(TAG, "send_station_data: 2");
        DefaultHttpClient client = new DefaultHttpClient();
        Log.d(TAG, "send_station_data: 3");
        HttpPost httppost = new HttpPost("http://128.199.88.79:3001/api/v1/minibus/updateLocation");
        Log.d(TAG, "send_station_data: 4");


        Log.d(TAG, "send_station_data: 5");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("location", update_location.toString()));
            nameValuePairs.add(new BasicNameValuePair("time", update_time.toString()));
            nameValuePairs.add(new BasicNameValuePair("journeyid", journeyid));
            nameValuePairs.add(new BasicNameValuePair("batteryLeft", Integer.toString(Bat_info)));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("httppost: ",httppost.toString());

            try {
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }
                Log.d(TAG, "Before send message");
                HttpResponse response =client.execute(httppost);
                Log.d(TAG, "After send message");
                Log.d("myapp", "response " + response.getEntity());

            } catch (ClientProtocolException e) {
                Log.d("Error:","ClientProtocol");
            } catch (IOException e) {
                Log.d("Error:","IOException");
            }
        } catch (UnsupportedEncodingException e) {
            Log.d("Error:","UnsupportedEncodingException");
        }
        return ;
    }












    private void send_journey_data(int route){

        Log.d(TAG, "send_journey_data: 2");
        DefaultHttpClient client = new DefaultHttpClient();
        Log.d(TAG, "send_journey_data: 3");
        HttpPost httppost = new HttpPost("http://128.199.88.79:3001/api/v1/minibus/insertJourney");
        Log.d(TAG, "send_journey_data: 4");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("journeyid", journeyid));
            nameValuePairs.add(new BasicNameValuePair("route", String.valueOf(route)));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.d("httppost: ",httppost.toString());

            try {
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }
                Log.d(TAG, "Before sending journey data");
                HttpResponse response =client.execute(httppost);
                Log.d(TAG, "After sending journey data");
                Log.d("myapp", "response " + response.getEntity());

            } catch (ClientProtocolException e) {
                Log.d("Error:","ClientProtocol");
            } catch (IOException e) {
                Log.d("Error:","IOException");
            }
        } catch (UnsupportedEncodingException e) {
            Log.d("Error:","UnsupportedEncodingException");
        }
        return ;
    }




    public void show_journey(){
        Log.e(TAG,"Sent the journey data:");
        Log.e(TAG, "Route_id = 1");
        Log.e(TAG, "Journeyid = "+journeyid);
    }

    public void show_stations() {
        Log.e(TAG, "It's time to send the stations information:");
        Log.e(TAG, "Station_id" + journeyid + send_i);
        Log.e(TAG, "Start_time = " + start_time);
        Log.e(TAG, "End_time = " + end_time);
        Log.e(TAG, "Location = " + endlocation_x + " , " + endlocation_y);
        Log.e(TAG, "Journeyid = " + journeyid);
    }


    private class mainTask extends TimerTask
    {
        public void run()
        {

            Calendar cal = Calendar.getInstance();
            Date currentLocalTime = cal.getTime();
            Log.e(TAG,"currentLocalTime = "+String.valueOf(currentLocalTime.getTime()));
            int start_plane = 0;
            int end_plane =0;
            if(currentLocalTime.getHours()==8)
            {
                start_plane=1;
            }

            if(currentLocalTime.getHours()==23)
            {
                end_plane=1;
            }
            Log.e(TAG,Integer.toString(currentLocalTime.getHours()));
            if(end_plane==1) {
                Log.e(TAG,"enter start_plane");
                int mode = 1;
                // 設定飛航模式的狀態並廣播出去
                try{
                    Settings.System.putInt(ctx.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode);
                    Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    i.putExtra("state", mode);
                    ctx.sendBroadcast(i);
                }
                catch(Exception ex)
                {
                    Log.e(TAG,"open plane");
                }
            }
            else if(start_plane==1)
            {
                try {
                    int mode = 0;
                    // 設定飛航模式的狀態並廣播出去
                    Settings.System.putInt(ctx.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode);
                    Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    i.putExtra("state", mode);
                    ctx.sendBroadcast(i);
                }
                catch(Exception ex){
                    Log.e(TAG, "close plane");
                }
            }
        }
    }

    private void startService()
    {
        timer.scheduleAtFixedRate(new mainTask(), 0, 5000);
    }
}