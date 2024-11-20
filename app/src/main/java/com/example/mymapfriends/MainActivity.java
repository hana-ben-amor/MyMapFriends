package com.example.mymapfriends;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
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
import com.example.mymapfriends.model.Position;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PHONE_CALL = 1;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private static FirebaseAuth mAuth;
    private SmsReceiver smsReceiver = new SmsReceiver();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        signInAnonymously();

        db = FirebaseFirestore.getInstance();
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

    public void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("Auth", "User ID: " + user.getUid());
                        }
                    }
                });
    }



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        LatLng defaultLocation = new LatLng(33.55, 9.5375); // Tunisia
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));

        fetchPositionsFromServer();

        mMap.setOnMarkerClickListener(marker -> {
            Position position = (Position) marker.getTag();

            if (position != null) {
                // If position is not null, show the InfoWindow
                marker.showInfoWindow();
            }
            showMarkerActionDialog(marker, position);
            return true; // Prevent the default behavior (zoom or move)
        });
        // Handle map click for empty zones
        mMap.setOnMapClickListener(this::showPopup);
    }

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
        Log.d("Delete", "Starting the delete request for position ID: " + position.getPositionId());

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

                    // Create the parameter string for the POST request
                    String postData = "id=" + URLEncoder.encode(String.valueOf(position.getPositionId()), "UTF-8");
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
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Log.d("Delete", "Position deleted successfully.");
                    // Here you can also update the UI or remove the marker from the map
                    if (marker != null) {
                        marker.remove();  // Remove the marker from the map
                        Log.d("Delete", "Marker removed from map.");
                    }
                } else {
                    Log.e("Delete", "Failed to delete position.");
                    // Optionally show a toast or notify the user about the failure
                }
            }
        }.execute(position);  // Execute AsyncTask
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
