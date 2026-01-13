package com.elojodelabuelo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements SentinelService.UiPreviewCallback {

    private SentinelService service;
    private boolean isBound = false;
    private ImageView previewImage;
    private Bitmap currentBitmap;
    private long lastFrameTime = 0;

    // FPS Throttling for UI (15 FPS max)
    private static final long MIN_FRAME_INTERVAL_MS = 66;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            SentinelService.LocalBinder localBinder = (SentinelService.LocalBinder) binder;
            service = localBinder.getService();
            isBound = true;
            SentinelService.setUiCallback(MainActivity.this); // Register for frames
            SentinelService.logToWeb("MainActivity: Registered UiPreviewCallback");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            SentinelService.setUiCallback(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Window Flags for Wake & Brightness (Active Monitor)
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        previewImage = (ImageView) findViewById(R.id.camera_preview);
        previewImage.setVisibility(View.INVISIBLE); // Hidden until connected

        Button btnActivate = (Button) findViewById(R.id.btn_activate);
        Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);
        Button btnConnectVideo = (Button) findViewById(R.id.btn_connect_video);

        // Auto-start Service
        Intent autoStartIntent = new Intent(this, SentinelService.class);
        startService(autoStartIntent);

        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startService(new Intent(MainActivity.this, SentinelService.class));
                    Toast.makeText(MainActivity.this, "Servicio Iniciado", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                }
            }
        });

        btnDeactivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    stopService(new Intent(MainActivity.this, SentinelService.class));
                    Toast.makeText(MainActivity.this, "Servicio Detenido", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                }
            }
        });

        // Toggle Video Visibility
        btnConnectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewImage.getVisibility() == View.VISIBLE) {
                    previewImage.setVisibility(View.INVISIBLE);
                    SentinelService.logToWeb("MainActivity: Preview HIDDEN");
                } else {
                    applyZoomAndPan(); // Apply layout params before showing
                    previewImage.setVisibility(View.VISIBLE);
                    SentinelService.logToWeb("MainActivity: Preview SHOWN");
                }
            }
        });
    }

    private void applyZoomAndPan() {
        try {
            SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float zoom = prefs.getFloat("defaultZoom", 1.0f);
            int panX = prefs.getInt("defaultPanX", 0);
            int panY = prefs.getInt("defaultPanY", 0);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int width = (int) (metrics.widthPixels * zoom);
            int height = (int) (metrics.heightPixels * zoom);

            // Use FrameLayout.LayoutParams because standard ImageView is usually within one
            // (or root)
            // Root is FrameLayout in activity_main.xml
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);

            // Center gravity, then offset with margins
            params.gravity = android.view.Gravity.CENTER;
            params.leftMargin = -panX;
            params.topMargin = -panY;

            previewImage.setLayoutParams(params);
            SentinelService.logToWeb("MainActivity: Zoom Applied: " + zoom + "x");
        } catch (Exception e) {
            SentinelService.logToWeb("Error applying zoom: " + e.getMessage());
        }
    }

    @Override
    public void onFrame(final byte[] jpegData) {
        // Run on UI Thread to update Image
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Check Visibility & Throttling
                    if (previewImage.getVisibility() != View.VISIBLE)
                        return;

                    long now = System.currentTimeMillis();
                    if (now - lastFrameTime < MIN_FRAME_INTERVAL_MS)
                        return; // Skip frame
                    lastFrameTime = now;

                    // 2. Decode Bitmap
                    // Only decode if we have valid data
                    if (jpegData != null && jpegData.length > 0) {
                        Bitmap nextBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

                        if (nextBitmap != null) {
                            // 3. Set new Bitmap
                            previewImage.setImageBitmap(nextBitmap);

                            // 4. Recycle OLD Bitmap (Memory Safety)
                            if (currentBitmap != null && currentBitmap != nextBitmap && !currentBitmap.isRecycled()) {
                                currentBitmap.recycle();
                            }
                            currentBitmap = nextBitmap;
                        }
                    }
                } catch (OutOfMemoryError oom) {
                    System.gc(); // Panic GC
                    SentinelService.logToWeb("MainActivity: OOM Error!");
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Intent intent = new Intent(getApplicationContext(), SentinelService.class);
            getApplicationContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            getApplicationContext().unbindService(connection);
            isBound = false;
            SentinelService.setUiCallback(null);
        }
        // Cleanup on stop
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (previewImage.getVisibility() == View.VISIBLE) {
            applyZoomAndPan();
        }
    }
}
