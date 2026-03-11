package com.solarsnap.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.solarsnap.app.repository.AuthRepository;
import com.solarsnap.app.network.models.LoginResponse;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private EditText companyIdInput;
    private Button loginButton;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize repository
        authRepository = new AuthRepository(this);
        
        // Check if already logged in
        if (authRepository.isLoggedIn()) {
            navigateToSiteSelection();
            return;
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        companyIdInput = findViewById(R.id.companyIdInput);
        loginButton = findViewById(R.id.loginButton);

        // Remove hardcoded test credentials - let user enter their own
        // emailInput.setText("inspector1@solartech.com");
        // passwordInput.setText("password123");
        // companyIdInput.setText("SOLARTECH-001");

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String companyId = companyIdInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button during login
            loginButton.setEnabled(false);
            loginButton.setText("Logging in...");

            // Call real API
            authRepository.login(email, password, companyId, new AuthRepository.LoginCallback() {
                @Override
                public void onSuccess(LoginResponse response) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, 
                            "Welcome " + response.getUser().getFullName(), 
                            Toast.LENGTH_SHORT).show();
                        navigateToSiteSelection();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, 
                            "Login failed: " + error, 
                            Toast.LENGTH_LONG).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    });
                }
            });
        });
    }
    
    private void navigateToSiteSelection() {
        Intent intent = new Intent(LoginActivity.this, SiteSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}
