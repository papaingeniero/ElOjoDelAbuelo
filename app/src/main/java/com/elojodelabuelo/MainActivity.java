package com.elojodelabuelo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

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
            // If surface is already ready (and lazy loaded), attach it now
            if (monitorView != null && monitorView.getHolder().getSurface().isValid()) {
                try {
                    service.attachSurface(monitorView.getHolder());
                } catch (Exception e) {
                    SentinelService.logToWeb("MainActivity: Auto-Attach Error: " + e.getMessage());
                }
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
        SentinelService.logToWeb("MainActivity: onCreate START");
        try {
            // Window flags commented out for debugging stability
            // Window window = getWindow();
            // window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            // WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            // WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            // WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            setContentView(R.layout.activity_main);
            SentinelService.logToWeb("MainActivity: setContentView SUCCESS");

            // monitorView is NOT initialized here. Lazy loading strategy.
            SentinelService.logToWeb("MainActivity: SurfaceView SKIPPED (LAZY LOADING)");

            Button btnActivate = (Button) findViewById(R.id.btn_activate);
            Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);
            Button btnConnectVideo = (Button) findViewById(R.id.btn_connect_video); // DEBUG

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

            // DEBUG: Manual Video Connect (Lazy Load)
            btnConnectVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        SentinelService.logToWeb("MainActivity: Manual Connect Triggered");

                        // Initialize SurfaceView Programmatically
                        if (monitorView == null) {
                            FrameLayout container = (FrameLayout) findViewById(R.id.monitor_container);
                            if (container != null) {
                                monitorView = new SurfaceView(MainActivity.this);
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT);
                                monitorView.setLayoutParams(params);
                                monitorView.getHolder().addCallback(MainActivity.this);
                                container.addView(monitorView, 0); // Add at index 0 (behind buttons)
                                SentinelService.logToWeb("MainActivity: Lazy SurfaceView Added");
                                Toast.makeText(MainActivity.this, "Creando Superficie...", Toast.LENGTH_SHORT).show();
                            } else {
                                SentinelService.logToWeb("MainActivity: Fatal - Container not found");
                            }
                        } else {
                            // If already added, just try to attach
                            if (isBound && service != null) {
                                service.attachSurface(monitorView.getHolder());
                                Toast.makeText(MainActivity.this, "Re-conectando...", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Servicio no conectado", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        SentinelService.logToWeb("MainActivity: Manual Connect ERROR: " + e.getMessage());
                        e.printStackTrace();
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
        SentinelService.logToWeb("MainActivity: onStart START");
        try {
            Intent intent = new Intent(getApplicationContext(), SentinelService.class);
            boolean result = getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
            SentinelService.logToWeb("MainActivity: bindService CALLED. Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error binding service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            getApplicationContext().unbindService(connection);
            isBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SentinelService.logToWeb("MainActivity: onResume START");
        try {
            // Phase 26: Apply Digital Zoom to SurfaceView Layout
            if (monitorView != null) {
                SentinelService.logToWeb("MainActivity: onResume CONFIG SURFACE");
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
                params.gravity = android.view.Gravity.CENTER;
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
        SentinelService.logToWeb("MainActivity: surfaceCreated");
        try {
            if (isBound && service != null) {
                service.attachSurface(holder);
                SentinelService.logToWeb("MainActivity: surfaceCreated ATTACHED");
            }
        } catch (Exception e) {
            SentinelService.logToWeb("MainActivity: surfaceCreated ERROR: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handled by Service
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            if (isBound && service != null) {
                service.detachSurface();
                SentinelService.logToWeb("MainActivity: surfaceDestroyed DETACHED");
            }
        } catch (Exception e) {
            SentinelService.logToWeb("MainActivity: surfaceDestroyed ERROR: " + e.getMessage());
        }
    }
}
