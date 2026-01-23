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
import android.widget.FrameLayout; // Ya no se usa para zoom, pero lo dejo por si acaso
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
            
            // ELIMINADO: Ya no reaccionamos a ZOOM_UPDATED aquí porque es Hardware.
            
            if ("com.elojodelabuelo.ACTION_REC_START".equals(action)) {
                // ALARMA: El Servicio ha despertado el móvil.
                setWindowBrightness(1.0f);
            } 
            else if ("com.elojodelabuelo.ACTION_REC_STOP".equals(action)) {
                // CALMA: El servicio soltó el bloqueo.
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
            // PASE VIP (Saltar bloqueo)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                 WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                 WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            setContentView(R.layout.activity_main);
            
            // 1. Guardar Timeout Original
            try {
                originalTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
            } catch (Exception e) { originalTimeout = 60000; } 

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
        
        // 1. ELIMINADO: applyZoomLogic(); 
        // ¡Ya no hacemos zoom por software! Dejamos la vista tranquila para no gastar CPU.
        
        // 2. RESTAURAR TIMEOUT
        if (originalTimeout > 0) setTimeout(originalTimeout);
        setWindowBrightness(-1.0f);

        // 3. REGISTRAR RECEIVER
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.elojodelabuelo.ACTION_REC_START");
            filter.addAction("com.elojodelabuelo.ACTION_REC_STOP");
            registerReceiver(systemReceiver, filter);
        } catch (Exception e) {}

        // 4. [CRÍTICO] AVISAR AL PINTOR VAGO: "ESTOY VIVO" 💡🟢
        // Le pasamos un callback (aunque esté vacío) para que uiPreviewCallback != null
        // y el servicio sepa que estamos mirando.
        SentinelService.setUiCallback(new SentinelService.UiPreviewCallback() {
            @Override
            public void onFrame(byte[] jpegData) {
                // No hacemos nada con el JPEG porque el SurfaceView lo pinta por Hardware.
                // Pero este callback mantiene al servicio despierto cuando la pantalla está ON.
            }
        });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        SentinelService.logToWeb("MainActivity: PAUSED (Background)");
        try { unregisterReceiver(systemReceiver); } catch (Exception e) {}

        // 5. [CRÍTICO] AVISAR AL PINTOR VAGO: "ME VOY A DORMIR" 💡🔴
        // Esto pone uiPreviewCallback = null.
        // Al ocurrir esto, el 'processFrame' del servicio entra en MODO 1 FPS (Enfriamiento).
        if (SentinelService.instance != null) { // Check de seguridad (aunque instance es static)
             SentinelService.setUiCallback(null);
        }
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

    // --- ZOOM LOGIC ELIMINADA ---
    // Hemos quitado applyZoomLogic para evitar crashes de memoria y calor.
    // El zoom ahora lo gestiona el Hardware de la cámara en SentinelService.

    @Override
    public void surfaceCreated(SurfaceHolder holder) { SentinelService.setPreviewSurface(holder); }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { SentinelService.setPreviewSurface(null); }
}