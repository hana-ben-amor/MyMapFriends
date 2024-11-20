package com.example.mymapfriends;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class JSONParser {

    private static final String CHARSET = "UTF-8";
    private static final String TAG = "JSONParser";
    private HttpURLConnection conn;
    private DataOutputStream wr;
    private StringBuilder result;
    private URL urlObj;
    private JSONObject jObj = null;
    private StringBuilder sbParams;
    private String paramsString;

    // Simplified GET Request
    public JSONObject makeRequest(String url) {
        try {
            urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);

            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            Log.d(TAG, "Result: " + result.toString());
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IO Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            jObj = new JSONObject(result.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
        }

        return jObj;
    }

    // General HTTP Request (GET or POST)
    public JSONObject makeHttpRequest(String url, String method, HashMap<String, String> params) {
        sbParams = new StringBuilder();

        if (params != null) {
            int i = 0;
            for (String key : params.keySet()) {
                try {
                    if (i != 0) {
                        sbParams.append("&");
                    }
                    sbParams.append(key).append("=")
                            .append(URLEncoder.encode(params.get(key), CHARSET));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Encoding Error: " + e.getMessage());
                }
                i++;
            }
        }

        if (method.equalsIgnoreCase("POST")) {
            // POST Request
            try {
                urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept-Charset", CHARSET);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);

                if (params != null) {
                    paramsString = sbParams.toString();
                    wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(paramsString);
                    wr.flush();
                    wr.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "IO Error: " + e.getMessage());
            }
        } else if (method.equalsIgnoreCase("GET")) {
            // GET Request
            if (sbParams.length() != 0) {
                url += "?" + sbParams.toString();
            }

            try {
                urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
            } catch (IOException e) {
                Log.e(TAG, "IO Error: " + e.getMessage());
            }
        }

        try {
            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            Log.d(TAG, "Result: " + result.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            jObj = new JSONObject(result.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
        }

        return jObj;
    }
}
