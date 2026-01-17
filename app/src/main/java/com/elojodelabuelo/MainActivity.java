package com.elojodelabuelo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // 1. Configuración de Ventana
            // (ELIMINADO: Delegamos esto al Manifest "Theme.NoTitleBar.Fullscreen" para evitar conflictos)

            // 2. Cargar interfaz
            setContentView(R.layout.activity_main);

            // 3. Hardware Preview
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
            SurfaceHolder holder = surfaceView.getHolder();
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            // 4. Botones
            Button btnActivate = (Button) findViewById(R.id.btn_activate);
            Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);

            // 5. Auto-start
            Intent autoStartIntent = new Intent(this, SentinelService.class);
            startService(autoStartIntent);
            
            // Usar try-catch interno para el Toast por si el contexto no está listo
            try {
                Toast.makeText(this, "Vigilancia Iniciada", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {}

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
                    finish(); 
                }
            });
            
        } catch (Throwable t) {
            Log.e("Sentinel", "CRASH en onCreate: " + t.getMessage());
            t.printStackTrace();
            // Intentar recuperar visualmente si es posible
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Leer preferencias
            android.content.SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float zoom = prefs.getFloat("defaultZoom", 1.0f);
            int panX = prefs.getInt("defaultPanX", 0);
            int panY = prefs.getInt("defaultPanY", 0);
            
            SurfaceView surface = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surface == null) return;

            // DEBUG SEGURO
            Log.d("Sentinel", "Applying Zoom: " + zoom);
            try {
                Toast.makeText(this, "Zoom Disco: " + zoom + "x", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {}

            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenW = metrics.widthPixels;
            int screenH = metrics.heightPixels;
            
            // --- BLOQUE DE SEGURIDAD DE LAYOUT ---
            // Verificamos quién es el padre antes de forzar reglas
            android.view.ViewGroup.LayoutParams rawParams = surface.getLayoutParams();
            android.widget.FrameLayout.LayoutParams params = null;

            if (rawParams instanceof android.widget.FrameLayout.LayoutParams) {
                // Es seguro, procedemos
                if (zoom <= 1.05f) {
                     params = new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    );
                } else {
                    int targetW = (int) (screenW * zoom);
                    int targetH = (int) (screenH * zoom);
                    int baseLeft = (screenW - targetW) / 2;
                    int baseTop = (screenH - targetH) / 2;

                    params = new android.widget.FrameLayout.LayoutParams(targetW, targetH);
                    params.leftMargin = baseLeft + panX;
                    params.topMargin = baseTop + panY;
                    params.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT; 
                }
                surface.setLayoutParams(params);
                surface.requestLayout();
            } else {
                // Si entra aquí, el XML no se ha actualizado bien, pero NO crasheamos
                Log.e("Sentinel", "Error: El padre no es un FrameLayout. XML desactualizado.");
                Toast.makeText(this, "Error: Reinicia el móvil (XML Cache)", Toast.LENGTH_LONG).show();
            }

        } catch (Throwable t) {
            t.printStackTrace();
            Log.e("Sentinel", "Error UI onResume: " + t.getMessage());
        }
    }

    // --- SurfaceHolder.Callback ---

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Envolvemos esto también por si el servicio no está listo
        try {
            SentinelService.setPreviewSurface(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            SentinelService.setPreviewSurface(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
