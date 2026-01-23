package com.elojodelabuelo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MainUI";
    
    // Configuración original para restaurar al salir
    private int originalTimeout = -1;

    private BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SentinelService.logToWeb("MainActivity: Broadcast -> " + intent.getAction());
            String action = intent.getAction();
            
            if ("com.elojodelabuelo.ACTION_ZOOM_UPDATED".equals(action)) {
                applyZoomLogic();
            } 
            else if ("com.elojodelabuelo.ACTION_REC_START".equals(action)) {
                // ALARMA: El Servicio ha despertado el móvil.
                // Nosotros ponemos el brillo al MÁXIMO para intimidar/ver.
                setWindowBrightness(1.0f);
            } 
            else if ("com.elojodelabuelo.ACTION_REC_STOP".equals(action)) {
                // CALMA: El servicio soltó el bloqueo.
                // Forzamos apagado en 1 segundo (Modo Frío).
                setTimeout(1000); 
                setWindowBrightness(-1.0f); // Restaurar brillo automático
                Toast.makeText(context, "Enfriando... (Apagado en 1s)", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SentinelService.logToWeb("MainActivity: CREATED");
        try {
            // --- NUEVO: PASE VIP (Saltar bloqueo de pantalla) ---
            // FLAG_DISMISS_KEYGUARD: Quita el candado si no hay contraseña.
            // FLAG_SHOW_WHEN_LOCKED: Muestra la app aunque el móvil esté bloqueado.
            // FLAG_TURN_SCREEN_ON: Asegura que se enciende (refuerzo al WakeLock del servicio).
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                 WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                 WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            // ----------------------------------------------------

            setContentView(R.layout.activity_main);
            
            // 1. Guardar Timeout Original
            try {
                originalTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
            } catch (Exception e) { originalTimeout = 60000; } // Default 60s

            // 2. Setup Hardware
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surfaceView != null) {
                SurfaceHolder holder = surfaceView.getHolder();
                holder.addCallback(this);
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            // 3. Arrancar Servicio
            startService(new Intent(this, SentinelService.class));
            setupExitButton();
            
        } catch (Exception e) {
            Log.e(TAG, "Error onCreate", e);
        }
    }

    private void setupExitButton() {
        Button btnKill = (Button) findViewById(R.id.btn_kill_app);
        if (btnKill != null) {
            btnKill.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        SentinelService.logToWeb("MainActivity: User Kill Switch");
                        stopService(new Intent(MainActivity.this, SentinelService.class));
                        restoreOriginalSettings();
                        finish();
                    } catch (Exception e) { Log.e(TAG, "Error kill", e); }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SentinelService.logToWeb("MainActivity: RESUMED (Visible)");
        applyZoomLogic();
        
        // Al volver manual, restaurar timeout normal para poder usar el móvil
        if (originalTimeout > 0) setTimeout(originalTimeout);
        setWindowBrightness(-1.0f);

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.elojodelabuelo.ACTION_ZOOM_UPDATED");
            filter.addAction("com.elojodelabuelo.ACTION_REC_START");
            filter.addAction("com.elojodelabuelo.ACTION_REC_STOP");
            registerReceiver(systemReceiver, filter);
        } catch (Exception e) {}
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        SentinelService.logToWeb("MainActivity: PAUSED (Background)");
        try { unregisterReceiver(systemReceiver); } catch (Exception e) {}
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreOriginalSettings();
    }

    // --- UTILS ---
    private void setTimeout(int millis) {
        try { Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, millis); } 
        catch (Exception e) {}
    }

    private void setWindowBrightness(float val) {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = val;
            getWindow().setAttributes(lp);
        } catch (Exception e) {}
    }

    private void restoreOriginalSettings() {
        if (originalTimeout > 0) setTimeout(originalTimeout);
    }

    // --- ZOOM LOGIC ---
    private void applyZoomLogic() {
        try {
            SurfaceView surface = (SurfaceView) findViewById(R.id.cameraPreview);
            if (surface == null) return;
            android.content.SharedPreferences prefs = getSharedPreferences("SentinelPrefs", MODE_PRIVATE);
            float zoom = prefs.getFloat("defaultZoom", 1.0f);
            int panX = prefs.getInt("defaultPanX", 0);
            int panY = prefs.getInt("defaultPanY", 0);

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
        } catch (Exception e) { Log.e(TAG, "Error Zoom UI", e); }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { SentinelService.setPreviewSurface(holder); }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { SentinelService.setPreviewSurface(null); }
}
