package com.solarsnap.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 1920; // Max width/height in pixels
    private static final int JPEG_QUALITY = 85; // JPEG compression quality (0-100)
    
    /**
     * Compress image file to reduce size for upload
     */
    public static File compressImage(Context context, String inputPath, String outputPath) {
        try {
            // Decode image bounds first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(inputPath, options);
            
            // Calculate sample size
            int sampleSize = calculateSampleSize(options.outWidth, options.outHeight);
            
            // Decode with sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(inputPath, options);
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + inputPath);
                return null;
            }
            
            // Handle image rotation
            bitmap = rotateImageIfRequired(bitmap, inputPath);
            
            // Further resize if still too large
            bitmap = resizeIfNeeded(bitmap, MAX_IMAGE_SIZE);
            
            // Save compressed image
            File outputFile = new File(outputPath);
            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            out.close();
            bitmap.recycle();
            
            Log.d(TAG, String.format("Image compressed: %s -> %s (%.1f KB -> %.1f KB)", 
                inputPath, outputPath, 
                new File(inputPath).length() / 1024.0,
                outputFile.length() / 1024.0));
            
            return outputFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }
    
    /**
     * Calculate sample size for efficient bitmap loading
     */
    private static int calculateSampleSize(int width, int height) {
        int sampleSize = 1;
        
        if (height > MAX_IMAGE_SIZE || width > MAX_IMAGE_SIZE) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / sampleSize) >= MAX_IMAGE_SIZE
                    && (halfWidth / sampleSize) >= MAX_IMAGE_SIZE) {
                sampleSize *= 2;
            }
        }
        
        return sampleSize;
    }
    
    /**
     * Rotate image based on EXIF orientation
     */
    private static Bitmap rotateImageIfRequired(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
                ExifInterface.ORIENTATION_NORMAL);
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                default:
                    return bitmap;
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not read EXIF orientation", e);
            return bitmap;
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    private static Bitmap rotateImage(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 
            bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotatedBitmap;
    }
    
    /**
     * Resize bitmap if it exceeds maximum size
     */
    private static Bitmap resizeIfNeeded(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        bitmap.recycle();
        return resized;
    }
    
    /**
     * Convert bitmap to byte array
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }
    
    /**
     * Get file size in human readable format
     */
    public static String getFileSizeString(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Create compressed copy of image for upload
     */
    public static File prepareImageForUpload(Context context, String originalPath) {
        try {
            File originalFile = new File(originalPath);
            if (!originalFile.exists()) {
                Log.e(TAG, "Original file does not exist: " + originalPath);
                return null;
            }
            
            // Create compressed version in cache directory
            File cacheDir = new File(context.getCacheDir(), "compressed_images");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            String compressedPath = new File(cacheDir, 
                "compressed_" + originalFile.getName()).getAbsolutePath();
            
            return compressImage(context, originalPath, compressedPath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image for upload", e);
            return null;
        }
    }
}