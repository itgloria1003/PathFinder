package hk.cuhk.gloriatse.pathfinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;

import hk.cuhk.gloriatse.pathfinder.model.DirectionResponse;
import hk.cuhk.gloriatse.pathfinder.model.EndLocation;
import hk.cuhk.gloriatse.pathfinder.model.OverviewPolyline;
import hk.cuhk.gloriatse.pathfinder.model.PathFinderRequest;
import hk.cuhk.gloriatse.pathfinder.model.Route;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    // UI component
    private GoogleMap mMap;
    private Button btnPlan;
    ZoomControls zoom;



    private final static int MY_PERMISSION_FINE_LOCATION = 101;



    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    private FusedLocationProviderClient mFusedLocationClient;

    private Polyline routeDrawn;
    private Marker destinationMarker;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 16;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;




    private Parcelable mCameraPosition;
    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }






    private void initControlButtons() {
        zoom = (ZoomControls) findViewById(R.id.zoomButton);
        zoom.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());

            }
        });
        zoom.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());

            }
        });

        btnPlan = (Button) findViewById(R.id.btnPlan);
        btnPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PathFinderHelperTask task = new PathFinderHelperTask();
                PathFinderRequest request = new PathFinderRequest();
                TextView viewById = (TextView) findViewById(R.id.textLocation);
                request.setOrig(new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude()));
                request.setDestLocation(viewById.getText().toString());

                try {
                    DirectionResponse directionResponse = task.execute(request).get();
                    System.out.println("number of routes :" + directionResponse.getRoutes().size());
                    PolylineOptions lineOptions = null;

                    // get the first route only
                    //for (int i = 0; i < directionResponse.getRoutes().size(); i++) {
                    if (directionResponse.getRoutes().size()>0){
                        if (routeDrawn!=null){
                            routeDrawn.remove();
                        }
                        if (destinationMarker!=null){
                            destinationMarker.remove();
                        }

                        lineOptions = new PolylineOptions();

                        // Fetching i-th route
                        Route route = directionResponse.getRoutes().get(0);


                        OverviewPolyline overviewPolyline = route.getOverviewPolyline();
                        // encoded overview_ployline
                        List<LatLng> points = PolyUtil.decode(overviewPolyline.getPoints());
                        for (LatLng point: points){
                            lineOptions.add(point);
                        }


                        lineOptions.color(Color.RED);
                        MarkerOptions markerOptions = new MarkerOptions();
                        EndLocation endLocation = route.getLegs().get(0).getEndLocation();
                        String endAddress = route.getLegs().get(0).getEndAddress();
                        markerOptions.position(new LatLng(endLocation.getLat(),endLocation.getLng()));
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        markerOptions.title("Go to " + endAddress);
                        markerOptions.snippet(route.getSummary());
                        destinationMarker = mMap.addMarker(markerOptions);
                        routeDrawn = mMap.addPolyline(lineOptions);


                    }




                } catch (InterruptedException e) {
                   Log.e(TAG,"Error in drawing the route",e);
                } catch (ExecutionException e) {
                    Log.e(TAG,"Error in drawing the route",e);
                }


            }

        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.instruction_details, null);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });


        // init the control of the buttons
        initControlButtons();

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();


    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult()!=null) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    /// Async Task

    private class PathFinderHelperTask extends AsyncTask<PathFinderRequest,Void,DirectionResponse> {
        private DirectionResponse directionResponse;

        @Override
        protected DirectionResponse doInBackground(PathFinderRequest... requests) {
            //call the api https://developers.google.com/maps/documentation/directions/intro#DirectionsRequests

            Client client = Client.create();

            String responseFormat = "json";
            WebResource webResource = client
                    .resource("https://maps.googleapis.com/maps/api/directions/" + responseFormat);
            MultivaluedMap map = new MultivaluedMapImpl();
            PathFinderRequest request = requests[0];
            if (request.getOrig()!=null){
                map.add("origin",request.getOrig().latitude + "," + request.getOrig().longitude);
            } else {
                map.add("origin",request.getOrigLocation());
            }

            map.add("destination",request.getDestLocation());


            ClientResponse response = webResource
                    .queryParams(map)
                    .get(ClientResponse.class);


            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            String output = response.getEntity(String.class);

            Log.i(TAG,"output from server:" + output);

            directionResponse = DirectionsJSONParser.parse(output);
            return directionResponse;
        }

        protected void onPostExecute(DirectionResponse response) {
            System.out.println("response status:" + response.getStatus());
        }
    }



}