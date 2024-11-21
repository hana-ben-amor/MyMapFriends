package com.example.mymapfriends.model;

public class Position {
    private String positionId; // Unique ID for the position (Firestore document ID)
    private String phoneNumber;
    private double latitude;
    private double longitude;
    private String name;

    @Override
    public String toString() {
        return "Position{" +
                "positionId='" + positionId + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", name='" + name + '\'' +
                '}';
    }

    // Default constructor required for Firestore
    public Position() {}

    // Constructor with all fields (excluding the ID)
    public Position( double latitude, double longitude,String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }


    // Constructor with positionId included (in case it's needed for local usage)
    public Position(String positionId, double latitude, double longitude,String phoneNumber,  String name) {
        this.positionId = positionId;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public Position(double latitude, double longitude) {
        this.phoneNumber = "5551234567";
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = "Friend";
    }

    // Getters and setters
    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}