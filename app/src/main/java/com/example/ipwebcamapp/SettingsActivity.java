package com.example.ipwebcamapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText portEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_PORT = "port";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        portEditText = findViewById(R.id.portEditText);
        saveButton = findViewById(R.id.saveButton);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved port number
        int savedPort = sharedPreferences.getInt(KEY_PORT, 8080);
        portEditText.setText(String.valueOf(savedPort));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePortNumber();
            }
        });
    }

    private void savePortNumber() {
        try {
            int portNumber = Integer.parseInt(portEditText.getText().toString());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_PORT, portNumber);
            editor.apply();

            // Return the result to MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("port", portNumber);
            setResult(RESULT_OK, resultIntent);
            finish(); // Close the activity
        } catch (NumberFormatException e) {
            portEditText.setError("Invalid port number");
        }
    }
}
