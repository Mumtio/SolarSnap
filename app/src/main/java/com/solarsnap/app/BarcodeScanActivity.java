package com.solarsnap.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.solarsnap.app.utils.BarcodeScanner;

public class BarcodeScanActivity extends AppCompatActivity implements BarcodeScanner.BarcodeDetectionListener {
    
    private static final String TAG = "BarcodeScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    public static final String EXTRA_SCANNED_BARCODE = "scanned_barcode";
    
    private PreviewView previewView;
    private TextView instructionText;
    private TextView statusText;
    private Button cancelButton;
    
    private BarcodeScanner barcodeScanner;
    private boolean scanCompleted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);
        
        initializeViews();
        setupListeners();
        
        // Initialize barcode scanner
        barcodeScanner = new BarcodeScanner(this, this, previewView);
        barcodeScanner.setBarcodeDetectionListener(this);
        
        // Check camera permission and start scanning
        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }
    
    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        instructionText = findViewById(R.id.instructionText);
        statusText = findViewById(R.id.statusText);
        cancelButton = findViewById(R.id.cancelButton);
        
        instructionText.setText("Point camera at panel barcode or QR code");
        statusText.setText("Initializing camera...");
    }
    
    private void setupListeners() {
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.CAMERA}, 
            CAMERA_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                statusText.setText("Camera permission required for barcode scanning");
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startScanning() {
        statusText.setText("Scanning for barcodes...");
        barcodeScanner.startScanning();
    }
    
    @Override
    public void onBarcodeDetected(String barcode) {
        if (scanCompleted) {
            return; // Prevent multiple detections
        }
        
        scanCompleted = true;
        
        runOnUiThread(() -> {
            statusText.setText("Barcode detected: " + barcode);
            Toast.makeText(this, "Panel ID detected: " + barcode, Toast.LENGTH_SHORT).show();
            
            // Return result to calling activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SCANNED_BARCODE, barcode);
            setResult(RESULT_OK, resultIntent);
            
            // Delay finish to show the result
            previewView.postDelayed(() -> finish(), 1000);
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            statusText.setText("Error: " + error);
            Toast.makeText(this, "Scanning error: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScanner != null && !scanCompleted && checkCameraPermission()) {
            if (!barcodeScanner.isScanning()) {
                startScanning();
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.stopScanning();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.cleanup();
        }
    }
}