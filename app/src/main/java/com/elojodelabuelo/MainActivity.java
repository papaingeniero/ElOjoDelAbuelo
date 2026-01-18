package com.elojodelabuelo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainUI";
    
    // --- NUEVO: La Antena de Radio ---
    private BroadcastReceiver zoomReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.elojodelabuelo.ACTION_ZOOM_UPDATED".equals(intent.getAction())) {
                // ¡Magia! Recibimos la orden y aplicamos el cambio en caliente
                // Toast.makeText(context, "Recibiendo ajustes remotos...", Toast.LENGTH_SHORT).show(); // Opcional
                applyZoomLogic();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surfaceView != null) {
                SurfaceHolder holder = surfaceView.getHolder();
                holder.addCallback(this);
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            startService(new Intent(this, SentinelService.class));
            setupExitButton();
            
        } catch (Exception e) {
            Log.e(TAG, "Error crítico en onCreate", e);
        }
    }

    private void setupExitButton() {
        Button btnKill = (Button) findViewById(R.id.btn_kill_app);
        if (btnKill != null) {
            btnKill.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        stopService(new Intent(MainActivity.this, SentinelService.class));
                        Toast.makeText(MainActivity.this, "Apagando Ojo...", Toast.LENGTH_LONG).show();
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al apagar", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 1. Aplicar Zoom inicial
        applyZoomLogic();
        
        // 2. Encender la Radio (Escuchar cambios)
        try {
            IntentFilter filter = new IntentFilter("com.elojodelabuelo.ACTION_ZOOM_UPDATED");
            registerReceiver(zoomReceiver, filter);
        } catch (Exception e) {
            Log.e(TAG, "Error registrando receiver", e);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 3. Apagar la Radio al salir (Ahorro de batería y estabilidad)
        try {
            unregisterReceiver(zoomReceiver);
        } catch (Exception e) {
            // Ignorar si no estaba registrado
        }
    }
    
    private void applyZoomLogic() {
        try {
            SurfaceView surface = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surface == null) return;

            android.content.SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float zoom = prefs.getFloat("defaultZoom", 1.0f);
            int panX = prefs.getInt("defaultPanX", 0);
            int panY = prefs.getInt("defaultPanY", 0);

            if (zoom > 1.01f) {
                // Feedback visual sutil
                Toast.makeText(this, "Zoom: " + zoom + "x", Toast.LENGTH_SHORT).show();
            }

            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenW = metrics.widthPixels;
            int screenH = metrics.heightPixels;

            FrameLayout.LayoutParams params;

            if (zoom <= 1.01f) {
                params = new FrameLayout.LayoutParams(-1, -1);
            } else {
                int targetW = (int) (screenW * zoom);
                int targetH = (int) (screenH * zoom);
                int baseLeft = (screenW - targetW) / 2;
                int baseTop = (screenH - targetH) / 2;

                params = new FrameLayout.LayoutParams(targetW, targetH);
                params.leftMargin = baseLeft + panX;
                params.topMargin = baseTop + panY;
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT; 
            }
            
            surface.setLayoutParams(params);
            surface.requestLayout();

        } catch (Exception e) {
            Log.e(TAG, "Error Zoom UI", e);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        SentinelService.setPreviewSurface(holder);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        SentinelService.setPreviewSurface(null);
    }
}
