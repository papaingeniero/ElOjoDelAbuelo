package com.elojodelabuelo;

import android.app.Activity;
import android.content.Context; // Added for BIND_AUTO_CREATE
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private SentinelService service;
    private boolean isBound = false;
    private SurfaceView monitorView;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            SentinelService.LocalBinder localBinder = (SentinelService.LocalBinder) binder;
            service = localBinder.getService();
            isBound = true;
            // If surface is already ready, attach it now
            if (monitorView != null && monitorView.getHolder().getSurface().isValid()) {
                service.attachSurface(monitorView.getHolder());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Phase 26: Active Monitor - Wake & Brightness
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.screenBrightness = 1.0f; // Max Brightness
            window.setAttributes(layoutParams);

            setContentView(R.layout.activity_main);

            monitorView = (SurfaceView) findViewById(R.id.camera_monitor);
            monitorView.getHolder().addCallback(this);

            Button btnActivate = (Button) findViewById(R.id.btn_activate);
            Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);

            // Phase 9: Auto-start Surveillance on Launch
            Intent autoStartIntent = new Intent(this, SentinelService.class);
            startService(autoStartIntent);
            Toast.makeText(this, "Auto-Iniciando Vigilancia...", Toast.LENGTH_SHORT).show();

            btnActivate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(MainActivity.this, SentinelService.class);
                        startService(intent);
                        Toast.makeText(MainActivity.this, "Servicio Iniciado", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error btnStart: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });

            btnDeactivate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(MainActivity.this, SentinelService.class);
                        stopService(intent);
                        Toast.makeText(MainActivity.this, "Servicio Detenido", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error btnStop: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error onCreate: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Intent intent = new Intent(this, SentinelService.class);
            // Fix: Use BIND_AUTO_CREATE to ensure service creation and binding
            bindService(intent, connection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error binding service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Phase 26: Apply Digital Zoom to SurfaceView Layout
            if (monitorView != null) {
                SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
                float zoom = prefs.getFloat("defaultZoom", 1.0f);
                int panX = prefs.getInt("defaultPanX", 0);
                int panY = prefs.getInt("defaultPanY", 0);

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                int width = (int) (metrics.widthPixels * zoom);
                int height = (int) (metrics.heightPixels * zoom);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
                params.leftMargin = -panX;
                params.topMargin = -panY;
                params.gravity = android.view.Gravity.CENTER; // Center first, then offset
                monitorView.setLayoutParams(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error onResume: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Surface Callbacks for Zero-Copy
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (isBound && service != null) {
            service.attachSurface(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handled by Service
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (isBound && service != null) {
            service.detachSurface();
        }
    }
}
