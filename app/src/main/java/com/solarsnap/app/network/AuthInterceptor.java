package com.solarsnap.app.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    
    private static final String PREF_NAME = "SolarSnapPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    
    private final Context context;
    
    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Skip auth for login and register endpoints
        String path = originalRequest.url().encodedPath();
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            return chain.proceed(originalRequest);
        }
        
        // Get token from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        
        // Add Authorization header if token exists
        if (token != null && !token.isEmpty()) {
            Request authorizedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
            return chain.proceed(authorizedRequest);
        }
        
        return chain.proceed(originalRequest);
    }
    
    // Helper method to save token
    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }
    
    // Helper method to get token
    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    // Helper method to clear token
    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
}
