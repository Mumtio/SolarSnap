package com.solarsnap.app.network;

import android.content.Context;
import android.util.Log;
import com.solarsnap.app.utils.ImageUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.IOException;

public class UploadProgressService {
    
    private static final String TAG = "UploadProgressService";
    
    private Context context;
    private SolarSnapApiService apiService;
    
    public interface UploadProgressListener {
        void onProgress(int percentage);
        void onSuccess(String response);
        void onError(String error);
    }
    
    public UploadProgressService(Context context) {
        this.context = context;
        this.apiService = ApiClient.getApiService(context);
    }
    
    /**
     * Upload thermal image with progress tracking
     */
    public void uploadThermalImage(String panelId, String filePath, 
                                 UploadProgressListener listener) {
        uploadImage("thermal", panelId, filePath, listener);
    }
    
    /**
     * Upload visual image with progress tracking
     */
    public void uploadVisualImage(String panelId, String filePath, 
                                UploadProgressListener listener) {
        uploadImage("visual", panelId, filePath, listener);
    }
    
    /**
     * Generic image upload method with compression and progress tracking
     */
    private void uploadImage(String imageType, String panelId, String filePath, 
                           UploadProgressListener listener) {
        
        new Thread(() -> {
            try {
                // Step 1: Compress image (10% progress)
                if (listener != null) {
                    listener.onProgress(10);
                }
                
                File compressedFile = ImageUtils.prepareImageForUpload(context, filePath);
                if (compressedFile == null) {
                    if (listener != null) {
                        listener.onError("Failed to compress image");
                    }
                    return;
                }
                
                // Step 2: Prepare multipart request (20% progress)
                if (listener != null) {
                    listener.onProgress(20);
                }
                
                RequestBody panelIdBody = RequestBody.create(
                    MediaType.parse("text/plain"), panelId);
                
                RequestBody imageTypeBody = RequestBody.create(
                    MediaType.parse("text/plain"), imageType);
                
                RequestBody fileBody = RequestBody.create(
                    MediaType.parse("image/jpeg"), compressedFile);
                
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image", compressedFile.getName(), 
                    new ProgressRequestBody(fileBody, new ProgressRequestBody.ProgressListener() {
                        @Override
                        public void onProgress(long bytesWritten, long totalBytes) {
                            // Map upload progress to 30-90% range
                            int progress = (int) (30 + (bytesWritten * 60 / totalBytes));
                            if (listener != null) {
                                listener.onProgress(progress);
                            }
                        }
                    }));
                
                // Step 3: Make API call
                Call<ResponseBody> call;
                if ("thermal".equals(imageType)) {
                    call = apiService.uploadThermalImage(panelIdBody, imagePart);
                } else {
                    call = apiService.uploadVisualImage(panelIdBody, imagePart);
                }
                
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        // Clean up compressed file
                        if (compressedFile.exists()) {
                            compressedFile.delete();
                        }
                        
                        if (response.isSuccessful()) {
                            if (listener != null) {
                                listener.onProgress(100);
                                try {
                                    String responseString = response.body() != null ? 
                                        response.body().string() : "Upload successful";
                                    listener.onSuccess(responseString);
                                } catch (IOException e) {
                                    listener.onSuccess("Upload successful");
                                }
                            }
                            Log.d(TAG, imageType + " image uploaded successfully for panel: " + panelId);
                        } else {
                            String error = "Upload failed: " + response.code() + " " + response.message();
                            Log.e(TAG, error);
                            if (listener != null) {
                                listener.onError(error);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // Clean up compressed file
                        if (compressedFile.exists()) {
                            compressedFile.delete();
                        }
                        
                        String error = "Upload failed: " + t.getMessage();
                        Log.e(TAG, error, t);
                        if (listener != null) {
                            listener.onError(error);
                        }
                    }
                });
                
            } catch (Exception e) {
                String error = "Upload preparation failed: " + e.getMessage();
                Log.e(TAG, error, e);
                if (listener != null) {
                    listener.onError(error);
                }
            }
        }).start();
    }
    
    /**
     * Batch upload multiple images
     */
    public void batchUpload(String panelId, String thermalPath, String visualPath,
                          UploadProgressListener listener) {
        
        new Thread(() -> {
            try {
                // Upload thermal image first (0-50% progress)
                uploadThermalImage(panelId, thermalPath, new UploadProgressListener() {
                    @Override
                    public void onProgress(int percentage) {
                        if (listener != null) {
                            listener.onProgress(percentage / 2); // Map to 0-50%
                        }
                    }
                    
                    @Override
                    public void onSuccess(String response) {
                        // Start visual image upload (50-100% progress)
                        uploadVisualImage(panelId, visualPath, new UploadProgressListener() {
                            @Override
                            public void onProgress(int percentage) {
                                if (listener != null) {
                                    listener.onProgress(50 + percentage / 2); // Map to 50-100%
                                }
                            }
                            
                            @Override
                            public void onSuccess(String response2) {
                                if (listener != null) {
                                    listener.onSuccess("Both images uploaded successfully");
                                }
                            }
                            
                            @Override
                            public void onError(String error) {
                                if (listener != null) {
                                    listener.onError("Visual image upload failed: " + error);
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (listener != null) {
                            listener.onError("Thermal image upload failed: " + error);
                        }
                    }
                });
                
            } catch (Exception e) {
                String error = "Batch upload failed: " + e.getMessage();
                Log.e(TAG, error, e);
                if (listener != null) {
                    listener.onError(error);
                }
            }
        }).start();
    }
    
    /**
     * Get upload file size estimate
     */
    public String getUploadSizeEstimate(String filePath) {
        try {
            File originalFile = new File(filePath);
            if (!originalFile.exists()) {
                return "File not found";
            }
            
            long originalSize = originalFile.length();
            // Estimate compressed size (roughly 30-50% of original for JPEG)
            long estimatedSize = (long) (originalSize * 0.4);
            
            return String.format("Original: %s, Estimated upload: %s", 
                ImageUtils.getFileSizeString(originalSize),
                ImageUtils.getFileSizeString(estimatedSize));
                
        } catch (Exception e) {
            Log.e(TAG, "Error calculating file size", e);
            return "Size unknown";
        }
    }
}