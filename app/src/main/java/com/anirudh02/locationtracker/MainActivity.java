package com.anirudh02.locationtracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //Create objects that will be used in defined methods below
    private GoogleMap map;
    private SensorManager sensorManager;
    private Sensor sensor;
    private float azimuth;
    float[] orientation;
    float[] mat;
    SupportMapFragment mf;
    CheckBox cb;
    CheckBox enable;
    ImageView compass;
    TextView direction;
    Button start_track;
    Button stop_track;
    Button view_track;
    boolean viewing;
    MarkerOptions marker;
    myDBHelper myHelper;
    SQLiteDatabase sqlDB;
    TextView visible;
    Marker m;
    boolean tracking;
    ImageButton currLocation;
    List<Polyline> polylines;
    List<Polyline> savedPolylines;
    List<PolylineOptions> lines;
    int count;
    Double latitude;
    Double longitude;
    Double lastLatitude;
    Double lastLongitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Location Tracker");
        //Ask for permission to access fine location, read external storage, and write external storage if permission is yet to be granted
        ArrayList<String> a1 = new ArrayList<>();
        count = 0;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            a1.add(Manifest.permission.ACCESS_FINE_LOCATION);
            count++;
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            a1.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            count++;
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            a1.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            count++;
        }
        if(count > 0) {
            Log.e("Count", String.valueOf(count));
            String[] arr = new String[a1.size()];
            for(int i = 0; i < a1.size(); i++) {
                arr[i] = a1.get(i);
            }
            ActivityCompat.requestPermissions(this, arr, 8080);
        }
        //Initialize objects
        azimuth = 0;
        marker = new MarkerOptions();
        orientation = new float[3];
        mat = new float[9];
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR); //This sensor is a jack of all trades;
        // it uses the accelerometer, gyroscope, and magnetometer to retrieve data orientation information.
        cb = (CheckBox) findViewById(R.id.type);
        enable = (CheckBox) findViewById(R.id.enable);
        enable.setVisibility(View.INVISIBLE); //This checkbox will only be visible when tracking is enabled
        compass = (ImageView) findViewById(R.id.compass);
        polylines = new ArrayList<Polyline>();
        savedPolylines = new ArrayList<Polyline>();
        visible = (TextView) findViewById(R.id.tracking);
        visible.setVisibility(View.INVISIBLE);
        currLocation = (ImageButton) findViewById(R.id.currLocation);
        start_track = (Button) findViewById(R.id.start_track);
        stop_track = (Button) findViewById(R.id.stop_track);
        view_track = (Button) findViewById(R.id.view_track);
        tracking = false;
        count = 0;
        latitude = 0.0;
        longitude = 0.0;
        lastLatitude = 0.0;
        lastLongitude = 0.0;
        viewing = false;
        myHelper = new myDBHelper(this);
        direction = (TextView) findViewById(R.id.direction);
        //Initialize Google Map
        mf = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mf.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.getUiSettings().setAllGesturesEnabled(true); //allow all gestures (zoom, scroll, etc.)
                map.getUiSettings().setCompassEnabled(false); //disable the compass that Google Maps provides as I have created my own
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL); //initially set the map type as normal
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //change map type based on checkbox
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(cb.isChecked()) {
                            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        } else {
                            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                    }
                });
            }
        });
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        startLocationService(); //Start the location service
    }
    //After requesting the user to allow permissions in the onCreate method
    // (should they not already be accepted), this method is called (implicitly) immediately after.
    // The method checks to see if the user allowed all permissions. If even one permission was
    // not allowed by the user, another activity will be launched to let the user know that all
    // permissions need to be enabled in order for them to use the app. However, if all
    //permissions were accepted, the app will be restarted so that it can run properly.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if(requestCode == 8080) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED ||ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Intent i = new Intent(getApplicationContext(), MainActivity2.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //These flags are to ensure
                //the activities in the bottom of the stack get removed. When the user presses the back button once the MainActivity reloads,
                //they should be sent to their home screen, not the MainActivity page before it was reloaded. These flags will ensure this happens.
                startActivity(i);
            } else {
                Intent j = new Intent(getApplicationContext(), this.getClass());
                j.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(j);
            }

        }
    }
    //This method starts the location service. It initializes an instance of the GPSListener that is created below
    //and initializes a LocationManager object. It also requests location updates for when the user moves.
    private void startLocationService() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        GPSListener gpsListener = new GPSListener();
        long minTime = 1000;
        float minDistance = 0;
        try {
            manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime,
                    minDistance,
                    gpsListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //This method uses the TYPE_ROTATION_VECTOR sensor to calculate the azimuth value in degrees whenever the sensor is changed.
    // The azimuth value indicates how much the compass should rotate. Reference used - https://www.deviantdev.com/journal/android-compass-azimuth-calculating
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mat, event.values);
            azimuth = (float) (Math.toDegrees(SensorManager.getOrientation(mat, orientation)[0]) + 360) % 360;
            //Got degree ranges from here - https://www7.ncdc.noaa.gov/climvis/help_wind.html
            if ((azimuth >= 0 && azimuth <= 11) || (azimuth >= 349 && azimuth <= 360))
                direction.setText(R.string.north);
            if (azimuth >= 12 && azimuth <= 33)
                direction.setText(R.string.northnortheast);
            if (azimuth >= 34 && azimuth <= 56)
                direction.setText(R.string.northeast);
            if (azimuth >= 57 && azimuth <= 78)
                direction.setText(R.string.eastnortheast);
            if (azimuth >= 79 && azimuth <= 101)
                direction.setText(R.string.east);
            if (azimuth >= 102 && azimuth <= 123)
                direction.setText(R.string.eastsoutheast);
            if (azimuth >= 124 && azimuth <= 146)
                direction.setText(R.string.southeast);
            if (azimuth >= 147 && azimuth <= 168)
                direction.setText(R.string.southsoutheast);
            if (azimuth >= 169 && azimuth <= 191)
                direction.setText(R.string.south);
            if (azimuth >= 192 && azimuth <= 213)
                direction.setText(R.string.southsouthwest);
            if (azimuth >= 214 && azimuth <= 236)
                direction.setText(R.string.southsouthwest);
            if (azimuth >= 237 && azimuth <= 258)
                direction.setText(R.string.westsouthwest);
            if (azimuth >= 259 && azimuth <= 281)
                direction.setText(R.string.west);
            if (azimuth >= 282 && azimuth <= 303)
                direction.setText(R.string.westnorthwest);
            if (azimuth >= 304 && azimuth <= 326)
                direction.setText(R.string.northwest);
            if (azimuth >= 327 && azimuth <= 348)
                direction.setText(R.string.northnorthwest);
            compass.setRotation(azimuth);
        }
    }
    //Blank method
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //Register sensor listener in this method (when app is active)
    @Override
    public void onResume() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }
    //Unregister sensor listener in this method (when app is inactive)
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, sensor);
    }
    //GPSListener class implemented here inside of MainActivity class so that the map and other objects can still be referenced
    private class GPSListener implements LocationListener {

        //This method is run when the location is changed (i.e. the user moves from point A to point B).
        @Override
        public void onLocationChanged(Location location) {
            //store previous and current latitude and longitude values in variables
            lastLatitude = latitude;
            lastLongitude = longitude;
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            //Create Latitude/Longitude object with current latitude and longitude
            LatLng curPoint = new LatLng(latitude, longitude);
            //Execute method to show the user's current location with a marker. Pass in current Latitude and Longitude as a parameter.
            showcurrentLocation(curPoint);
            if(tracking) { //if tracking is on, call the drawPath() method to draw a path from the user's last Latitude/Longitude position
                //to current Latitude/Longitude position
                drawPath();
            } else { //otherwise if tracking is off, remove all the polylines that were used to track the user's position from the map.
                //the polylines were stored in an ArrayList as they were created. This makes it easy to identify and remove them from the map
                //as needed.
                for(int i = 0; i < polylines.size(); i++) {
                    polylines.get(i).remove();
                }
                polylines.clear();
            }
            //if the user enables tracking
            start_track.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //update boolean value and send toast stating that tracking has started
                    tracking = true;
                    Toast.makeText(getApplicationContext(), R.string.started, Toast.LENGTH_SHORT).show();
                    visible.setVisibility(View.VISIBLE); //display text that states tracking has started on top of screen.
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 16)); //move to the user's position on the map.
                    enable.setVisibility(View.VISIBLE); //display checkbox to enable/disable scrolling and zooming
                }
            });
            //if tracking is disabled by user
            stop_track.setOnClickListener(new View.OnClickListener() {
                //update boolean value and send toast stating that tracking has stopped.
                @Override
                public void onClick(View view) {
                    tracking = false;
                    Toast.makeText(getApplicationContext(), R.string.stopped, Toast.LENGTH_SHORT).show();
                    visible.setVisibility(View.INVISIBLE); //make text that says "Tracking in progress" disappear
                    enable.setVisibility(View.INVISIBLE); //make checkbox for scrolling/zooming disappear
                }
            });
            //if user wants to view the GPS Logs (or not)
            view_track.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //update the variable that indicates whether or not the user is viewing GPS logs.
                    viewing = !viewing;
                    //if the user is viewing the GPS logs, highlight the view button and send a toast that says GPS vlogs are being viewed.
                    if(viewing) {
                        view_track.setBackgroundColor(Color.RED);
                        Toast.makeText(getApplicationContext(), R.string.info, Toast.LENGTH_SHORT).show();
                        //retrieve all lines that were drawn during tracking sessions. They were stored in a database.
                        //display the lines.
                        lines = myHelper.getAllPoints();
                        for(int i = 0; i < lines.size(); i++) {
                            Polyline line = map.addPolyline(lines.get(i));
                            savedPolylines.add(line);
                        }
                    } else { //if user is not viewing GPS logs
                        //remove the polylines from the map and revert button color back to its inital color.
                        view_track.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                        Toast.makeText(getApplicationContext(), R.string.info2, Toast.LENGTH_SHORT).show();
                        for(int i = 0; i < savedPolylines.size(); i++) {
                            savedPolylines.get(i).remove();
                        }
                        savedPolylines.clear();
                    }
                }
            });
            //if the button to allow scrolling/zooming during tracking is not checked, keep the focus on the current location at all times.
            //even if the user tries to scroll or zoom, they will be brought back to their current location.
            if(enable.getVisibility() == View.VISIBLE && enable.isChecked() == false) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 16));
            }
        }
        //this method shows the current location of the user.
        private void showcurrentLocation(LatLng curPoint) {
            //If the button to show the current location is pressed, zoom into the map and show the location.
            //This button can be pressed multiple times and the most accurate current location will be shown.
            currLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 16));
                }
            });
            //If the marker to show the current position is not initializzed, initialize it and add it to the map.
            if(m == null) {
                marker.position(new LatLng(latitude + 0.001, longitude + 0.001));
                marker.draggable(true);
                marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                m = map.addMarker(marker);
            } else { //otherwise, update the marker to the current position of the user.
                m.setPosition(curPoint);
            }
        }
        //this method draws the path of the user when tracking is enabled. Since we stored the last Latitude/Longitude and current Latitude/Longitude
        //in variables, we can easily create a Polyline and add it to the map. We will also store the last and current latitude/longitude
        //in the database so the Polylines can be viewed when the button to view GPS logs is pressed.
        private void drawPath() {
            PolylineOptions line =
                    new PolylineOptions().add(new LatLng(lastLatitude, lastLongitude),
                            new LatLng(latitude, longitude))
                    .width(5).color(Color.RED);
           Polyline p = map.addPolyline(line);
           polylines.add(p);
           myHelper.addPoints(lastLatitude, lastLongitude, latitude, longitude);
        }
        //empty methods
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}