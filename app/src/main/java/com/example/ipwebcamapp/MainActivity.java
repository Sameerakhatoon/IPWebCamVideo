package com.example.ipwebcamapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IPWebcamApp";
    private static final int CAMERA_REQUEST_CODE = 50;
    private static final int SETTINGS_REQUEST_CODE = 100; // Unique request code for settings activity
    private static int PORT = 8080;
    private static final String BOUNDARY = "myboundary";

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private NanoHTTPD server;
    private TextView ipTextView;
    private volatile byte[] currentFrame;

    private Size previewSize = new Size(640, 480);  // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Activity created");

        textureView = findViewById(R.id.textureView);
        Log.d(TAG, "onCreate: TextureView initialized");

        ipTextView = findViewById(R.id.ipPortTextView);
        Log.d(TAG, "onCreate: IP TextView initialized");

        Button settingsButton = findViewById(R.id.settingsButton);  // Ensure you have a button with this ID
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });

        textureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Log.d(TAG, "onGlobalLayout: TextureView layout observed");
                adjustTextureViewSize();
            }
        });

        checkCameraPermission();
        Log.d(TAG, "onCreate: Checked camera permissions");

        startServer(PORT);

        displayIpAddress();
        Log.d(TAG, "onCreate: Displayed IP address");
    }

    private void checkCameraPermission() {
        Log.d(TAG, "checkCameraPermission: Checking camera permissions");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkCameraPermission: Camera permission not granted, requesting permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            Log.d(TAG, "checkCameraPermission: Camera permission already granted");
            startCamera(); // Permission already granted, start camera
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Received result for request code " + requestCode);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Camera permission granted");
                startCamera(); // Permission granted, start camera
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Camera permission denied");
                // Optionally, you can show a message or take appropriate action
            }
        }
    }

    private void startCamera() {
        Log.d(TAG, "startCamera: Starting camera");
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            Log.d(TAG, "startCamera: Camera ID " + cameraId);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                previewSize = map.getOutputSizes(SurfaceTexture.class)[0];  // Choose a suitable size
                Log.d(TAG, "startCamera: Preview size set to " + previewSize);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "startCamera: Camera permission check failed");
                return;
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "onOpened: Camera opened");
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "onDisconnected: Camera disconnected");
                    cameraDevice.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "onError: Camera error " + error);
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "startCamera: CameraAccessException", e);
        }
    }

    private void startPreview() {
        Log.d(TAG, "startPreview: Starting camera preview");
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Log.d(TAG, "startPreview: SurfaceTexture buffer size set");

        Surface surface = new Surface(surfaceTexture);
        Log.d(TAG, "startPreview: Surface created");

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Log.d(TAG, "startPreview: CaptureRequest.Builder created");

            captureRequestBuilder.addTarget(surface);
            Log.d(TAG, "startPreview: Target surface added to CaptureRequest");

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            Log.d(TAG, "startPreview: ImageReader created with size " + previewSize);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    try {
                        byte[] nv21 = yuv420ToNV21(image);
                        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 80, outputStream);
                        byte[] jpegBytes = outputStream.toByteArray();
                        synchronized (this) {
                            currentFrame = jpegBytes;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image", e);
                    } finally {
                        image.close();
                    }
                }
            }, null);

            Surface imageReaderSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(imageReaderSurface);
            Log.d(TAG, "startPreview: ImageReader surface added to CaptureRequest");

            // Enable auto-focus
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            Log.d(TAG, "startPreview: Auto-focus mode set");

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReaderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigured: Capture session configured");
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                Log.d(TAG, "onCaptureCompleted: Capture completed");
                                super.onCaptureCompleted(session, request, result);
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e(TAG, "onConfigured: CameraAccessException during capture session setup", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "onConfigureFailed: Capture session configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "startPreview: CameraAccessException during preview setup", e);
        }
    }

    private void adjustTextureViewSize() {
        Log.d(TAG, "adjustTextureViewSize: Adjusting TextureView size");
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = previewSize.getWidth();
        layoutParams.height = previewSize.getHeight();
        textureView.setLayoutParams(layoutParams);
        Log.d(TAG, "adjustTextureViewSize: TextureView size adjusted to " + previewSize);
    }

    private void startServer(int port) {
        if (server != null) {
            server.stop();
            Log.d(TAG, "startServer: NanoHTTPD server stopped");
        }

        server = new NanoHTTPD(port) {
            @Override
            public Response serve(IHTTPSession session) {
                Log.d(TAG, "serve: Received request with URI " + session.getUri());
                if ("/video".equals(session.getUri())) {
                    return streamVideo();  // Call once per request
                } else {
                    String response = "IP Webcam Running!";
                    return newFixedLengthResponse(response);
                }
            }
        };

        try {
            server.start();
            Log.d(TAG, "startServer: NanoHTTPD server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "startServer: Failed to start NanoHTTPD server", e);
        }
    }

    private NanoHTTPD.Response streamVideo() {
        Log.d(TAG, "streamVideo: Streaming video");
        try {
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(outputStream);

            // Start a new thread to continuously write video frames to the output stream
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] frame;
                        synchronized (this) {
                            frame = currentFrame;
                        }
                        if (frame != null) {
                            outputStream.write(("--" + BOUNDARY + "\r\n").getBytes());
                            outputStream.write("Content-Type: image/jpeg\r\n".getBytes());
                            outputStream.write(("Content-Length: " + frame.length + "\r\n").getBytes());
                            outputStream.write("\r\n".getBytes());
                            outputStream.write(frame);
                            outputStream.write("\r\n".getBytes());
                            outputStream.flush();
                            Log.d(TAG, "streamVideo: Frame written to output stream");
                        }
                        Thread.sleep(16);  // Control frame rate
                    }
                } catch (IOException | InterruptedException e) {

                    e.printStackTrace();
                    Log.e(TAG, "streamVideo: Exception in video streaming thread", e);
                } finally {
                    try {
                        outputStream.close();
                        Log.d(TAG, "streamVideo: Output stream closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "streamVideo: IOException when closing output stream", e);
                    }
                }
            }).start();

            return newChunkedResponse(NanoHTTPD.Response.Status.OK, "multipart/x-mixed-replace; boundary=" + BOUNDARY, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "streamVideo: IOException in streamVideo", e);
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Error streaming video");
        }
    }

    private void displayIpAddress() {
        Log.d(TAG, "displayIpAddress: Displaying IP address");
        String ipAddress = getLocalIpAddress();
        if (ipAddress == null) {
            ipAddress = "Unknown IP";
            Log.w(TAG, "displayIpAddress: IP address is unknown");
        }
        String link = "http://" + ipAddress + ":" + PORT + "/video";
        ipTextView.setText("Connect to: " + link);
        Log.d(TAG, "displayIpAddress: IP address displayed as " + link);
    }

    private String getLocalIpAddress() {
        Log.d(TAG, "getLocalIpAddress: Getting local IP address");
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().length() < 16) {
                        Log.d(TAG, "getLocalIpAddress: Found IP address " + inetAddress.getHostAddress());
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "getLocalIpAddress: SocketException", e);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Destroying activity");
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
            Log.d(TAG, "onDestroy: Camera device closed");
        }
        if (server != null) {
            server.stop();
            Log.d(TAG, "onDestroy: NanoHTTPD server stopped");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            int newPort = data.getIntExtra("port", PORT);
            if (newPort != PORT) {
                PORT = newPort;
                Log.d(TAG, "onActivityResult: Port updated to " + PORT);
                startServer(PORT);
                displayIpAddress();
            }
        }
    }
    private byte[] yuv420ToNV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int yRowStride = image.getPlanes()[0].getRowStride();
        int uvRowStride = image.getPlanes()[1].getRowStride();
        int uvPixelStride = image.getPlanes()[1].getPixelStride();

        byte[] nv21 = new byte[width * height * 3 / 2];

        // Copy Y plane
        int pos = 0;
        for (int row = 0; row < height; row++) {
            int yRowStart = row * yRowStride;
            yBuffer.position(yRowStart);
            yBuffer.get(nv21, pos, width);
            pos += width;
        }

        // Interleave VU (not UV) for NV21
        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; row++) {
            int uvRowStart = row * uvRowStride;
            for (int col = 0; col < chromaWidth; col++) {
                int index = uvRowStart + col * uvPixelStride;
                vBuffer.position(index);
                uBuffer.position(index);
                nv21[pos++] = vBuffer.get(); // V
                nv21[pos++] = uBuffer.get(); // U
            }
        }

        return nv21;
    }
}