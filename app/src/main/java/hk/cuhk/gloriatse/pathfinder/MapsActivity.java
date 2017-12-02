package hk.cuhk.gloriatse.pathfinder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MultivaluedMap;

import hk.cuhk.gloriatse.pathfinder.model.DirectionResponse;
import hk.cuhk.gloriatse.pathfinder.model.EndLocation;
import hk.cuhk.gloriatse.pathfinder.model.Leg;
import hk.cuhk.gloriatse.pathfinder.model.OverviewPolyline;
import hk.cuhk.gloriatse.pathfinder.model.PathFinderRequest;
import hk.cuhk.gloriatse.pathfinder.model.Route;
import hk.cuhk.gloriatse.pathfinder.model.Step;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    // UI component
    private GoogleMap mMap;
    private Button btnPlan;
    ZoomControls zoom;
    private RadioGroup radioButtonCurrentMode;
    private ToggleButton toggleRefresh;



    private final static int MY_PERMISSION_FINE_LOCATION = 101;



    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    private FusedLocationProviderClient mFusedLocationClient;

    private Polyline routeDrawn;
    private Marker destinationMarker;
    private ArrayList<Marker> stepMarkers;

    Client client ;


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
    private PathFinderRequest request;


    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // setup the HTTP client
        client = Client.create();


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

        // background task to update location
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult.getLastLocation()!=null) {
                    // Update UI with location data
                    mLastKnownLocation = locationResult.getLastLocation();
                    showToast("Current location updated at " + Calendar.getInstance().getTime().toString());

                    onMyLastKnowLocationUpdated(false);

                }
            };
        };
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

    // init the buttons when ready

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

        radioButtonCurrentMode = (RadioGroup) findViewById(R.id.radioMode);
        radioButtonCurrentMode.setEnabled(false);
        radioButtonCurrentMode.getCheckedRadioButtonId();
        radioButtonCurrentMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (request != null) {
                    if (checkedId == R.id.mode_drive) {
                        request.setMode(PathFinderRequest.MODE_DRIVING);
                        showToast("Finding path for driving");
                    } else {
                        request.setMode(PathFinderRequest.MODE_WALKING);
                        showToast("Finding path for walking");
                    }

                    PathFinderHelperTask task = new PathFinderHelperTask();
                    requestPathAndRedraw(task,true);

                }
            }
        });


        toggleRefresh=  (ToggleButton) findViewById(R.id.toggleUpdate);
        toggleRefresh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled, disable other buttons
                    btnPlan.setEnabled(false);
                    radioButtonCurrentMode.setEnabled(false);


                    showToast("Start updating the path at interval of 10 seconds...");
                    LocationRequest r=  new LocationRequest();
                    int interval10S = 10000;
                    r.setInterval(interval10S);
                    r.setFastestInterval(interval10S);
                    try{
                        mFusedLocationClient.requestLocationUpdates(r, mLocationCallback, null);
                    } catch (SecurityException e)  {
                        Log.e("Exception: %s", e.getMessage());
                    }


                    
                } else {
                    // The toggle is disabled
                    showToast("Stop updating the path...");
                    // The toggle is disabled , enable other buttons
                    btnPlan.setEnabled(true);
                    radioButtonCurrentMode.setEnabled(true);

                    if (mLocationCallback!=null){
                        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    }
                }
            }
        });

        btnPlan = (Button) findViewById(R.id.btnPlan);
        btnPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PathFinderHelperTask task = new PathFinderHelperTask();
                request = new PathFinderRequest();
                TextView viewById = (TextView) findViewById(R.id.textLocation);
                request.setOrig(new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude()));
                request.setDestLocation(viewById.getText().toString());
                radioButtonCurrentMode.setEnabled(true);
                requestPathAndRedraw(task,true);


            }

        });
    }

    public static PolylineOptions createPolylineForSteps( ArrayList<LatLng> locationList,  int color) {
        PolylineOptions rectLine = new PolylineOptions().color(color).geodesic(true);
        for (LatLng location : locationList) {
            rectLine.add(location);
        }
        return rectLine;
    }

    private void requestPathAndRedraw(PathFinderHelperTask task, boolean adjustCamera) {
        try {
            DirectionResponse directionResponse = task.execute(request).get();
            System.out.println("number of routes :" + directionResponse.getRoutes().size());

            PolylineOptions lineOptions = null;

            // get the first route only
            //for (int i = 0; i < directionResponse.getRoutes().size(); i++) {
            if (directionResponse!=null && directionResponse.getRoutes().size()>0){
                if (routeDrawn!=null){
                    routeDrawn.remove();
                }
                if (destinationMarker!=null){
                    destinationMarker.remove();
                }

                if (stepMarkers!=null && stepMarkers.size()>0){
                    for (Marker m:stepMarkers){
                        m.remove();
                    }
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
                if (adjustCamera) {
                    if (points.size() > 0) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(new LatLng(route.getBounds().getNortheast().getLat(), route.getBounds().getNortheast().getLng()));
                        builder.include(new LatLng(route.getBounds().getSouthwest().getLat(), route.getBounds().getSouthwest().getLng()));
                        int padding = 50;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
                    }
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                }

                lineOptions.color(Color.RED);


                // display the steps
                if (stepMarkers==null){
                    stepMarkers = new ArrayList<Marker>();
                }
                Leg leg = route.getLegs().get(0);


                if (toggleRefresh.isChecked() ){
                    if (leg.getSteps().size()>0) {
                        // just show the first few  markers
                        Step s = leg.getSteps().get(0);
                        Marker marker = addStepMakerOnMap(s);
                        stepMarkers.add(marker);
                    }
                    if (leg.getSteps().size()>1) {
                        Step s = leg.getSteps().get(1);
                        Marker marker = addStepMakerOnMap(s);
                        stepMarkers.add(marker);
                    }

                } else {
                    for (Step s: leg.getSteps()){
                        Marker marker = addStepMakerOnMap(s);
                        stepMarkers.add(marker);

                    }
                }
                if (stepMarkers.size()>0){

                    stepMarkers.get(0).showInfoWindow();
                }







                // add the marker on the destination
                MarkerOptions markerOptions = new MarkerOptions();
                EndLocation endLocation = route.getLegs().get(0).getEndLocation();
                String endAddress = route.getLegs().get(0).getEndAddress();
                markerOptions.position(new LatLng(endLocation.getLat(),endLocation.getLng()));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                markerOptions.title("Go to " + endAddress);
                markerOptions.snippet(route.getSummary());
                destinationMarker = mMap.addMarker(markerOptions);
                destinationMarker.showInfoWindow();
                routeDrawn = mMap.addPolyline(lineOptions);


            } else {
                showToast("Cannot find the route to " +  request.getDestLocation());
            }




        } catch (InterruptedException e) {
           Log.e(TAG,"Error in drawing the route",e);
        } catch (ExecutionException e) {
            Log.e(TAG,"Error in drawing the route",e);
        }
    }

    private Marker addStepMakerOnMap(Step s) {
        MarkerOptions stepMarker = new MarkerOptions();

        stepMarker.snippet(s.getHtmlInstructions());
        stepMarker.position(new LatLng(s.getStartLocation().getLat(),s.getStartLocation().getLng()));
        stepMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

        return mMap.addMarker(stepMarker);
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


                Spanned spannedContent = Html.fromHtml(marker.getSnippet(), FROM_HTML_MODE_LEGACY);
                snippet.setText(spannedContent, TextView.BufferType.SPANNABLE);

                
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
                showToast("Getting current location...");
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult()!=null) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            onMyLastKnowLocationUpdated(true);


//                            MarkerOptions startOpts= new MarkerOptions();
//                            startOpts.position(new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude()));
//                            startOpts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//                            startingPointMarker = mMap.addMarker(startOpts);

                        } else {
                            showToast("Cannot get current location, using default location...");
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                    }
                });
            } else {
                showToast("Please turn on the GPS! No location permission granted");

            }
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    showToast("Redrawing the path from current location");
                    PathFinderHelperTask task = new PathFinderHelperTask();
                    getDeviceLocation();

                    return true;
                }

            });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void onMyLastKnowLocationUpdated(boolean adjustCamera) {
        if (request!=null) {
            PathFinderHelperTask pathFinderHelperTask = new PathFinderHelperTask();
            request.setOrig(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            requestPathAndRedraw(pathFinderHelperTask, adjustCamera);
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }
    }


    private void showToast(String shortMessage) {
       showToast(shortMessage,Toast.LENGTH_SHORT);


    }
    private void showToast(String shortMessage, int duration) {
        Context context = getApplicationContext();
        CharSequence text = shortMessage;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
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
            map.add("mode",request.getMode() );


            ClientResponse response = webResource
                    .queryParams(map)
                    .get(ClientResponse.class);


            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            } else {

                String output = response.getEntity(String.class);

                Log.i(TAG, "output from server:" + output);

                directionResponse = DirectionsJSONParser.parse(output);
            }
            return directionResponse;
        }

        protected void onPostExecute(DirectionResponse response) {
            System.out.println("response status:" + response.getStatus());
        }
    }



}