package com.solarsnap.app.repository;

import android.content.Context;
import android.util.Log;

import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.AuthInterceptor;
import com.solarsnap.app.network.SolarSnapApiService;
import com.solarsnap.app.network.models.LoginRequest;
import com.solarsnap.app.network.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    
    private static final String TAG = "AuthRepository";
    private final SolarSnapApiService apiService;
    private final Context context;
    
    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getApiService(context);
    }
    
    // Login callback interface
    public interface LoginCallback {
        void onSuccess(LoginResponse response);
        void onError(String error);
    }
    
    // Logout callback interface
    public interface LogoutCallback {
        void onSuccess();
        void onError(String error);
    }
    
    // Login method
    public void login(String email, String password, String companyId, LoginCallback callback) {
        LoginRequest request = new LoginRequest(email, password, companyId);
        
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    if (loginResponse.isSuccess()) {
                        // Save token
                        String token = loginResponse.getAccessToken();
                        AuthInterceptor.saveToken(context, token);
                        
                        Log.d(TAG, "Login successful: " + loginResponse.getUser().getFullName());
                        callback.onSuccess(loginResponse);
                    } else {
                        callback.onError(loginResponse.getMessage());
                    }
                } else {
                    callback.onError("Login failed: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "Login error: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Logout method
    public void logout(LogoutCallback callback) {
        // Call backend logout API first
        apiService.logout().enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                // Clear local token regardless of backend response
                AuthInterceptor.clearToken(context);
                Log.d(TAG, "User logged out");
                callback.onSuccess();
            }
            
            @Override
            public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                // Clear local token even if backend call fails
                AuthInterceptor.clearToken(context);
                Log.d(TAG, "User logged out (offline)");
                callback.onSuccess(); // Still consider it success since local logout worked
            }
        });
    }
    
    // Simple logout method (for backward compatibility)
    public void logout() {
        AuthInterceptor.clearToken(context);
        Log.d(TAG, "User logged out");
    }
    
    // Check if user is logged in
    public boolean isLoggedIn() {
        String token = AuthInterceptor.getToken(context);
        return token != null && !token.isEmpty();
    }
    
    // Get current token
    public String getToken() {
        return AuthInterceptor.getToken(context);
    }
}
