package com.solarsnap.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BarcodeScanner {
    
    private static final String TAG = "BarcodeScanner";
    
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private MultiFormatReader barcodeReader;
    private BarcodeDetectionListener listener;
    private boolean isScanning = false;
    
    public interface BarcodeDetectionListener {
        void onBarcodeDetected(String barcode);
        void onError(String error);
    }
    
    public BarcodeScanner(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        
        // Configure ZXing barcode reader (ACE-compatible)
        barcodeReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.DATA_MATRIX
        ));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        barcodeReader.setHints(hints);
    }
    
    public void setBarcodeDetectionListener(BarcodeDetectionListener listener) {
        this.listener = listener;
    }
    
    public void startScanning() {
        if (isScanning) {
            Log.w(TAG, "Scanner is already running");
            return;
        }
        
        if (!checkCameraPermission()) {
            if (listener != null) {
                listener.onError("Camera permission not granted");
            }
            return;
        }
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(context);
        
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isScanning = true;
                Log.d(TAG, "Barcode scanning started");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                if (listener != null) {
                    listener.onError("Failed to start camera: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    public void stopScanning() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            isScanning = false;
            Log.d(TAG, "Barcode scanning stopped");
        }
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }
        
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis use case for barcode detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), 
            new BarcodeAnalyzer());
        
        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            if (listener != null) {
                listener.onError("Camera binding failed: " + e.getMessage());
            }
        }
    }
    
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            try {
                // Convert ImageProxy to byte array for ZXing
                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                
                int width = imageProxy.getWidth();
                int height = imageProxy.getHeight();
                
                // Create luminance source from YUV data
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data, width, height, 0, 0, width, height, false);
                
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                
                try {
                    Result result = barcodeReader.decode(bitmap);
                    String barcode = result.getText();
                    
                    if (barcode != null && !barcode.isEmpty()) {
                        Log.d(TAG, "Barcode detected: " + barcode);
                        
                        // Validate if it looks like a panel ID
                        if (isValidPanelId(barcode)) {
                            if (listener != null) {
                                listener.onBarcodeDetected(barcode);
                            }
                            // Stop scanning after successful detection
                            stopScanning();
                            return;
                        } else {
                            Log.d(TAG, "Invalid panel ID format: " + barcode);
                        }
                    }
                } catch (NotFoundException e) {
                    // No barcode found in this frame, continue scanning
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Barcode analysis error", e);
            } finally {
                imageProxy.close();
            }
        }
        
        private boolean isValidPanelId(String barcode) {
            // Check if barcode matches panel ID pattern (e.g., PNL-A7-1234)
            return barcode.matches("^PNL-[A-Z0-9]+-\\d+$") || 
                   barcode.matches("^\\d{4,}$") || // Simple numeric ID
                   barcode.length() >= 4; // Any barcode with at least 4 characters
        }
    }
    
    /**
     * Scan barcode from static image using ZXing (ACE-compatible)
     */
    public void scanFromBitmap(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            Result result = barcodeReader.decode(binaryBitmap);
            String barcode = result.getText();
            
            if (barcode != null && !barcode.isEmpty()) {
                Log.d(TAG, "Barcode detected from image: " + barcode);
                if (listener != null) {
                    listener.onBarcodeDetected(barcode);
                }
            } else {
                if (listener != null) {
                    listener.onError("No barcode found in image");
                }
            }
            
        } catch (NotFoundException e) {
            Log.d(TAG, "No barcode found in image");
            if (listener != null) {
                listener.onError("No valid barcode found in image");
            }
        } catch (Exception e) {
            Log.e(TAG, "Barcode scanning from image failed", e);
            if (listener != null) {
                listener.onError("Barcode scanning failed: " + e.getMessage());
            }
        }
    }
    
    public void cleanup() {
        stopScanning();
        // ZXing doesn't require explicit cleanup like ML Kit
    }
}