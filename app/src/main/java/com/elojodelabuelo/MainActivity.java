package com.elojodelabuelo;

import android.app.Activity;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. CLEAN SETUP: Sin window flags conflictivas (El Manifest manda)
        try {
            setContentView(R.layout.activity_main);
            
            // 2. Hardware Preview Setup (Vital para Android 2.3)
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surfaceView != null) {
                SurfaceHolder holder = surfaceView.getHolder();
                holder.addCallback(this);
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            // 3. Auto-Start del Cerebro (Servicio)
            startService(new Intent(this, SentinelService.class));
            
            // 4. Botones
            setupExitButton();
            
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error in onCreate", e);
        }
    }

    // Método de configuración del botón único
    private void setupExitButton() {
        try {
            Button btnKill = (Button) findViewById(R.id.btn_kill_app);
            if (btnKill != null) {
                btnKill.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // ACCIÓN: Matar servicio y cerrar app
                        try {
                            stopService(new Intent(MainActivity.this, SentinelService.class));
                            Toast.makeText(MainActivity.this, "Sistema Apagado. Hasta luego.", Toast.LENGTH_LONG).show();
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Error al apagar", e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en setupExitButton", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyZoomLogic();
    }
    
    // --- LA FÓRMULA DEL SANDBOX (VALIDADA) ---
    private void applyZoomLogic() {
        try {
            SurfaceView surface = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surface == null) return;

            // Leer Preferencias
            android.content.SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float zoom = prefs.getFloat("defaultZoom", 1.0f);
            int panX = prefs.getInt("defaultPanX", 0);
            int panY = prefs.getInt("defaultPanY", 0);

            // Feedback visual
            if (zoom > 1.01f) {
                Toast.makeText(this, "Zoom: " + zoom + "x", Toast.LENGTH_SHORT).show();
            }

            // Calcular Pantalla
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenW = metrics.widthPixels;
            int screenH = metrics.heightPixels;

            FrameLayout.LayoutParams params;

            if (zoom <= 1.01f) {
                // MODO NATIVO
                params = new FrameLayout.LayoutParams(-1, -1); // MATCH_PARENT
            } else {
                // MODO ZOOM (Hardware Scaling)
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
            surface.requestLayout(); // Forzar al driver

        } catch (Exception e) {
            Log.e(TAG, "Error Zoom UI", e);
        }
    }

    // --- CONEXIÓN CON EL SERVICIO (NO TOCAR) ---
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
