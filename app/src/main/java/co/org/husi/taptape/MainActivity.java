package co.org.husi.taptape;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mNombreArchivo = null;

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private Button mBotonGrabar = null;
    private Button mBotonReproducir = null;

    private Chronometer mCronometro = null;

    private TextView mMovimientoTexto = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permisoParaGrabar = false;
    private String[] permisos = {Manifest.permission.RECORD_AUDIO};

    private boolean mEmpezarGrabar;
    private boolean mEmpezarReproducir;

    // Start with some variables
    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permisoParaGrabar = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permisoParaGrabar) finish();

    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Pedir permisos para grabar
        ActivityCompat.requestPermissions(this, permisos, REQUEST_RECORD_AUDIO_PERMISSION);

        setContentView(R.layout.activity_main);

        //Grabar en el directorio de cache externo para visibilidad
        mNombreArchivo = getExternalCacheDir().getAbsolutePath();
        mNombreArchivo += "/grabaciontest.3gp";

        mBotonGrabar = (Button) findViewById(R.id.grabarBoton);
        mBotonReproducir = (Button) findViewById(R.id.reproducirBoton);
        mCronometro = (Chronometer) findViewById(R.id.cronometroTexto);
        mMovimientoTexto = (TextView) findViewById(R.id.movimientoTexto);

        mEmpezarGrabar = true;
        mBotonGrabar.setText("Start recording");
        mBotonGrabar.setOnClickListener(presionoGrabar());

        mEmpezarReproducir = true;
        mBotonReproducir.setText("Start playing");
        mBotonReproducir.setOnClickListener(presionoReproducir());


        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void mensajeTap(){
        long segundosRegistrados = (SystemClock.elapsedRealtime() - mCronometro.getBase())/1000;
        mMovimientoTexto.setText("Tap: " + segundosRegistrados);
    }

    private View.OnClickListener presionoGrabar() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                grabar(mEmpezarGrabar);
                if (mEmpezarGrabar) {
                    mBotonGrabar.setText("Stop recording");
                    mCronometro.setBase(SystemClock.elapsedRealtime());
                    mCronometro.start();
                } else {
                    mBotonGrabar.setText("Start recording");
                    mCronometro.stop();
                }
                mEmpezarGrabar = !mEmpezarGrabar;
            }
        };
    }

    private void grabar(boolean empezar) {
        if (empezar) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mNombreArchivo);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            mRecorder.start();
        } else {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private View.OnClickListener presionoReproducir() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reproducir(mEmpezarReproducir);
                if (mEmpezarReproducir) {
                    mBotonReproducir.setText("Stop playing");
                } else {
                    mBotonReproducir.setText("Start playing");
                }
                mEmpezarReproducir = !mEmpezarReproducir;
            }
        };
    }

    private void reproducir(boolean empezar) {
        if (empezar) {
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mNombreArchivo);
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }
        } else {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if ((event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) && !mEmpezarGrabar){
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x*x + y*y + z*z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > 5){
                mensajeTap();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onResume() {
        super.onResume();
        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }



}