// Simple test to verify backend connection
// You can run this as a unit test or integrate into your testing framework

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BackendConnectionTest {
    
    private static final String BACKEND_URL = "https://solarsnap-backend.onrender.com";
    
    public static void main(String[] args) {
        System.out.println("🔍 Testing SolarSnap Backend Connection...");
        System.out.println("Backend URL: " + BACKEND_URL);
        System.out.println("=" + "=".repeat(50));
        
        // Test health endpoint
        testHealthEndpoint();
        
        // Test API root endpoint
        testApiRootEndpoint();
        
        // Test auth endpoint (should return 400/422 for missing data, not 500)
        testAuthEndpoint();
        
        System.out.println("=" + "=".repeat(50));
        System.out.println("✅ Backend connection tests completed!");
    }
    
    private static void testHealthEndpoint() {
        try {
            URL url = new URL(BACKEND_URL + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();
                
                System.out.println("✅ Health Check: " + responseCode + " - " + response);
            } else {
                System.out.println("❌ Health Check: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Health Check Failed: " + e.getMessage());
        }
    }
    
    private static void testApiRootEndpoint() {
        try {
            URL url = new URL(BACKEND_URL + "/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                System.out.println("✅ API Root: " + responseCode + " - OK");
            } else {
                System.out.println("❌ API Root: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("❌ API Root Failed: " + e.getMessage());
        }
    }
    
    private static void testAuthEndpoint() {
        try {
            URL url = new URL(BACKEND_URL + "/api/v1/auth/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            
            // Should return 400 or 422 for missing data, not 500
            if (responseCode == 400 || responseCode == 422) {
                System.out.println("✅ Auth Endpoint: " + responseCode + " - Correctly rejects empty request");
            } else if (responseCode < 500) {
                System.out.println("✅ Auth Endpoint: " + responseCode + " - Accessible");
            } else {
                System.out.println("❌ Auth Endpoint: " + responseCode + " - Server error");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Auth Endpoint Failed: " + e.getMessage());
        }
    }
}