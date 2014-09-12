package com.spendmatic.scanmatic;

import java.io.IOException;

import org.apache.cordova.CallbackContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;


@SuppressLint("ViewConstructor")
public class SMViewer extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {

	public SurfaceHolder surfaceHolder;

	public int numTouch = 0;
	boolean record = false;
	long lastmillis;

	SMCamera smCamera;

	public ImageView overlayView;
	public CallbackContext captureCallback;
	public CallbackContext previewCallback;
	public CallbackContext autoFocusCallback;
	public CallbackContext autoFocusMovedCallback;


	@SuppressLint("NewApi")
	public SMViewer(Activity activity) {
		super(activity);
		//overlayView = new ImageView(activity);
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		lastmillis = System.currentTimeMillis();
		numTouch = 0;
	}

	public boolean requestCapture() {   
		long currentmillis = System.currentTimeMillis();
		if(currentmillis - lastmillis > 500)
		{
			lastmillis = currentmillis;
			
			//Frame and Picture Capture when this flag is true
			smCamera.record = true;
			//numTouch++;
		}
		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
    	try {
    		if(smCamera == null) {
    			smCamera = new SMCamera(this);
    		}
    		
			smCamera.camera.setPreviewDisplay(holder);
			
	    } catch (IOException e) {
	    	Log.e("SMViewer", "IOException in surfaceCreated");
	    }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		try{
			if (smCamera != null)
			{
				smCamera.stopPreview();
		    	smCamera = null;
			}
		}catch (Exception e)
		{
			Log.e("SMViewer", "IOException in surfaceDestroyed");
		}
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		try{
			if (smCamera != null)
			{
				smCamera.stopPreview();
		    	smCamera = null;
			}
		}catch (Exception e)
		{
			Log.e("SMViewer", "IOException in surfaceChanged");
		}
	}

}
