package com.example.mobilt_java23_karl_kowal_shakev4;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Sensorer
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor lightSensor;

    // UI-komponenter
    private ProgressBar progressBarX;
    private ProgressBar progressBarY;
    private ProgressBar progressBarZ;
    private ImageView rotateImage;
    private SeekBar seekBar;
    private boolean sensorsActive = false;
    private float totalRotation = 0;
    private long lastTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialisera SensorManager och specifika sensorer
        initSensors();
        // Tilldela UI-komponenterna
        initUIComponents();
        // Ställ in switchen för att aktivera och avaktivera sensorer
        SwitchCompat switchSensor = findViewById(R.id.switchSensor);
        switchSensor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activateSensors();
                Toast.makeText(this, "Sensors Activated", Toast.LENGTH_SHORT).show();
            } else {
                deactivateSensors();
                Toast.makeText(this, "Sensors Deactivated", Toast.LENGTH_SHORT).show();
                resetToDefault();
            }
        });
    }
    // Initialiserar SensorManager och sensorer
    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }
    // Initialiserar UI-komponenter
    private void initUIComponents() {
        // Initialisera ProgressBars för varje axel
        progressBarX = findViewById(R.id.progressBarX);
        progressBarY = findViewById(R.id.progressBarY);
        progressBarZ = findViewById(R.id.progressBarZ);
        progressBarX.setProgress(0);
        progressBarY.setProgress(0);
        progressBarZ.setProgress(0);
        // Initialisera övriga UI-komponenter
        rotateImage = findViewById(R.id.rotateImage);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(360); // Sätter maxvärdet till 360 grader
        seekBar.setProgress(180); // Startar i mitten av baren.
        seekBar.setEnabled(false); // Inaktivera användarinteraktion, endast indikator
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                handleAccelerometerData(event.values[0], event.values[1], event.values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                handleGyroscopeData(event.values[1], event.values[2], event.timestamp);
                break;
            case Sensor.TYPE_LIGHT:
                handleLightData(event.values[0]);
                break;
        }
    }
    // Hanterar accelerometerdata
    private void handleAccelerometerData(float x, float y, float z) {
        final float GRAVITY = 9.81f;
        float adjustedY = y - GRAVITY; //Av någon anledning så är telefonen i "freefall" ??? så tar bort gravitationen så att progressbarsen stämmer bättre  överen.
        // Skala upp värdena för att göra ProgressBar mer känslig
        int scaledProgressX = (int) (Math.abs(x) * 15); // För x-axeln
        int scaledProgressY = (int) (Math.abs(adjustedY) * 15); // För y-axeln
        int scaledProgressZ = (int) (Math.abs(z) * 15); // För z-axeln

        Log.d("mainActivity","Values: X:"+x+", Y:"+y+", Z:"+z);
        // Begränsa värdet till intervallet 0-100 för att matcha ProgressBar
        scaledProgressX = Math.min(scaledProgressX, 100);
        scaledProgressY = Math.min(scaledProgressY, 100);
        scaledProgressZ = Math.min(scaledProgressZ, 100);

        // Uppdatera respektive ProgressBar
        progressBarX.setProgress(scaledProgressX);
        progressBarY.setProgress(scaledProgressY);
        progressBarZ.setProgress(scaledProgressZ);

        // Minimum värden för x, y och z innan toasten kommer upp
        if (Math.abs(x) >= 12 || Math.abs(y) >= 12 || Math.abs(z) >= 12) {
            Toast.makeText(this, "Device shaken!", Toast.LENGTH_SHORT).show();
        }
    }
    // Hanterar gyroskopdata
    private void handleGyroscopeData(float yRotationRate, float zRotationRate, long timestamp) {
        // Kontrollerar om det är första gången metoden anropas
        if (lastTimestamp == 0) {
            // Sparar aktuellt tidsstämpel för framtida beräkningar
            lastTimestamp = timestamp;
            return; // Returnera tidigt eftersom vi inte kan beräkna rotation utan tidigare tidsstämpel
        }
        // Beräkna tiden från förra uppdateringen
        float deltaTime = (timestamp - lastTimestamp) * 1.0f / 1000000000.0f; // Nanosekunder till sekunder
        lastTimestamp = timestamp; // Uppdaterar för nästa anrop
        // Ändrar rotationshastigheten från radianer per sekund till grader per sekund
        float rotationInDegrees = yRotationRate * deltaTime * (180 / (float) Math.PI);
        totalRotation += rotationInDegrees;
        // Begränsar rotationsintervallet till -180 till 180 grader
        if (totalRotation > 180) {
            totalRotation -= 360; // Om över 180 grader, justera för att hålla inom intervallet
        } else if (totalRotation < -180) {
            totalRotation += 360; // Om under -180 grader, justera för att hålla inom intervallet
        }
        // Rotera bilden och uppdatera SeekBar
        rotateImage.setRotationY(totalRotation);
        seekBar.setProgress((int) (totalRotation + 180));
        // Kontrollera om rotationshastigheten överstiger tröskelvärdet
        if (Math.abs(yRotationRate) > 2 || Math.abs(zRotationRate) > 2) {
            Toast.makeText(this, "Quick rotation detected!", Toast.LENGTH_SHORT).show();
        }
    }
    // Hanterar ljussensordata
    private void handleLightData(float light) {
        View mainView = findViewById(R.id.main);
        if (light >= 1000) {
            mainView.setBackgroundColor(Color.RED); // Sätt bakgrunden till röd om ljusstyrkan är över 1000 lux
        } else {
            mainView.setBackgroundColor(Color.WHITE); // Sätt bakgrunden till vit annars
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Hantera förändringar i sensorens noggrannhet, om tillämpligt
    }
    // Metod för att aktivera sensorer
    private void activateSensors() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorsActive = true;
    }
    // Metod för att avaktivera sensorer
    private void deactivateSensors() {
        sensorManager.unregisterListener(this);
        sensorsActive = false;
    }
    // Metod för att återställa till standardläge
    private void resetToDefault() {
        totalRotation = 0; // Återställ rotationsvinkeln
        rotateImage.setRotationY(0); // Återställ bildrotation
        seekBar.setProgress(180); // Återställ SeekBar till 180
        progressBarX.setProgress(0); // Återställ X-axel ProgressBar till 0
        progressBarY.setProgress(0); // Återställ Y-axel ProgressBar till 0
        progressBarZ.setProgress(0); // Återställ Z-axel ProgressBar till 0
    }
    @Override
    protected void onPause() {
        super.onPause();
        deactivateSensors(); // Avaktivera sensorer när appen pausas
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorsActive) {
            activateSensors(); // Återaktivera sensorer när appen återupptas
        }
    }
}
