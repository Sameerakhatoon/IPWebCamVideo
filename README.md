# IP Webcam App

A simple Android app that turns your device into an IP webcam. Stream live video from your device's camera over HTTP and view it on any web browser.

![Project Demo](WebCamDemo.gif)

**Note:** This project was initially developed in a hurry, and there are plans to include audio streaming & saving files in future updates.

## Features

- **Live Video Streaming**: Streams live video from your device's camera using a built-in HTTP server.
- **Customizable Port Number**: Allows you to change the port number for the HTTP server through a settings page.
- **Automatic IP Address Detection**: Displays the IP address and port number for easy access to the video stream.

## Requirements

- Android 6.0 (Marshmallow) or higher
- Camera permission
- Internet access

## Permissions

- `CAMERA`: To access the device's camera.
- `INTERNET`: To serve the video stream over HTTP.
- `ACCESS_NETWORK_STATE`: To detect network state changes.

## Getting Started

1. **Clone the Repository**
    
    `git clone https://github.com/yourusername/ip-webcam-app.git`
    
2. **Open the Project**
    
    Open the project in Android Studio.
    
3. **Build and Run**
    
    Build and run the app on an Android device or emulator.
    
4. **Access the Video Stream**
    
    Once the app is running, you can access the video stream using the IP address and port number displayed in the app. Open a web browser and navigate to `http://<IP_ADDRESS>:<PORT>/video`.
    

## Configuration

You can change the port number used by the HTTP server through the settings activity:

1. Tap on the **Settings** button in the main activity.
2. Enter a new port number and tap **Save**.
3. The app will restart the server with the new port number.

## Future Plans

- **Audio Streaming**: We plan to add support for streaming audio from the device in future updates. Stay tuned!

## Code Structure

- **`MainActivity.java`**: Main activity handling camera setup, preview, and HTTP server.
- **`SettingsActivity.java`**: Activity for configuring the port number.
- **`activity_main.xml`**: Layout for the main activity, including `TextureView` for camera preview and a `TextView` for IP address display.
- **`activity_settings.xml`**: Layout for the settings activity, including an `EditText` for port number input and a `Button` for saving the settings.

## Troubleshooting

- **App Crashes on Settings Button**: Ensure that your `SettingsActivity` is correctly defined in the AndroidManifest.xml file and that the `SettingsActivity` layout file is properly set up.
    
- **Video Stream Not Displaying**: Check that your device has a stable network connection and that the port number is not being used by another application.
    

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes.

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Acknowledgements

- **[NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)**: A lightweight HTTP server for Java.
- **[Camera2 API](https://developer.android.com/reference/android/hardware/camera2/package-summary)**: Android API for camera access and control.

---

Feel free to tweak any part of this to better fit your project's specifics!
