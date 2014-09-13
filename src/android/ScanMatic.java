package com.spendmatic.scanmatic;

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.util.Log;


public class ScanMatic extends CordovaPlugin {

	SMViewer smViewer;

	public static final String tag = "ScanMatic";
	public static final String version = "0.0.1";

	public static final int shutter = R.raw.shutter;
	
	public SoundPool soundPool;
	public HashMap<Integer, Integer> soundPoolMap; 

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
			
		Activity activity = cordova.getActivity();
		ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
		View webViewContainer = (View) webView.getParent(); // webView is nested in a LinearLayoutSoftKeyboardDetect
		smViewer = new SMViewer(activity);
		root.removeView(webViewContainer);
		root.addView(smViewer);
		root.addView(webViewContainer);

		webView.setBackgroundColor(0);
		WebSettings settings = webView.getSettings();
		settings.setBuiltInZoomControls(false);
		settings.setSupportZoom(true);
		
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		soundPoolMap = new HashMap<Integer, Integer>(1);
		
		soundPoolMap.put(shutter, soundPool.load(activity, shutter, 1));

		Log.d(tag, "Initialized");
	}

	@Override
    public void onPause(boolean multitasking) {
    	try{
    		smViewer.smCamera.stopPreview();
    		Log.d(tag, "Paused" + (smViewer != null ? " smViewer ok " : "smViewer null"));
    	}catch (Exception e)
    	{
    		Log.e(tag, "ERROR onPause");
    	}
    	
    }

    @Override
    public void onResume(boolean multitasking) {
    	try {
    		// smViewer.smCamera.startPreview();
    		Log.d(tag, "Resumed" + (smViewer != null ? " smViewer ok " : "smViewer null"));
    	}catch (Exception e)
    	{
    		Log.e(tag, "ERROR onResume");
    	}

    }

	public void playShutterSound() {
		try {
			soundPool.play(soundPoolMap.get(shutter), 1, 1, 1, 0, 1f);
		} catch (Exception e) {
			Log.e("ScanMatic", "failed to play the shutter sound");
		}
	}

	
	public boolean startCamera(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					smViewer.smCamera.startPreview();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
				
			}
		});
		return true;
	}
	
	public boolean stopCamera(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					smViewer.smCamera.stopPreview();
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}
	
	public boolean flash(final String state, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					smViewer.smCamera.setFlash(state);
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	public boolean capture(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				smViewer.requestCapture();
				playShutterSound();
			}
		});
		callbackContext.success();
		return true;
	}

	public boolean info(final CallbackContext callbackContext) {

		if ((smViewer != null) && (smViewer.smCamera != null)) {
		
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {

					JSONObject result = new JSONObject();
			
			    	try {

			    		result.put("version", version);
			    		result.put("camera", smViewer.smCamera.info());
			    		callbackContext.success(result);

			    	} catch (JSONException ex) {
			    		callbackContext.error(ex.getLocalizedMessage());	
			    	}
				}
			});	
			
		} else {
			callbackContext.success();
		}
		
		return true;
	}

	public boolean focus(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				smViewer.smCamera.focus();
				callbackContext.success();
			}
		});
		return true;
	}
	
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		
		if (action.equals("camera")) {
			if (args.getBoolean(0))
				return startCamera(callbackContext);
			else
				return stopCamera(callbackContext);
		} else if (action.equals("info")) {
			return info(callbackContext);
        } else if (action.equals("capture")) {
        	return capture(callbackContext);
        } else if (action.equals("focus")) {
        	return focus(callbackContext);
        } else if (action.equals("flash")) {
        	return flash(args.getString(0), callbackContext);
        } else if (action.equals("finish")) {
        	cordova.getActivity().finish();
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
        }
		return false;
    	
	}

}
