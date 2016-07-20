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


    //private static final String TAG = "CamTestActivity";
    TextView tvX, tvY;
    Sensor s;
    SensorManager sm;
    Camera cam;
    SurfaceView surfaceView;    //Provides a dedicated drawing surface embedded inside of a view hierarchy. You can control the format of this surface and, if you like, its size; the SurfaceView takes care of placing the surface at the correct location on the screen
    SurfaceHolder surfaceHolder;    // Abstract interface to someone holding a display surface. Allows you to control the surface size and format, edit the pixels in the surface, and monitor changes to the surface.
    boolean didItWork = false;
    LayoutInflater controlInflater = null;  // Instantiates a layout XML file into its corresponding View objects.
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

        // getWindow() to get window and set it's pixel format which is UNKNOWN
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


        surfaceHolder = surfaceView.getHolder();    // Return the SurfaceHolder providing access and control over this SurfaceView's underlying surface.
        surfaceHolder.addCallback(this);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, new LinearLayout(getBaseContext()), false);

        layoutParamsControl = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);  // width, height
        this.addContentView(viewControl, layoutParamsControl);

        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);   // Create Sensor Manager
        s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Accelerometer Sensor
        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);// Register Sensor Listener
        // SensorManager.SENSOR_DELAY_NORMAL: report data to the screen at a normal speed

        zoomControls = (ZoomControls) findViewById(R.id.zoomControls);

        zoomControls.setIsZoomInEnabled(true);
        zoomControls.setIsZoomOutEnabled(true);
        zoomControls.setZoomSpeed(20);  // 20: milliseconds (float)
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
        // if (cam == null) {
        cam = Camera.open();
        //   }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (didItWork) {
            cam.stopPreview();
            // cam.release();
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
        /** ShutterCallback: Called as near as possible to the moment when a photo is captured from the sensor.
         * This is a good opportunity to play a shutter sound or give other feedback of camera operation.
         * This may be some time after the photo was triggered, but some time before the actual data is available.
         */
        @Override
        public void onShutter() {

        }
    };

    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            /**Called when image data is available after a picture is taken.
             * The format of the data depends on the context of the callback and Camera.Parameters settings.
             */


        }
    };

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
           /* Uri uriTarget = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
            OutputStream imageFile;

            try {
                assert uriTarget != null;
                imageFile = getContentResolver().openOutputStream(uriTarget);
                assert imageFile != null;
                imageFile.write(data);              // wrong
                imageFile.flush();
                imageFile.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            new SaveImageTask().execute(data);
            //  Log.d(TAG, "onPictureTaken - jpeg");
            cam.startPreview();

        }
    };

    public class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream fos;

            // Write to SD Card
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

                //    Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

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
        // Not in use
    }

}