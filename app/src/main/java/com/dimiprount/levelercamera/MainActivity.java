package com.dimiprount.levelercamera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomControls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener {


    
    TextView tvX, tvY;
    Sensor s;
    SensorManager sm;
    Camera cam;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean didItWork = false;
    LayoutInflater controlInflater = null;
    ViewGroup.LayoutParams layoutParamsControl;
    Camera.Parameters params;

    ZoomControls zoomControls;
    int currentZoomLevel;
    int maxZoomLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    params = cam.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    params.setRotation(90);
                    cam.setParameters(params);
                    cam.startPreview();
                    cam.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });


        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, new LinearLayout(getBaseContext()), false);

        layoutParamsControl = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.addContentView(viewControl, layoutParamsControl);

        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);

        zoomControls = (ZoomControls) findViewById(R.id.zoomControls);

        zoomControls.setIsZoomInEnabled(true);
        zoomControls.setIsZoomOutEnabled(true);
        zoomControls.setZoomSpeed(20)
    }

    protected void onResume() {
        super.onResume();
        try {
            cam.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            cam.release();
            cam = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            cam.release();
            cam = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
       
        cam = Camera.open();
        
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (didItWork) {
            cam.stopPreview();
            didItWork = false;
        }


        if (cam != null) {
            try {
                cam.setPreviewDisplay(surfaceHolder);
                cam.startPreview();
                params = cam.getParameters();
                didItWork = true;
                currentZoomLevel = params.getZoom();
                maxZoomLevel = params.getMaxZoom();
                if (params.isZoomSupported() && params.isSmoothZoomSupported()) {

                    zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (currentZoomLevel < maxZoomLevel) {
                                currentZoomLevel++;
                                cam.startSmoothZoom(currentZoomLevel);

                            }
                        }
                    });

                    zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (currentZoomLevel > 0) {
                                currentZoomLevel--;
                                cam.startSmoothZoom(currentZoomLevel);
                            }
                        }
                    });
                } else if (params.isZoomSupported() && !params.isSmoothZoomSupported()) {

                    zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (currentZoomLevel < maxZoomLevel) {
                                currentZoomLevel++;
                                params.setZoom(currentZoomLevel);
                                cam.setParameters(params);

                            }
                        }
                    });

                    zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (currentZoomLevel > 0) {
                                currentZoomLevel--;
                                params.setZoom(currentZoomLevel);
                                cam.setParameters(params);
                            }
                        }
                    });

                    cam.setParameters(params);
                } else {
                    zoomControls.setVisibility(View.GONE);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            cam.stopPreview();
            cam.release();
            cam = null;
            didItWork = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {
        
        @Override
        public void onShutter() {

        }
    };

    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            


        }
    };

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
           
            new SaveImageTask().execute(data);
            cam.startPreview();

        }
    };

    public class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream fos;

            
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/Leveler Camera");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File finalFile = new File(dir, fileName);

                fos = new FileOutputStream(finalFile);
                fos.write(data[0]);
                fos.flush();
                fos.close();


                refreshGallery(finalFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


    }


    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        tvY.setText(String.format("%.1f", event.values[0]));
        tvY.setRotation(270);
        tvX.setText(String.format("%.1f", event.values[1]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
