package com.elojodelabuelo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnActivate = (Button) findViewById(R.id.btn_activate);
        Button btnDeactivate = (Button) findViewById(R.id.btn_deactivate);

        // Phase 9: Auto-start Surveillance on Launch
        Intent autoStartIntent = new Intent(this, SentinelService.class);
        startService(autoStartIntent);
        Toast.makeText(this, "Auto-Iniciando Vigilancia...", Toast.LENGTH_SHORT).show();

        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentinelService.class);
                startService(intent);
                Toast.makeText(MainActivity.this, "Servicio Iniciado", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeactivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentinelService.class);
                stopService(intent);
                Toast.makeText(MainActivity.this, "Servicio Detenido", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
