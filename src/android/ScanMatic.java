package com.spendmatic.scanmatic;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import android.app.Activity;
import android.media.SoundPool;
import android.media.AudioManager;

import com.spendmatic.app.R;


public class ScanMatic extends CordovaPlugin {

	SMViewer smViewer;

	public static final int shutter = R.raw.shutter;
	
	public SoundPool soundPool;
	public HashMap<Integer, Integer> soundPoolMap; 

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Activity activity = cordova.getActivity();
		smViewer = new SMViewer(activity);
		RelativeLayout newroot = new RelativeLayout(activity);
		newroot.addView(smViewer);
		View v = activity.findViewById(android.R.id.content);
		ViewGroup oldroot = (ViewGroup) v.getParent();
		oldroot.removeView(v);
		newroot.addView(v);
		activity.setContentView(newroot);

		webView.setBackgroundColor(0);
		WebSettings settings = webView.getSettings();
		settings.setBuiltInZoomControls(false);
		settings.setSupportZoom(true);
		settings.setUserAgentString(settings.getUserAgentString() + " com.spendmatic.app");

		//settings.setDomStorageEnabled(true);
		// settings.setAllowFileAccess(true);
		// settings.setAppCacheEnabled(true);
		// settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // bad

		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		soundPoolMap = new HashMap<Integer, Integer>(1);
		
		soundPoolMap.put(shutter, soundPool.load(activity, shutter, 1));
	}

	  /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
	@Override
    public void onPause(boolean multitasking) {
    	smViewer.smCamera.pausePreview();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
    	smViewer.smCamera.resumePreview();
    }

	public void playShutterSound() {
		soundPool.play(soundPoolMap.get(shutter), 1, 1, 1, 0, 1f);
	}

	public void resetSession(CallbackContext callbackContext) {
		if (smViewer != null) {
			smViewer.numTouch = 0;
			smViewer.smCamera.capturedFrames = 0;
			callbackContext.success();
		}
	}
	
	public void startCamera(CallbackContext callbackContext) {
		if (smViewer != null) {
			smViewer.smCamera.startPreview();
			callbackContext.success();
		}
	}
	
	public void stopCamera(CallbackContext callbackContext) {
		if(smViewer != null) {     
			smViewer.smCamera.stopPreview();
			callbackContext.success();
		}
	}
	
	public void setFlash(String state, CallbackContext callbackContext) {
		if (smViewer != null) {
			smViewer.smCamera.setFlash(state);
			callbackContext.success();
		}
	}

	public void capture(CallbackContext callbackContext) {
		if (smViewer != null) {
			smViewer.requestCapture();
			//SpendMatic sm = (SpendMatic) cordova.getActivity();
			//sm.playShutterSound();
			callbackContext.success();
		}
	}
	
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		
		if (action.equals("camera")) {
			if (args.getBoolean(0))
				startCamera(callbackContext);
			else
				stopCamera(callbackContext);
			return true;
		} else if (action.equals("cameraInfo")) {
			callbackContext.success(smViewer.smCamera.info());
			return true;
        } else if (action.equals("capture")) {
        	capture(callbackContext);
        	return true;
        } else if (action.equals("onCapture")) {
        	smViewer.captureCallback = callbackContext;
        	return true;
        } else if (action.equals("onPreview")) {
        	smViewer.previewCallback = callbackContext;
        	return true;
        } else if (action.equals("onAutoFocus")) {
        	smViewer.autoFocusCallback = callbackContext;
        	return true;
        } else if (action.equals("onAutoFocusMove")) {
        	smViewer.autoFocusMovedCallback = callbackContext;
        	return true;
        } else if (action.equals("focus")) {
        	smViewer.smCamera.focus();
        	callbackContext.success();
        	return true;
        } else if (action.equals("flash")) {
        	setFlash(args.getString(0), callbackContext);
        	return true;
        } else if (action.equals("reset")) {
        	resetSession(callbackContext);
        	return true;
        } else if (action.equals("transparent")) {
        	//SpendMatic sm = (SpendMatic) cordova.getActivity();
        	//((WebView) sm.getWebView()).setBackgroundColor(0x00000000);
        	callbackContext.success();
        	return true;
        }
		return false;
    	
	}

}
