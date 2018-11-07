package de.psychomechanics.android.catfinderproject;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class CatMapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    public static final int FINE_LOCATION_PERMISSION_REQUEST = 100;
    public static final String URL = "http://192.168.1.105:8080/arduino_recv/ArduinoRecvServlet";

    long startTime = 0;

    private String name_1;
    private String id_1;
    private String key_1;

    private String name_2;
    private String id_2;
    private String key_2;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            CatPosFetch task = new CatPosFetch();
            task.execute(URL);

            timerHandler.postDelayed(this, 60000);
        }
    };

    void startActivity(View view) {
        startActivity(new Intent(CatMapsActivity.this, SettingsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_maps);

        getPreferences();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> myMaps = prefs.getAll();
        name_1 = prefs.getString("ui_name_1", null);
        id_1 =  prefs.getString("ui_id_1", null);
        key_1 =  prefs.getString("ui_key_1", null);
        name_2 = prefs.getString("ui_name_2", null);
        id_2 =  prefs.getString("ui_id_2", null);
        key_2 =  prefs.getString("ui_key_2", null);
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMinZoomPreference(6.0f);
        mMap.setMaxZoomPreference(34.0f);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_PERMISSION_REQUEST);

                // FINE_LOCATION_PERMISSION_REQUEST is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        timerHandler.postDelayed(timerRunnable, 0);

        /*
         * Setting a custom info window adapter for the google map.
         *
         * An info window allows you to display information to the user when they tap on a marker.
         * Only one info window is displayed at a time.
         *
         * Info window is not a live View, rather the view is rendered as an image onto the map.
         * As a result, any listeners you set on the view (setOnInfoWindowClickListener)cannot
         * distinguish between click events on various parts of the view.
         */
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                // Getting view from the layout file info_window_layout
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                // Getting the position from the marker
                LatLng latLng = arg0.getPosition();

                // Get the reference to the ImageView to set the infoWindow image
                ImageView iv = (ImageView) v.findViewById(R.id.info_window_image);
                // Get the reference to the TextView to set latitude
                TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                tvLat.setText("Lat:" + latLng.latitude);

                // Get the reference to the TextView to set longitude
                TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);
                tvLng.setText("Lon:" + latLng.longitude);

                // Get the reference to the TextView to set the description
                TextView tvTitle = (TextView) v.findViewById(R.id.tv_title);
                tvTitle.setText(arg0.getTitle());
                // Getting reference to the TextView to set the description
                TextView tvDescription = (TextView) v.findViewById(R.id.tv_description);
                tvDescription.setText(arg0.getSnippet());
                // Returning the view containing InfoWindow contents
                return v;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private CookieManager cookieManager = new CookieManager();

    private class CatPosFetch extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getOutputFromUrl(urls[0]);
        }

        private String getOutputFromUrl(String targetUrl) {
            StringBuffer output = new StringBuffer("");
            InputStream stream = null;
            URL url = null;

            HttpURLConnection urlConnection = null;
            try {
                url = new URL(targetUrl);
                /*
                Get Cookies from cookieManager and load them into connection:
                 */
                urlConnection = (HttpURLConnection) url.openConnection();
                /*
                SET SESSION COOKIE HEADER
                Get Cookies from response header and load them into cookieManager
                */
                if (cookieManager.getCookieStore().getCookies().size() > 0) {
                    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
                    urlConnection.setRequestProperty("Cookie",
                            TextUtils.join(";", cookieManager.getCookieStore().getCookies()));
                }
                Log.d(this.getClass().getName(), TextUtils.join(";", cookieManager.getCookieStore().getCookies()));
                getPreferences();
                /*
                SET AUTAHORIZATION HEADER
                 */
                String creds = String.format("%s:%s",id_1,key_1);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                urlConnection.setRequestProperty("Authorization", auth);

                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    CookieHandler.setDefault(cookieManager);
                    Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
                    List<String> cookiesHeader = headerFields.get("Set-Cookie");
                    /*
                    Create cookie in cookieStore with JSESSIONID from received "Set-Cookie" header
                     */
                    if (cookiesHeader != null) {
                        for (String cookie : cookiesHeader) {
                            cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                        }
                    }
                    /*
                    READ RESPONSE FROM INPUTSTREAM: LAT, LNG, ALT
                    */
                    stream = urlConnection.getInputStream();
                    if (stream != null) {
                        BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
                        String s = "";
                        while ((s = buffer.readLine()) != null) output.append(s);
                        buffer.close();
                    } else {
                        // TODO: Show "Cannot connect ro server" message
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return output.toString();
        }

        @Override
        protected void onPostExecute(String output) {
            String[] data = output.split(",");
            if (data.length == 3) {
                double latitude = 0;
                double longitude = 0;
                double altitude = 0;
                try {
                    latitude = Double.parseDouble(data[0]);
                    longitude = Double.parseDouble(data[1]);
                    altitude = Double.parseDouble(data[2]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }
                LatLng catPos = new LatLng(latitude, longitude);
                // Add a marker and move the camera
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(catPos).title(name_1));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(catPos));
            }
        }
    }
}
