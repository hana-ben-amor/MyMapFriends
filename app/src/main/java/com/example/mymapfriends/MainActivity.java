package com.example.mymapfriends;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PHONE_CALL = 1;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private static FirebaseAuth mAuth;
    private SmsReceiver smsReceiver = new SmsReceiver();


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

        LatLng defaultLocation = new LatLng(33.8869, 9.5375); // Tunisia
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));

        fetchPositionsFromFirestore();

        mMap.setOnMarkerClickListener(marker -> {
            Position position = (Position) marker.getTag();
            if (position != null) {
                showMarkerActionDialog(marker, position);
            }


            return true; // Prevent default behavior
        });
        // Handle map click for empty zones
        mMap.setOnMapClickListener(this::showPopup);
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
            deleteMarkerFromFirebase(position, marker);
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
                        savePositionToFirestore(position);
                        addMarkerOnMap(position);
                    } else {
                        Toast.makeText(this, "Name or phone cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();

        popupDialog.show();
    }

    private boolean isValidInput(String name, String phone) {
        return !name.isEmpty() && !phone.isEmpty() && phone.matches("\\d+");
    }


    private void fetchPositionsFromFirestore() {
        db.collection("positions")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(MainActivity.this, "Error loading data.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        mMap.clear(); // Clear all markers to avoid duplicates
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            Position position = document.toObject(Position.class);
                            if (position != null) {
                                position.setPositionId(document.getId());
                                addMarkerOnMap(position);
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No data available.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void addMarkerOnMap(Position position) {
        LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(position.getName()));
        if (marker != null) {
            marker.setTag(position);
        }
    }



    private void savePositionToFirestore(Position position) {
        db.collection("positions")
                .add(position)
                .addOnSuccessListener(documentReference -> {
                    position.setPositionId(documentReference.getId());
                    Toast.makeText(this, "Position saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving position", Toast.LENGTH_SHORT).show();
                });
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

            deleteButton.setOnClickListener(v -> deleteMarkerFromFirebase(position, marker));
            sendSmsButton.setOnClickListener(v -> sendMessage(position.getPhoneNumber(), "Hello from MyMapFriends!"));

        }

        return view;
    }





    private void deleteMarkerFromFirebase(Position position, Marker marker) {
        db.collection("positions").document(position.getPositionId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting friend.", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error deleting document", e);
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update intent for this activity

        // Reload map data or refresh UI if necessary
        fetchPositionsFromFirestore();
    }


    private void sendMessage(String phoneNumber, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }
}
