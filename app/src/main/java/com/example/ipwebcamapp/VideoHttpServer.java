package com.example.ipwebcamapp;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoHttpServer {
    private static final String TAG = "VideoHttpServer";
    private final int port;
    private ServerSocket serverSocket;
    private boolean running;

    public VideoHttpServer(int port) {
        this.port = port;
        Log.d(TAG, "VideoHttpServer: Server initialized on port " + port);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Log.d(TAG, "start: ServerSocket created and server started on port " + port);

        new Thread(() -> {
            while (running) {
                try {
                    Log.d(TAG, "start: Waiting for client connection...");
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, "start: Client connected from " + clientSocket.getInetAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    Log.e(TAG, "start: IOException while accepting client connection", e);
                    if (!running) {
                        break;
                    }
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        Log.d(TAG, "stop: Stopping server...");
        try {
            serverSocket.close();
            Log.d(TAG, "stop: ServerSocket closed");
        } catch (IOException e) {
            Log.e(TAG, "stop: IOException while closing ServerSocket", e);
        }
    }

    public void broadcastVideoData(byte[] videoData) {
        Log.d(TAG, "broadcastVideoData: Broadcasting video data to clients");
        // Broadcast video data to all connected clients
        // This method will need implementation depending on how clients are managed
    }

    private void handleClient(Socket clientSocket) {
        try {
            Log.d(TAG, "handleClient: Handling client " + clientSocket.getInetAddress());
            OutputStream outputStream = clientSocket.getOutputStream();
            // Handle client communication and send video data
            // Implementation needed based on your application's requirements
            Log.d(TAG, "handleClient: Client handling complete");
        } catch (IOException e) {
            Log.e(TAG, "handleClient: IOException while handling client", e);
        } finally {
            try {
                clientSocket.close();
                Log.d(TAG, "handleClient: Client socket closed");
            } catch (IOException e) {
                Log.e(TAG, "handleClient: IOException while closing client socket", e);
            }
        }
    }
}
