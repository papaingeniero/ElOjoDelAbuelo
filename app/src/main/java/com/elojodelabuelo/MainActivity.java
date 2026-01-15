package com.elojodelabuelo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hardware Preview Setup
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);
        // CRITICAL for Android 2.3 (deprecated in newer versions but required here)
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Buttons Implementation
        Button btnActivate = (Button) findViewById(R.id.btn_activate);
        Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);

        // Phase 9: Auto-start Surveillance on Launch
        Intent autoStartIntent = new Intent(this, SentinelService.class);
        startService(autoStartIntent);
        Toast.makeText(this, "Vigilancia Iniciada", Toast.LENGTH_SHORT).show();

        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentinelService.class);
                startService(intent);
                Toast.makeText(MainActivity.this, "Reiniciando...", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeactivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentinelService.class);
                stopService(intent);
                finish(); // Close App
            }
        });
    }

    // --- SurfaceHolder.Callback Implementation ---

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Connect Camera to Screen (Direct Hardware Path)
        SentinelService.setPreviewSurface(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // No logic needed, camera is already configured by Service
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Disconnect Camera to prevent crash when switching apps
        SentinelService.setPreviewSurface(null);
    }
}
