package com.elojodelabuelo;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Clean Slate: No window flags here. Manifest handles it.
            setContentView(R.layout.activity_main);
            
            setupUI();
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error in onCreate", e);
        }
    }

    private void setupUI() {
        try {
            Button btnActivate = (Button) findViewById(R.id.btn_activate);
            Button btnExit = (Button) findViewById(R.id.btn_deactivate);
            
            if (btnActivate != null) {
                btnActivate.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            startService(new Intent(MainActivity.this, SentinelService.class));
                        } catch (Exception e) {
                            Log.e(TAG, "Error starting service", e);
                        }
                    }
                });
            }
            
            if (btnExit != null) {
                btnExit.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            stopService(new Intent(MainActivity.this, SentinelService.class));
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Error stopping service", e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupUI", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Force LayoutParams to ensure SurfaceView behaves
            // This is the "Safety Reset" for the SurfaceView
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
}
