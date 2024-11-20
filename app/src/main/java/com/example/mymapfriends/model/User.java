package com.example.mymapfriends.model;

import java.util.List;

public class User {
    private int id;  // User ID
    private String name;
    private String phoneNumber;
    private List<Position> positions;  // List of positions associated with the user

    // Constructor
    public User(int id, String name, String phoneNumber, List<Position> positions) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.positions = positions;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    // Optional: Method to return a string representation of the User object
    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', phoneNumber='" + phoneNumber + "', positions=" + positions + "}";
    }
}
