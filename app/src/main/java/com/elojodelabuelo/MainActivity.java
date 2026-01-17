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

// RECUPERAMOS: implements SurfaceHolder.Callback (Vital para ver la cámara)
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // 1. CLEAN SETUP: Confiamos en el Manifest para el FullScreen
            setContentView(R.layout.activity_main);
            
            // 2. RECUPERAMOS: Configuración de Hardware (Vital para Android 2.3)
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surfaceView != null) {
                SurfaceHolder holder = surfaceView.getHolder();
                holder.addCallback(this);
                // LA LÍNEA MÁGICA DEL GALAXY S:
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            // 3. RECUPERAMOS: Auto-Start del Servicio (Para que vaya la Web)
            Intent autoStartIntent = new Intent(this, SentinelService.class);
            startService(autoStartIntent);
            
            // Feedback visual seguro
            try { Toast.makeText(this, "Vigilancia Iniciada", Toast.LENGTH_SHORT).show(); } catch(Exception e){}

            setupButtons();
            
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error in onCreate", e);
        }
    }

    private void setupButtons() {
        try {
            Button btnActivate = (Button) findViewById(R.id.btn_activate);
            Button btnExit = (Button) findViewById(R.id.btn_deactivate);
            
            if (btnActivate != null) {
                btnActivate.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        startService(new Intent(MainActivity.this, SentinelService.class));
                        Toast.makeText(MainActivity.this, "Reiniciando...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (btnExit != null) {
                btnExit.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        stopService(new Intent(MainActivity.this, SentinelService.class));
                        finish();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setupButtons", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // 4. MANTENEMOS: Estabilidad visual (Sin Zoom por ahora)
            // Esto asegura que la superficie tenga el tamaño correcto para pintar
            SurfaceView sv = (SurfaceView) findViewById(R.id.cameraPreview);
            if (sv != null) {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 
                    FrameLayout.LayoutParams.MATCH_PARENT
                );
                sv.setLayoutParams(params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    // --- 5. RECUPERAMOS: El Puente Cámara <-> Pantalla ---

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Conectar la tubería de video
        SentinelService.setPreviewSurface(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Nada que hacer aquí, el servicio controla el tamaño
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Desconectar para evitar fugas de memoria
        SentinelService.setPreviewSurface(null);
    }
}
