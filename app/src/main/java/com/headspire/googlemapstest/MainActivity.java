package com.headspire.googlemapstest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.Task;

/**
 * @version 1.0
 * created by Ashish Yadav 07-03-2019
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST=5;
    private Button gotomap;
    private Button exit;

    public static Intent makeNotificationIntent(Context applicationContext, String msg) {
        Intent intent=new Intent(applicationContext,MainActivity.class);
        intent.putExtra("NOTIFICATION_MSG",msg);
        return intent;
    }


    //***code for google maps
    public boolean isGoogleServiceAvailable()
    {
        Log.d(TAG, "isGoogleServiceAvailable: checking google service version");
        int available= GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if(available== ConnectionResult.SUCCESS)
        {
            Log.d(TAG, "google play service is working.");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available))
        {
            Log.d(TAG, "error can be resolved..");
            Dialog dialog=GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this,available,ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else
            Toast.makeText(this,"you cann't make a map request.",Toast.LENGTH_SHORT).show();
        return false;
    }

    private void startAction() {
        Intent intent=new Intent(MainActivity.this,MapActivity.class);
        Bundle bundle=new Bundle();
        startActivity(intent);

    }
    //************************




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gotomap=findViewById(R.id.gotomap);
        exit=findViewById(R.id.exit);
        exit.setOnClickListener(this);
        gotomap.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gotomap:
                if(isGoogleServiceAvailable())
                {
                    startAction();
                }
            case R.id.exit:
                finish();
        }
    }

}
