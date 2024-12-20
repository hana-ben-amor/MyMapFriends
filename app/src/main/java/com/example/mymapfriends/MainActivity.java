package com.example.mymapfriends;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mymapfriends.model.Position;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PHONE_CALL = 1;
    private GoogleMap mMap;
    private Marker friendMarker;
    private List<LatLng> routePoints = new ArrayList<>();  // List to track the route (optional)
    private Polyline routePolyline = null;  // Polyline for the route (optional)

    private SmsReceiver smsReceiver = new SmsReceiver();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final long MIN_TIME=1000;
    private final long MIN_DIST=5;
    private LocationListener locationListener;
    private LatLng latLng;
    private LocationManager locationManager;
    private List<LatLng> predefinedLocations = new ArrayList<>();
    private int currentLocationIndex = 0;
    private Polyline currentPolyline; // Polyline to show the path
    private Marker currentMarker;
    private Marker startMarker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize predefined locations (static points)
        predefinedLocations.add(new LatLng(33.55, 9.5375));  // Sousse
        predefinedLocations.add(new LatLng(33.58, 9.56));
        predefinedLocations.add(new LatLng(33.60, 9.57));

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                // Move camera to the user's current position
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                if (currentMarker != null) {
                    currentMarker.remove();  // Remove previous marker
                }
                currentMarker=mMap.addMarker(new MarkerOptions().position(latLng).title("My position"));

                // Send SMS with the current position
                sendPositionSMS(location);

                // Draw path if moving to the next predefined location
                drawPathBetweenAllLocations();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(positionReceiver,
               new IntentFilter("com.example.mymapfriends.POSITION_RECEIVED"));

        String sender = getIntent().getStringExtra("sender");
        String message = getIntent().getStringExtra("message");

        if (sender != null && message != null) {
            // Example: Display the received SMS in a Toast
            Toast.makeText(this, "Message from " + sender + ": " + message, Toast.LENGTH_LONG).show();
        }

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

    }
    private void sendPositionSMS(Location location) {
        String phone = "+15551234567";
        String myLatitude = String.valueOf(location.getLatitude());
        String myLongitude = String.valueOf(location.getLongitude());
        String message = "My position is: #" + myLatitude + "#" + myLongitude;
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
    }




    // BroadcastReceiver to handle position updates
    private BroadcastReceiver positionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract the position data from the Intent
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);
            String name = intent.getStringExtra("name");

            // Add a marker on the map
            if (mMap != null) {
                LatLng position = new LatLng(latitude, longitude);
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(name));
                if (marker != null) {
                    marker.setTag(name);
                }
                // Optionally, move camera to the new marker
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        // Request location updates (you may already have these in your code)
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates when the activity is paused
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Register the receiver to listen for incoming SMS messages
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister the receiver when the activity is no longer visible
        unregisterReceiver(smsReceiver);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        fetchPositionsFromServer();
        // Move camera to a starting position (e.g., Sousse)
        LatLng startLocation = predefinedLocations.get(0);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 12));

        // Add a marker for the starting position
        startMarker=mMap.addMarker(new MarkerOptions().position(startLocation).title("Start"));

        // Start simulating the movement from the first predefined location
        simulateMovement();
        mMap.setOnMarkerClickListener(marker -> {
            // Get position data for the marker (ensure marker.getTag() contains the Position object)
            Position position = (Position) marker.getTag();

            if (position != null) {
                // Show the info window when a marker is clicked
                marker.showInfoWindow();

                // Show the action dialog (implement showMarkerActionDialog as needed)
                showMarkerActionDialog(marker, position);
            }

            return true; // Prevent default behavior (like zooming or moving the camera)
        });

        // Handle map click to show popup
        mMap.setOnMapClickListener(latLng -> showPopup(latLng));


    // Handle map click to show popup
    }
    private void simulateMovement() {
        if (currentLocationIndex < predefinedLocations.size()) {
            LatLng nextLocation = predefinedLocations.get(currentLocationIndex);

            // Move camera to the next location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nextLocation, 12));

            // Add a marker at the next location
            mMap.addMarker(new MarkerOptions().position(nextLocation).title("Next location"));

            // If not the first location, draw a path between the previous and current location
            if (currentLocationIndex > 0) {
                LatLng previousLocation = predefinedLocations.get(currentLocationIndex - 1);
                if (currentPolyline != null) {
                    currentPolyline.remove(); // Remove the previous path
                }
                currentPolyline = mMap.addPolyline(new PolylineOptions()
                        .add(predefinedLocations.get(0), nextLocation)  // Points to draw the path
                        .color(Color.BLUE)  // Set the color of the path
                        .width(8));   /// Set the path width

                //drawPathBetweenAllLocations();
            }

            // Move to the next predefined location after 5 seconds (simulate movement)
            new Handler().postDelayed(() -> {
                currentLocationIndex++;  // Move to the next static location
                simulateMovement();      // Keep simulating the movement
            }, 5000);  // Delay for 5 seconds before moving to the next location
        }
    }

    private void drawPathBetweenLocations() {
        if (currentLocationIndex > 0) {
            LatLng previousLocation = predefinedLocations.get(currentLocationIndex - predefinedLocations.size());
            LatLng currentLocation = predefinedLocations.get(currentLocationIndex);

            if (currentPolyline != null) {
                currentPolyline.remove();  // Remove previous polyline if exists
            }

            // Draw a polyline between the last and current locations
            currentPolyline = mMap.addPolyline(new PolylineOptions()
                    .add(previousLocation, currentLocation)
                    .color(Color.BLUE)  // Blue path color
                    .width(9));         // Path width
        }
    }

    private void drawPathBetweenAllLocations() {
        // Remove previous polyline
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        // List to store points for the polyline
        List<LatLng> pathPoints = new ArrayList<>();

        // Add all predefined locations to path points
        pathPoints.addAll(predefinedLocations);

        // Add the current location as the last point of the path
        pathPoints.add(latLng); // Current location marker

        // Draw the polyline between all points (static locations and current location)
        currentPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(pathPoints)  // Path between all predefined locations and current position
                .color(Color.BLUE)    // Blue path color
                .width(5));           // Path width
    }


/*
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        LatLng defaultLocation = new LatLng(33.55, 9.5375); // Tunisia
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));

        fetchPositionsFromServer();

        mMap.setOnMarkerClickListener(marker -> {
            Position position = (Position) marker.getTag();
            LatLng sousse= new LatLng(33.821430,10);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sousse, 12));

            mMap.addMarker(new MarkerOptions().position(sousse).title("Marker in Sousse"));
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    try {
                        latLng = new LatLng(location.getLatitude(),location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(latLng).title("My position"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        String phone="+15551234567";
                        String myLatitude = String.valueOf(location.getLatitude());
                        String myLongitude = String.valueOf(location.getLongitude());
                        String message="My position is : #"+myLatitude+"#"+myLongitude;
                        SmsManager smsManager=SmsManager.getDefault();
                        smsManager.sendTextMessage(phone,null,message,null,null);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            };
            locationManager= (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME,MIN_DIST,locationListener);
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER,MIN_TIME,MIN_DIST,locationListener);
            }catch (SecurityException e)
            {
                e.printStackTrace();
            }
            if (position != null) {
                // If position is not null, show the InfoWindow
                marker.showInfoWindow();
            }
            showMarkerActionDialog(marker, position);
            return true; // Prevent the default behavior (zoom or move)
        });
        // Handle map click for empty zones
        mMap.setOnMapClickListener(this::showPopup);
    }*/

    private void fetchPositionsFromServer() {
        // URL to your PHP backend script that fetches all positions
        String url = Config.Url_GetAll;

        Log.d("Debug", "Fetching positions from URL: " + url); // Log the URL to check if it's correct

        // Create a new AsyncTask to perform the network operation
        new AsyncTask<Void, Void, List<Position>>() {
            @Override
            protected List<Position> doInBackground(Void... params) {
                List<Position> positions = new ArrayList<>();
                try {
                    // Create the URL object
                    URL urlObj = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoInput(true);

                    Log.d("Debug", "Requesting data..."); // Log before sending the request

                    // Get the response
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    Log.d("Debug", "Response received: " + result.toString()); // Log the raw response

                    // Convert JSON response to List of Position objects
                    JSONArray jsonArray = new JSONArray(result.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject positionObj = jsonArray.getJSONObject(i);
                        Position position = new Position(
                                positionObj.getString("id"),
                                positionObj.getDouble("latitude"),
                                positionObj.getDouble("longitude"),
                                positionObj.getString("phone"),
                                positionObj.getString("name")

                        );
                        positions.add(position);

                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    Log.e("Debug", "Error while fetching positions: " + e.getMessage()); // Log errors
                }
                return positions;
            }

            @Override
            protected void onPostExecute(List<Position> positions) {
                super.onPostExecute(positions);
                Log.d("Debug", "Positions fetched: " + positions.size()); // Log the number of positions fetched

                if (positions != null && !positions.isEmpty()) {
                    for (Position position : positions) {
                        // Add markers to the map (if you are using Google Maps)
                        LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(position.getLatitude(), position.getLongitude()))
                                .title(position.getName()));  // Optional: Set a title
                        marker.setTag(position);  // Set the position as the tag for this marker
                        Log.d("Debug", "Adding marker for: " + position.getName() + " at " + latLng); // Log each marker added
                    }
                } else {
                    Log.d("Debug", "No positions found!"); // Log if no positions are found
                    Toast.makeText(getApplicationContext(), "No positions found!", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }



    private void attemptCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PHONE_CALL);
        } else {
            makePhoneCall(phoneNumber);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHONE_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Try again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showMarkerActionDialog(Marker marker, Position position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_info_window, null);
        TextView friendNameTextView = dialogView.findViewById(R.id.friendName);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.phoneNumber);
        Button deleteButton = dialogView.findViewById(R.id.deleteButtonDetail);
        Button sendSmsButton = dialogView.findViewById(R.id.sendSmsButton);
        Button callButton = dialogView.findViewById(R.id.callButton);
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        callButton.setOnClickListener(v -> {
            attemptCall(position.getPhoneNumber());
            alertDialog.dismiss();
        });
        // Fill dialog with marker details
        friendNameTextView.setText(position.getName());
        phoneNumberTextView.setText(position.getPhoneNumber());

        deleteButton.setOnClickListener(v -> {
            deleteMarkerFromBackend(position,marker);
            alertDialog.dismiss();  // Close dialog
        });

        sendSmsButton.setOnClickListener(v -> {
            sendMessage(position.getPhoneNumber(), "Hello from MyMapFriends!");
            alertDialog.dismiss();
        });

        alertDialog.show();
    }


    private void showPopup(LatLng latLng) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_edit, null);
        EditText nameEditText = popupView.findViewById(R.id.editName);
        EditText phoneEditText = popupView.findViewById(R.id.editPhoneNumber);

        AlertDialog popupDialog = new AlertDialog.Builder(this)
                .setView(popupView)
                .setTitle("Enter Friend's Info")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEditText.getText().toString().trim();
                    String phone = phoneEditText.getText().toString().trim();

                    if (isValidInput(name, phone)) {
                        Position position = new Position(latLng.latitude, latLng.longitude, phone, name);
                        savePositionToBackend(position);
                        addMarkerOnMap(position);
                    } else {
                        Toast.makeText(this, "Name or phone cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();

        popupDialog.show();
    }





    private void savePositionToBackend(Position position) {
        // URL to your PHP backend script
        String url = Config.Url_AddPosition;

        // Create a new AsyncTask to perform the network operation
        new AsyncTask<Position, Void, String>() {
            @Override
            protected String doInBackground(Position... params) {
                Position position = params[0];

                try {
                    // Prepare the URL connection
                    URL urlObj = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) urlObj.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);

                    // Create a parameter string for the POST request
                    String postData = "name=" + URLEncoder.encode(position.getName(), "UTF-8") +
                            "&phone=" + URLEncoder.encode(position.getPhoneNumber(), "UTF-8") +
                            "&latitude=" + position.getLatitude() +
                            "&longitude=" + position.getLongitude();

                    // Send the data
                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(postData);
                    writer.flush();
                    writer.close();
                    os.close();

                    // Get the response code
                    int responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        return "Position saved successfully!";
                    } else {
                        return "Error saving position!";
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return "Network error!";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                // Show the result to the user
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            }
        }.execute(position);
    }


    private boolean isValidInput(String name, String phone) {
        return !name.isEmpty() && !phone.isEmpty() && phone.matches("\\d+");
    }




    private void addMarkerOnMap(Position position) {
        LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(position.getLatitude(), position.getLongitude())));
        marker.setTag(position); // Make sure to set the Position object as the marker's tag
    }

    private void deleteMarkerFromBackend(Position position, final Marker marker) {
        String url = Config.Url_DeletePosition;  // Your PHP delete script URL
        Log.d("Position is",position.toString());// Convert positionId to an integer
        int idInt = Integer.parseInt(position.getPositionId());

        Log.d("Delete", "Starting the delete request for position ID: " + idInt);

        // Create AsyncTask to send POST request
        new AsyncTask<Position, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Position... params) {
                Position position = params[0];
                HttpURLConnection urlConnection = null;
                OutputStream os = null;
                BufferedWriter writer = null;

                try {
                    // Prepare the URL connection
                    URL urlObj = new URL(url);
                    Log.d("Delete", "URL: " + urlObj.toString());

                    urlConnection = (HttpURLConnection) urlObj.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setConnectTimeout(5000);  // Optional: Set timeout for connection
                    urlConnection.setReadTimeout(5000);     // Optional: Set timeout for reading data

                    // Prepare the parameter string for the POST request
                    String postData = "id=" + URLEncoder.encode(String.valueOf(idInt), "UTF-8"); // Use the integer id

                    Log.d("Delete", "Post data: " + postData);

                    // Send the data
                    os = urlConnection.getOutputStream();
                    writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(postData);
                    writer.flush();

                    // Get the response code
                    int responseCode = urlConnection.getResponseCode();
                    Log.d("Delete", "Response code: " + responseCode);

                    // Check if the deletion was successful
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("Delete", "Delete request successful.");
                        return true;  // Success
                    } else {
                        Log.e("Delete", "Error in deletion: Response code " + responseCode);
                        return false; // Failure
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Delete", "Error while deleting position: " + e.getMessage());
                    return false;  // Failure
                } finally {
                    try {
                        if (writer != null) writer.close();
                        if (os != null) os.close();
                        if (urlConnection != null) urlConnection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                // Handle the result (e.g., update UI)
                if (marker != null) {
                    marker.remove();
                }
            }
        }.execute(position);
    }



    private View setupInfoWindow(@NonNull Marker marker) {
        View view = LayoutInflater.from(this).inflate(R.layout.popup_info_window, null);
        Position position = (Position) marker.getTag();

        if (position != null) {
            TextView friendNameTextView = view.findViewById(R.id.friendName);
            TextView friendNumberTextView = view.findViewById(R.id.phoneNumber);
            Button deleteButton = view.findViewById(R.id.deleteButtonDetail);
            Button sendSmsButton = view.findViewById(R.id.sendSmsButton);

            friendNameTextView.setText(position.getName());
            friendNumberTextView.setText(position.getPhoneNumber());

            deleteButton.setOnClickListener(v -> deleteMarkerFromBackend(position, marker));
            sendSmsButton.setOnClickListener(v -> sendMessage(position.getPhoneNumber(), "Hello from MyMapFriends!"));

        }

        return view;
    }





    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update intent for this activity

        // Reload map data or refresh UI if necessary
        //fetchPositionsFromFirestore();
        fetchPositionsFromServer();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void sendMessage(String phoneNumber, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }
}
