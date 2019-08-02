package com.headspire.googlemapstest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MapActivity class contains the code for loading searching and get the current location of the client.
 */
public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener,
        ResultCallback<Status>
{
    private static final String TAG = "MapActivity";
    private GoogleMap mgoogleMap;
    private EditText address;
    private Address searchAddress;
    private static final float DEFAULT_ZOOM = 15f;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeofencingClient geofencingClient;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LatLng currentLatLng;
    private LatLng latLng;

    private String mAddress;
    private String mCity;
    private String mState;
    private String mCountry;
    private String mPostalCode;


    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL =  1000;
    private final int FASTEST_INTERVAL = 900;

    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 50.0f; // in meters
    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        address=findViewById(R.id.textlocation);
        geofencingClient= LocationServices.getGeofencingClient(this);
        if(!checkPermission())
            getPermission();
        LocationManager locationManager= (LocationManager) getSystemService(LOCATION_SERVICE);
        googleMapApi();
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            turnOnGps();
        //initializing the map
        initMap();
    }

    //popup dialog if the user turnoff the GPS....
    private void turnOnGps() {
        Log.d(TAG, "turnOnGps: called");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MapActivity.this, GEOFENCE_REQ_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    private void googleMapApi() {
        Log.d(TAG, "googleMapApi: called");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }
    //override the enter key
    private void init()
    {
        Log.d(TAG, "init: called");
        address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_SEARCH
                || actionId==EditorInfo.IME_ACTION_DONE
                || event.getAction()==KeyEvent.ACTION_DOWN
                || event.getAction()==KeyEvent.KEYCODE_ENTER)
                {
                    Log.d("tagg","success");
                    geoLocate();
                }
                return false;
            }
        });
    }

    //searching the string to get latitude and longitude and other information
    private void geoLocate()
    {
        Log.d(TAG, "geoLocate: called");
        String searchString=address.getText().toString();
        Geocoder geocoder=new Geocoder(MapActivity.this);
        List<Address> addresses=new ArrayList<>();
        try
        {
            addresses=geocoder.getFromLocationName(searchString,1);
            if(addresses.size()>0)
            {
                searchAddress=addresses.get(0);
                moveCamera(new LatLng(searchAddress.getLatitude(),searchAddress.getLongitude()),DEFAULT_ZOOM
                ,searchAddress.getAddressLine(0));
                markerForGeofence(new LatLng(searchAddress.getLatitude(),searchAddress.getLongitude()));
            }
        }
        catch (Exception e)
        {Log.e("tagg",e.getMessage());}
    }

    /**
     * initializing the google map
     */
    private void initMap() {
        Log.d(TAG, "initMap: called");
        getPermission();
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: called");
        mgoogleMap = googleMap;
        mgoogleMap.setOnMapClickListener(this);
        mgoogleMap.setOnMarkerClickListener(this);

        Geofence geofence=new Geofence.Builder().setRequestId("212")
                .setCircularRegion(30.3165,78.0322,500)
                .setExpirationDuration(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                |Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        GeofencingRequest geofencingRequest=new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
        getCurrentLocation();
        if (!checkPermission()) {
            mgoogleMap.setMyLocationEnabled(true);
        }
        else
            getPermission();
        init();
        //****************************
    }

    /**
     * moveCamera will move the screen to searched location in the map
     * @param latLng object will have the longitude and latitude
     * @param zoom floating value for display map
     * @param title string that will displayed when the user click on the marker
     */
    private void moveCamera(LatLng latLng,float zoom,String title)
    {
        Log.d(TAG, "moveCamera: called");
        currentLatLng=latLng;
        mgoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        if(!title.equals("your location")) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mgoogleMap.addMarker(markerOptions);
        }
    }

    /**
     * gives the current position of the user.
     */
    public void getCurrentLocation() {
        Log.d(TAG, "getCurrentLocation: called");
        fusedLocationProviderClient = new FusedLocationProviderClient(this);
        LocationRequest mlocationRequest = new LocationRequest();
        mlocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(!checkPermission())
        {
            fusedLocationProviderClient.requestLocationUpdates(mlocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);


                    currentLatLng=new LatLng(locationResult.getLastLocation().getLatitude()
                    ,locationResult.getLastLocation().getLongitude());


                    moveCamera(new LatLng(locationResult.getLastLocation().getLatitude()
                    ,locationResult.getLastLocation().getLongitude()),DEFAULT_ZOOM,"your location");
                    markerForGeofence(new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()));
                    latLng=new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());
                }
            }, getMainLooper());
        }
        else
            getPermission();
    }
    LatLng getLastLocation()
    {
        Log.d(TAG, "getLastLocation: called");
        final LatLng[] latlng = {new LatLng(0.0, 0.0)};
        fusedLocationProviderClient = new FusedLocationProviderClient(this);
        LocationRequest mlocationRequest = new LocationRequest();
        mlocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(!checkPermission())
        {
            fusedLocationProviderClient.requestLocationUpdates(mlocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    latlng[0] =new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                }
            }, getMainLooper());
        }
        else
            getPermission();
        return latlng[0];
    }
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: called");
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called");
        super.onStop();
        googleApiClient.disconnect();
    }

    /**
     * request for the required permissions
     */
    public void getPermission() {
        Log.d(TAG, "getPermission: called");
            if(checkPermission())
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container,new PermissionFragment())
                            .addToBackStack(null)
                            .commit();
    }

    /**
     * check the required permission are given or not.
     * @return true if permission not given and false if permission are given
     */
    public boolean checkPermission()
    {
        Log.d(TAG, "checkPermission: called");
        if (ContextCompat.checkSelfPermission(getApplicationContext()
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick: called");
        Log.d(TAG,latLng.latitude+"::"+latLng.longitude);
        this.latLng=new LatLng(latLng.latitude,latLng.longitude);
        markerForGeofence(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick: called");
        Log.d("mapStatus",marker.getPosition()+"::");
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: called");
        if(!checkPermission())
            getPermission();
        getLastKnownLocation();
        recoverGeofenceMarker();
    }

    private void recoverGeofenceMarker() {
        Log.d(TAG, "recoverGeofenceMarker: called");
        SharedPreferences sharedPreferences=getPreferences(Context.MODE_PRIVATE);
        if(sharedPreferences.contains("lat") && sharedPreferences.contains("long"))
        {
            double lat=Double.longBitsToDouble(sharedPreferences.getLong("lat",-1));
            double longitude=Double.longBitsToDouble(sharedPreferences.getLong("long",-1));
            LatLng latLng=new LatLng(lat,longitude);
            Log.e(TAG,"recoverGeofence");
            markerForGeofence(latLng);
            drawGeofence();
        }
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else getPermission();
    }
    private void writeLastLocation() {
        Log.d(TAG, "writeLastLocation: called");
        writeActualLocation(lastLocation);
    }
    private void writeActualLocation(Location location) {
        // ...
        Log.d(TAG, "writeActualLocation: called");
        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private Marker locationMarker;
    // Create a Location Marker
    private void markerLocation(LatLng latLng) {
        Log.d(TAG, "markerLocation: called");
        Log.i(TAG, "markerLocation("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( mgoogleMap!=null ) {
            // Remove the anterior marker
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = mgoogleMap.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            mgoogleMap.animateCamera(cameraUpdate);
        }
    }


    private void startLocationUpdates(){
        Log.d(TAG, "startLocationUpdates: called");
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private Marker geoFenceMarker;
    // Create a marker for the geofence creation
    private void markerForGeofence(LatLng latLng) {
        Log.d(TAG, "markerForGeofence: called");
        Log.i(TAG, "markerForGeofence(" + latLng + ")");
        //String title = latLng.latitude + ", " + latLng.longitude;
        getCompleteAddress(latLng);
        String[] address=mAddress.split(",");

        String title=address[1]+","+sector+","+mCity+","+mState+","+mPostalCode;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if (mgoogleMap != null) {
            // Remove last geoFenceMarker
            if (geoFenceMarker != null)
                geoFenceMarker.remove();

            geoFenceMarker = mgoogleMap.addMarker(markerOptions);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: called");
        lastLocation=location;
        markerForGeofence(new LatLng(location.getLatitude(),location.getLongitude()));
        writeActualLocation(location);
    }

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius ) {
        Log.d(TAG, "createGeofence: called");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( Geofence.NEVER_EXPIRE )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }
    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( Geofence geofence ) {
        Log.d(TAG, "createGeofenceRequest: called");
        Toast.makeText(this,"geo fence added"+geofence.getRequestId(),Toast.LENGTH_SHORT).show();
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }


    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent: called");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Log.e(TAG,"not null");
        Intent intent = new Intent( this, GeofenceBroadcast.class);
        geoFencePendingIntent=PendingIntent.getBroadcast(this,0,intent
        ,PendingIntent.FLAG_UPDATE_CURRENT);
        return geoFencePendingIntent;
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence: called");
        if (!checkPermission())
            geofencingClient.addGeofences(request,createGeofencePendingIntent())
            .addOnSuccessListener(this,new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG,"success");
                }

            }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG,"fail");
                }
            });
//            LocationServices.GeofencingApi.addGeofences(
//                    googleApiClient,
//                    request,
//                    createGeofencePendingIntent()
//            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.d(TAG, "onResult: called");
        if(status.isSuccess())
        {
            saveGeofence();
            drawGeofence();
        }else
        {
        }
    }
    private Circle geofenceLimits;

    private void drawGeofence() {
        Log.d(TAG, "drawGeofence: called");
        if(geofenceLimits!=null)
            geofenceLimits.remove();

        CircleOptions circleOptions=new CircleOptions()
                .center(geoFenceMarker.getPosition())
               // .strokeColor(Color.argb(50,70,70,70))
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(100,150,150,150))
                .radius(GEOFENCE_RADIUS);
        geofenceLimits=mgoogleMap.addCircle(circleOptions);
    }

    private void saveGeofence() {
        Log.d(TAG, "saveGeofence: called");
        SharedPreferences sharedPreferences=getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putLong("lat",Double.doubleToRawLongBits(geoFenceMarker.getPosition().latitude));
        editor.putLong("long",Double.doubleToRawLongBits(geoFenceMarker.getPosition().longitude));
        editor.apply();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: called");
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: called");
        switch (item.getItemId())
        {
            case R.id.geofence: {
                startGeofence();
                drawGeofence();
                return true;
            }
               // break;
            case R.id.clear:{
                clearGeofence();
                return true;
            }
        //        break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearGeofence() {
        Log.d(TAG, "clearGeofence: called");
        LocationServices.GeofencingApi.removeGeofences(googleApiClient
        ,createGeofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess())
                        {
                            removeGeofenceDraw();
                        }
                    }
                });
    }
    private void removeGeofenceDraw()
    {
        Log.d(TAG, "removeGeofenceDraw: called");
        if(geoFenceMarker!=null)
        {
            geoFenceMarker.remove();
        }
        if(geofenceLimits!=null)
            geofenceLimits.remove();
    }

    private void startGeofence() {
        Log.d(TAG, "startGeofence: called");
        try {
            //createGeofenceRequest(createGeofence(latLng, 50));
          //  Log.e("Location",currentLatLng.latitude+"");
            addGeofence(createGeofenceRequest(createGeofence(latLng, GEOFENCE_RADIUS)));
        }
        catch (Exception e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    String sector;
    private void getCompleteAddress(LatLng latLng)
    {
        Log.d(TAG, "getCompleteAddress: called.");
        Geocoder geocoder;
        List<Address> addresses;
        geocoder=new Geocoder(this, Locale.getDefault());
        mAddress=new String();
        mCountry=new String();
        mCity=new String();
        mPostalCode=new String();
        mState=new String();
        sector=new String();
        try {
            addresses=geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            mAddress=addresses.get(0).getAddressLine(0);
           // mCity=addresses.get(0).getLocality();
            mCity=addresses.get(0).getSubAdminArea();
            mState=addresses.get(0).getAdminArea();
            mCountry=addresses.get(0).getCountryName();
            mPostalCode=addresses.get(0).getPostalCode();
            sector=addresses.get(0).getSubLocality();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
