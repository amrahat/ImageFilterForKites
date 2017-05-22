package com.amrahat.imagefilter.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amrahat.imagefilter.R;
import com.amrahat.imagefilter.asynctask.GrayScaleTask;
import com.amrahat.imagefilter.interfaces.ImageViewOnTouchActionUp;
import com.amrahat.imagefilter.interfaces.OnGreyScalingDone;
import com.amrahat.imagefilter.view.ZoomableImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, SeekBar.OnSeekBarChangeListener, OnGreyScalingDone, ImageViewOnTouchActionUp {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private static final int SELECT_PHOTO = 2000;
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 9;
    private static final int CAMERA_PERMISSION_CODE = 10;
    private static final int REQUEST_IMAGE_CAPTURE = 3000;
    private static final int TARGET_WIDTH = 1024;
    private Button selectFromGallery;
    private String TAG = "MainActivity";
    private ZoomableImageView imageView;
    private LinearLayout colorView;
    private Button save;
    private Bitmap imgbmp;
    private SeekBar seekBar;
    private TextView toleranceLevel;
    private int newColor, targetColor;
    private int selectedX = -1, selectedY = -1;
    private Button camera;
    private Bitmap lastTouchedBmp, zoomedOutBitmap;
    private ContentValues values;
    private Uri capturedImageUri;
    private boolean forCamera = false;
    private float startY, startX;
    GrayScaleTask grayScaleTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grayScaleTask = new GrayScaleTask(this, this);

        initView();
        initListensers();
    }

    private void initListensers() {
        selectFromGallery.setOnClickListener(this);
        //imageView.setOnTouchListener(this);
        imageView.setImageViewOnActionUp(this);
        save.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        camera.setOnClickListener(this);
    }

    private void initView() {
        selectFromGallery = (Button) findViewById(R.id.gallery);
        imageView = (ZoomableImageView) findViewById(R.id.image);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    saveBitmap(imgbmp);
                } else {
                    requestStoragePermission();
                }
            } else {
                saveBitmap(imgbmp);
            }
        } else if (view == camera) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkPermission("android.permission.CAMERA")) {
                    if (checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
                        openCamera();
                    } else {
                        forCamera = true;
                        requestStoragePermission();

                    }
                } else {
                    requestCameraPermission();
                }
            } else {
                openCamera();
            }
        }
    }

    private void openCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        capturedImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }*/
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
                    Log.d(TAG, "onActivityResult: " + data.getData().toString());
                    Bitmap bitmap = null;
                    try {
                        //bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        imageView.setImageResource(0);
                        bitmap = handleSamplingAndRotationBitmap(getApplicationContext(), data.getData());
                        Bitmap resizedBmp = getResizedBitmap(bitmap);
                        Log.d(TAG, "onActivityResult: " + bitmap.getWidth());
                        imageView.setImageBitmap(resizedBmp);
                        //zoomedOutBitmap = null;

                        zoomedOutBitmap = resizedBmp.copy(Bitmap.Config.ARGB_8888, true);
                        selectedX = selectedY = -1;
                        save.setVisibility(View.GONE);
                        seekBar.setProgress(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
                break;
            case REQUEST_IMAGE_CAPTURE:

                if (resultCode == RESULT_OK) {


                    try {
                        /*Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(), capturedImageUri);*/
                        imageView.setImageResource(0);
                        Bitmap imageBitmap = handleSamplingAndRotationBitmap(getApplicationContext(), capturedImageUri);
                        Bitmap resizedBmp = getResizedBitmap(imageBitmap);
                        imageView.setImageBitmap(resizedBmp);
                        zoomedOutBitmap = resizedBmp.copy(Bitmap.Config.ARGB_8888, true);

                        //zoomedOutBitmap = null;
                        selectedX = selectedY = -1;
                        save.setVisibility(View.GONE);
                        seekBar.setProgress(0);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                break;
        }
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == imageView) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = motionEvent.getX();
                    startY = motionEvent.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    float endX = motionEvent.getX();
                    float endY = motionEvent.getY();
                    if (isAClick(startX, endX, startY, endY)) {
                        save.setVisibility(View.VISIBLE);
                        Log.d(TAG, "onTouch: X" + motionEvent.getX());
                        Log.d(TAG, "onTouch: Y" + motionEvent.getY());
                        selectedX = (int) motionEvent.getX();
                        selectedY = (int) motionEvent.getY();
                        //setColorInPosition();
                    }


                    break;
            }
            return true;
        }
        return false;
    }

    private int CLICK_ACTION_THRESHHOLD = 5;

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        if (differenceX > CLICK_ACTION_THRESHHOLD/* =5 */ || differenceY > CLICK_ACTION_THRESHHOLD) {
            return false;
        }
        return true;
    }

    private void setColorInPosition() {
        /**/
        if (zoomedOutBitmap != null) {
            imgbmp = Bitmap.createBitmap(zoomedOutBitmap);

        } else {
            imageView.setDrawingCacheEnabled(true);
            imgbmp = Bitmap.createBitmap(imageView.getDrawingCache(true));
            imageView.setDrawingCacheEnabled(false);
        }

        int pxl = imgbmp.getPixel(selectedX, selectedY);

        targetColor = getColorFromPixel(pxl);

        int A = Color.alpha(pxl);
        int R = Color.red(pxl);
        int G = Color.green(pxl);
        int B = Color.blue(pxl);
        // take conversion up to one single value
        R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);
        // set new pixel color to output bitmap
        newColor = Color.argb(A, R, G, B);
        floodFill();

        //imgbmp.recycle();
    }

    private void floodFill() {
        lastTouchedBmp = Bitmap.createBitmap(imgbmp);
        lastTouchedBmp.setHasAlpha(true);
        /*GrayScaleTask grayScaleTask = new GrayScaleTask(targetColor, newColor, imgbmp, seekBar.getProgress(), selectedX, selectedY, this, this);
        grayScaleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/
/*QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(imgbmp,targetColor,newColor);
        queueLinearFloodFiller.setTolerance(seekBar.getProgress());
        queueLinearFloodFiller.floodFill(selectedX,selectedY);*/
        setTaskVariablesAndExecute();


    }

    private void floodFillTolerance() {
        imgbmp = Bitmap.createBitmap(lastTouchedBmp);
        imgbmp.setHasAlpha(true);
        setTaskVariablesAndExecute();
        /*GrayScaleTask grayScaleTask = new GrayScaleTask(targetColor, newColor, imgbmp, seekBar.getProgress(), selectedX, selectedY, this, this);
        grayScaleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/
       /* QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(imgbmp,targetColor,newColor);
        queueLinearFloodFiller.setTolerance(seekBar.getProgress());
        queueLinearFloodFiller.floodFill(selectedX,selectedY);*/
        // imageView.setImageBitmap(imgbmp);
    }

    private void setTaskVariablesAndExecute() {
        grayScaleTask = new GrayScaleTask(this, this);
        grayScaleTask.setImage(imgbmp);
        grayScaleTask.setTargetColor(targetColor);
        grayScaleTask.setNewColor(newColor);
        grayScaleTask.setTolerance(seekBar.getProgress());
        grayScaleTask.setSelectedX(selectedX);
        grayScaleTask.setSelectedY(selectedY);
        grayScaleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                    if (forCamera) {
                        openCamera();
                        forCamera = false;
                    } else {
                        saveBitmap(imgbmp);
                    }
                }
                break;
            case CAMERA_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermission("android.permission.WRITE_EXTERNAL_STORAGE")) {
                        openCamera();
                    } else {
                        forCamera = true;
                        requestStoragePermission();
                    }
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

    @Override
    public void onDone() {
        imageView.setImageBitmap(imgbmp);

        zoomedOutBitmap = Bitmap.createBitmap(imgbmp);

    }

    @Override
    public void onUp(int selectedX, int selectedY) {
        save.setVisibility(View.VISIBLE);

        this.selectedX = selectedX;
        this.selectedY = selectedY;
        Log.d(TAG, "onTouch: X" + this.selectedX);
        Log.d(TAG, "onTouch: Y" + this.selectedY);
        setColorInPosition();
    }

    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {
        int MAX_HEIGHT = 1024;
        int MAX_WIDTH = 1024;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(img, selectedImage);
        return img;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() > TARGET_WIDTH) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float aspectRatio = (float) width / height;
        int newWidth = TARGET_WIDTH;
        int newHeight = (int) (newWidth / aspectRatio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);

    }
}
