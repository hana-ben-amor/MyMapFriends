package com.example.mymapfriends;

import com.example.mymapfriends.model.Position;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("save_position.php")
    Call<JsonObject> savePosition(
            @Field("latitude") double latitude,
            @Field("longitude") double longitude,
            @Field("phone_number") String phoneNumber
    );

    @GET("fetch_positions.php")
    Call<List<Position>> fetchPositions();
}
