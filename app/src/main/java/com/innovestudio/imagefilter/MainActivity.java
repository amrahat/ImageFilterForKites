package com.innovestudio.imagefilter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, SeekBar.OnSeekBarChangeListener {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private static final int SELECT_PHOTO = 2000;
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 9;
    private static final int CAMERA_PERMISSION_CODE = 10;
    private static final int REQUEST_IMAGE_CAPTURE = 3000;
    private Button selectFromGallery;
    private String TAG = "MainActivity";
    private ImageView imageView;
    private LinearLayout colorView;
    private Button save;
    private Bitmap imgbmp;
    private SeekBar seekBar;
    private TextView toleranceLevel;
    private int newColor, targetColor;
    private int selectedX = -1, selectedY = -1;
    private Button camera;
    private Bitmap lastTouchedBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListensers();
    }

    private void initListensers() {
        selectFromGallery.setOnClickListener(this);
        imageView.setOnTouchListener(this);
        save.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        camera.setOnClickListener(this);
    }

    private void initView() {
        selectFromGallery = (Button) findViewById(R.id.gallery);
        imageView = (ImageView) findViewById(R.id.image);
        colorView = (LinearLayout) findViewById(R.id.color_view);
        save = (Button) findViewById(R.id.save);
        camera = (Button) findViewById(R.id.camera);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        toleranceLevel = (TextView) findViewById(R.id.tolerance);
    }

    @Override
    public void onClick(View view) {
        if (view == selectFromGallery) {
            openImageSelector();
        } else if (view == save) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    saveBitmap(imgbmp);
                } else {
                    requestStoragePermission();
                }
            } else {
                saveBitmap(imgbmp);
            }
        } else if (view == camera) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkPermission("android.permission.CAMERA")) {
                    openCamera();
                } else {
                    requestCameraPermission();
                }
            } else {
                openCamera();
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private boolean checkPermission(String permission) {

        int res = checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    private void saveBitmap(Bitmap imgbmp) {
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        Integer counter = 0;
        File mediaFolder = new File(path, "ImageFilter");
        mediaFolder.mkdirs();
        File file = new File(mediaFolder, getTimeStamp() + ".png"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        try {
            fOut = new FileOutputStream(file);
            imgbmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream

            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

            Toast.makeText(this, "Image Saved in ImageFilter directory", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private String getTimeStamp() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }

    private void openImageSelector() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (data != null) {
                    Log.d(TAG, "onActivityResult: " + data.getData().getPath());
                    imageView.setImageURI(data.getData());
                    selectedX = selectedY = -1;
                    save.setVisibility(View.GONE);
                    seekBar.setProgress(0);
                }
                break;
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);
                    selectedX = selectedY = -1;
                    save.setVisibility(View.GONE);
                    seekBar.setProgress(0);

                }
                break;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == imageView) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    save.setVisibility(View.VISIBLE);
                    Log.d(TAG, "onTouch: X" + motionEvent.getX());
                    Log.d(TAG, "onTouch: Y" + motionEvent.getY());
                    selectedX = (int) motionEvent.getX();
                    selectedY = (int) motionEvent.getY();
                    setColorInPosition();

                    break;
            }
            return true;
        }
        return false;
    }

    private void setColorInPosition() {
        imageView.setDrawingCacheEnabled(true);
        imgbmp = Bitmap.createBitmap(imageView.getDrawingCache());
        imageView.setDrawingCacheEnabled(false);
        int pxl = imgbmp.getPixel(selectedX, selectedY);

        targetColor = getColorFromPixel(pxl);

        /*int A = Color.alpha(pxl);
        int R = Color.red(pxl);
        int G = Color.green(pxl);
        int B = Color.blue(pxl);
        // take conversion up to one single value
        R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);
        // set new pixel color to output bitmap*/
        newColor = Color.argb(255, 128, 128, 128);

        // colorView.setBackgroundColor(targetColor);

        floodFill();

        //imgbmp.recycle();
    }

    private void floodFill() {
        lastTouchedBmp = Bitmap.createBitmap(imgbmp);
        QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(imgbmp, targetColor, newColor);
        queueLinearFloodFiller.useImage(imgbmp);
        queueLinearFloodFiller.setTargetColor(targetColor);
        queueLinearFloodFiller.setFillColor(newColor);
        Log.d(TAG, "floodFill: progress"+seekBar.getProgress());
        queueLinearFloodFiller.setTolerance(seekBar.getProgress());
        queueLinearFloodFiller.floodFill(selectedX, selectedY);

        imageView.setImageBitmap(imgbmp);
    }

    private void floodFillTolerance() {
        imgbmp = Bitmap.createBitmap(lastTouchedBmp);
        QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(imgbmp, targetColor, newColor);
        /*queueLinearFloodFiller.useImage(imgbmp);
        queueLinearFloodFiller.setTargetColor(targetColor);
        queueLinearFloodFiller.setFillColor(newColor);*/
        Log.d(TAG, "floodFill: progress"+seekBar.getProgress());
        queueLinearFloodFiller.setTolerance(seekBar.getProgress());
        queueLinearFloodFiller.floodFill(selectedX, selectedY);

        imageView.setImageBitmap(imgbmp);
    }


    private int getColorFromPixel(int pxl) {
        int redComponent = Color.red(pxl);
        int greenComponent = Color.green(pxl);
        int blueComponent = Color.blue(pxl);
        Log.d(TAG, "getColorFromPixel: " + redComponent + "," + greenComponent + "," + blueComponent);

        return Color.argb(255, redComponent, greenComponent, blueComponent);

    }

    private void requestStoragePermission() {
        final String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //Asking request Permissions
        ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, EXTERNAL_STORAGE_PERMISSION_CODE);
    }

    private void requestCameraPermission() {
        final String[] PERMISSIONS_CAMERA = {Manifest.permission.CAMERA};
        //Asking request Permissions
        ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveBitmap(imgbmp);
                }
                break;
            case CAMERA_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        toleranceLevel.setText(i + "");

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (selectedX != -1 && selectedY != -1) {
            floodFillTolerance();
        }
    }
}
