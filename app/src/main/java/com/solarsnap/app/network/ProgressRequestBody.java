package com.solarsnap.app.network;

import android.os.Handler;
import android.os.Looper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import java.io.IOException;

public class ProgressRequestBody extends RequestBody {
    
    private RequestBody requestBody;
    private ProgressListener progressListener;
    private Handler mainHandler;
    
    public interface ProgressListener {
        void onProgress(long bytesWritten, long totalBytes);
    }
    
    public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
        this.requestBody = requestBody;
        this.progressListener = progressListener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }
    
    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }
    
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }
    
    private class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;
        
        public CountingSink(Sink delegate) {
            super(delegate);
        }
        
        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            
            bytesWritten += byteCount;
            
            if (progressListener != null) {
                // Post progress update to main thread
                mainHandler.post(() -> {
                    try {
                        progressListener.onProgress(bytesWritten, contentLength());
                    } catch (IOException e) {
                        // Ignore - contentLength() might fail
                        progressListener.onProgress(bytesWritten, bytesWritten);
                    }
                });
            }
        }
    }
}