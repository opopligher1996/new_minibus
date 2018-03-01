package com.example.cheukleong.minibus_project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.provider.Settings.System;

import org.w3c.dom.Text;

public class MainActivity extends Activity {
    Intent mServiceIntent;
    private GPSTracker mSensorService;
    private Button start;
    private Button show;
    private Button go;
    private Button back;
    private Button Change;
    private Button airplane;
    private Button off_airplane;
    private EditText ID;
    private EditText Route;
    private TextView Show_Location;
    private TextView Dans;
    public final Context context=this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ID=findViewById(R.id.Car_ID);
        start=findViewById(R.id.startButton);
        show=findViewById(R.id.show_button);
        Change=findViewById(R.id.Change);
        go=findViewById(R.id.go);
        back=findViewById(R.id.back);
        Show_Location=findViewById(R.id.Show_Location);
        Route=findViewById(R.id.Route);
        Dans=findViewById(R.id.Dans);
        airplane=findViewById(R.id.airplane);
        off_airplane= findViewById(R.id.off_airplane);


        airplane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mode = 1;
                // 設定飛航模式的狀態並廣播出去
                Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode);
                Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                i.putExtra("state", mode);
                context.sendBroadcast(i);
            }
        });

        off_airplane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mode = 0;
                // 設定飛航模式的狀態並廣播出去
                Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, mode);
                Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                i.putExtra("state", mode);
                context.sendBroadcast(i);
            }
        });

        Change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSTracker.dans=Integer.parseInt(Dans.getText().toString());
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSTracker.CAR_ID=ID.getText().toString();
                GPSTracker.route=Route.getText().toString();
                startService(new Intent(context, GPSTracker.class));
            }
        });


        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("Car Id:",GPSTracker.CAR_ID);
                Log.e("",Double.toString(GPSTracker.endlocation_x)+", "+Double.toString(GPSTracker.endlocation_y));
                Show_Location.setText(Double.toString(GPSTracker.endlocation_x)+","+Double.toString(GPSTracker.endlocation_y));
            }
        });


        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSTracker.init=1;
                GPSTracker.go_back=0;
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSTracker.init=1;
                GPSTracker.go_back=1;
            }
        });




//        double go_station[][]={
//                {22.2831582,114.1597272},
//                {22.2841445,114.1392645},
//                {22.2836933,114.1366914},
//                {22.26823162,114.12865509},
//                {22.26642942,114.12825444},
//                {22.26208544,114.13187478}
//        };
//
//        double back_station[][]={
//                {22.26208544,114.13187478},
//                {22.266572,114.128184},
//                {22.269442,114.129753},
//                {22.2843794,114.13428},
//                {22.2831582,114.1597272}
//        };
    }
}
