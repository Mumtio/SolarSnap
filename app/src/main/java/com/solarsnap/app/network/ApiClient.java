package com.solarsnap.app.network;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    
    // Production backend URL
    private static final String BASE_URL = "https://solarsnap-backend.onrender.com/api/v1/";
    
    // Alternative URLs for different environments
    // For local development: "http://10.0.2.2:5000/api/v1/" (Android emulator)
    // For physical device: "http://YOUR_COMPUTER_IP:5000/api/v1/"
    // For production: "https://solarsnap-backend.onrender.com/api/v1/"
    
    private static Retrofit retrofit = null;
    private static SolarSnapApiService apiService = null;
    
    public static SolarSnapApiService getApiService(Context context) {
        if (apiService == null) {
            retrofit = createRetrofit(context);
            apiService = retrofit.create(SolarSnapApiService.class);
        }
        return apiService;
    }
    
    private static Retrofit createRetrofit(Context context) {
        // Logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Auth interceptor for JWT token
        AuthInterceptor authInterceptor = new AuthInterceptor(context);
        
        // OkHttp client with interceptors
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // Increased for large responses
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        // Retrofit instance
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }
    
    // Method to update base URL (for testing different environments)
    public static void setBaseUrl(String baseUrl, Context context) {
        // Create new OkHttp client with interceptors
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        AuthInterceptor authInterceptor = new AuthInterceptor(context);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // Increased for large responses
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        apiService = retrofit.create(SolarSnapApiService.class);
    }
    
    // Get current base URL for debugging
    public static String getBaseUrl() {
        return BASE_URL;
    }
}
