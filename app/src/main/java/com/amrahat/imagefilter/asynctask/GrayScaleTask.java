package com.amrahat.imagefilter.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.amrahat.imagefilter.GrayScaleFilter.QueueLinearFloodFiller;
import com.amrahat.imagefilter.interfaces.OnGreyScalingDone;

/**
 * Created by AMRahat on 5/21/2017.
 */

public class GrayScaleTask extends AsyncTask<Void,Void,Void> {

    private QueueLinearFloodFiller queueLinearFloodFiller;
    private Context context;
    private int targetColor,newColor;
    private Bitmap image;
    private int tolerance;
    private int selectedX;
    private int selectedY;
    private OnGreyScalingDone onGreyScalingDone;
    private ProgressDialog progressDialog;

    public GrayScaleTask(int targetColor, int newColor, Bitmap image, int tolerance, int selectedX, int selectedY, OnGreyScalingDone onGreyScalingDone, Context context) {
        this.targetColor = targetColor;
        this.newColor = newColor;
        this.image = image;
        this.tolerance = tolerance;
        this.selectedX = selectedX;
        this.selectedY = selectedY;
        this.onGreyScalingDone = onGreyScalingDone;
        this.progressDialog = new ProgressDialog(context);
    }

    public GrayScaleTask(Context applicationContext,OnGreyScalingDone onGreyScalingDone,QueueLinearFloodFiller queueLinearFloodFiller) {
        this.context = applicationContext;
        this.onGreyScalingDone = onGreyScalingDone;
        this.progressDialog = new ProgressDialog(applicationContext);
        this.queueLinearFloodFiller = queueLinearFloodFiller;
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.setMessage("Gray Scaling....");
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        //QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(image, targetColor, newColor);
        /*queueLinearFloodFiller.useImage(image);
        queueLinearFloodFiller.setTargetColor(targetColor);
        queueLinearFloodFiller.setFillColor(newColor);
        queueLinearFloodFiller.setTolerance(tolerance);*/
        queueLinearFloodFiller.floodFill(selectedX, selectedY);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
        onGreyScalingDone.onDone();
    }

    public int getTargetColor() {
        return targetColor;
    }

    public void setTargetColor(int targetColor) {
        this.targetColor = targetColor;
    }

    public int getNewColor() {
        return newColor;
    }

    public void setNewColor(int newColor) {
        this.newColor = newColor;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public int getTolerance() {
        return tolerance;
    }

    public void setTolerance(int tolerance) {
        this.tolerance = tolerance;
    }

    public int getSelectedX() {
        return selectedX;
    }

    public void setSelectedX(int selectedX) {
        this.selectedX = selectedX;
    }

    public int getSelectedY() {
        return selectedY;
    }

    public void setSelectedY(int selectedY) {
        this.selectedY = selectedY;
    }
}
