package com.spendmatic.app;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.view.View;
import android.webkit.WebView;


public class ScanMatic extends CordovaPlugin {

	SMViewer smViewer;
	
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		SpendMatic sm = (SpendMatic) cordova.getActivity();
		//smViewer = sm.smViewer;
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
			SpendMatic sm = (SpendMatic) cordova.getActivity();
			sm.playShutterSound();
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
        	SpendMatic sm = (SpendMatic) cordova.getActivity();
        	((WebView) sm.getWebView()).setBackgroundColor(0x00000000);
        	callbackContext.success();
        	return true;
        }
		return false;
    	
	}

}
