package com.spendmatic.scanmatic;

import java.io.File;
import java.io.IOException;
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

import com.spendmatic.app.R;
import android.util.Log;

public class ScanMatic extends CordovaPlugin {

	SMViewer smViewer;

	public static final String tag = "ScanMatic";
	
	public static final int shutter = R.raw.shutter;
	public static final int coin = R.raw.coin;
	public static final int cashregister = R.raw.cashregister;
	public static final int alarm = R.raw.alarm;
	public static final int ding = R.raw.ding;
	
	public SoundPool soundPool;
	public HashMap<Integer, Integer> soundPoolMap; 

	private String intentToLaunchURI(Intent intent) {
		String intentString = intent.getDataString();
		String recRoute = intent.getStringExtra("route");
		String recQuery = intent.getStringExtra("query");
		String recHash = intent.getStringExtra("hash");
		if (intentString != null) {
			return intentString + "app/" + recRoute + recQuery + recHash;
		} else {
			return null; 
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (intent.getDataString() != null) {
			webView.loadUrl("javascript:handleOpenURL('" + intentToLaunchURI(intent) + "');");
			intent.setData(null);
		}
	}

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
		soundPoolMap.put(coin, soundPool.load(activity, coin, 1));
		soundPoolMap.put(cashregister, soundPool.load(activity, cashregister, 1));
		soundPoolMap.put(alarm, soundPool.load(activity, alarm, 1));
		soundPoolMap.put(ding, soundPool.load(activity, ding, 1));

		Log.d(tag, "Initialized");
	}

	
	public boolean getLaunchURI(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					Intent intent = cordova.getActivity().getIntent();
					callbackContext.success(intentToLaunchURI(intent));
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
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

	public void playSound(String soundName) {
		try {
			if (soundName.equals("shutter")) 
				{soundPool.play(soundPoolMap.get(shutter), 1, 1, 1, 0, 1f);}
			else if (soundName.equals("coin")) 
				{soundPool.play(soundPoolMap.get(coin), 1, 1, 1, 0, 1f);}
			else if (soundName.equals("cashregister")) 
				{soundPool.play(soundPoolMap.get(cashregister), 1, 1, 1, 0, 1f);}
			else if (soundName.equals("alarm")) 
				{soundPool.play(soundPoolMap.get(alarm), 1, 1, 1, 0, 1f);}
			else if (soundName.equals("ding")) 
				{soundPool.play(soundPoolMap.get(ding), 1, 1, 1, 0, 1f);}
			else 
				{Log.e ("PlaySound", "sound name not found");}
		} catch (Exception e) {
			Log.e ("PlaySound", "sound unable to play");
		}
	}

	public boolean sound(final String soundName, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					playSound(soundName);
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	public boolean deleteResource(final String resource, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					Context context = smViewer.getContext();
					File cache = context.getCacheDir();
					String path = cache.getAbsolutePath();
					File file = new File(path, resource);
					boolean deleted = file.delete();
					if(deleted)
					{
						callbackContext.success();
					}
					else
					{
						callbackContext.error("file not deleted");
					}
					
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	public boolean startCamera(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					synchronized(smViewer.smCamera) {
						smViewer.smCamera.startPreview();
					}
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
					synchronized(smViewer.smCamera) {
						smViewer.smCamera.stopPreview();
					}
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
					synchronized(smViewer.smCamera) {
						if (smViewer.smCamera.camera != null)
							smViewer.smCamera.setFlash(state);
					}
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	//setting capture dimensions and compression
	public boolean setImageSpecs(final String compression, final String pixelsOutput, final String pixelsCapture, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					smViewer.smCamera.jpegCompression = Integer.parseInt(compression);
					smViewer.smCamera.pixelsTarget = Integer.parseInt(pixelsOutput);
					smViewer.smCamera.pixelsCaptureTarget = Integer.parseInt(pixelsCapture);
					synchronized(smViewer.smCamera) {
						if (smViewer.smCamera.active) {
							smViewer.smCamera.startPreview();
						}
					}
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	public boolean getImageSpecs(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					
					JSONObject imageSpecs = new JSONObject();
					
					synchronized(smViewer.smCamera != null) {
						if (smViewer.smCamera) {
							imageSpecs.put("jpegCompression", smViewer.smCamera.jpegCompression);
							imageSpecs.put("pixelsTarget", smViewer.smCamera.pixelsTarget);
							imageSpecs.put("pixelsCaptureTarget", smViewer.smCamera.pixelsCaptureTarget);
						}
					}

					callbackContext.success(imageSpecs);

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
				try {
					synchronized(smViewer.smCamera) {
						smViewer.requestCapture();
					}
					playSound("shutter");
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}

	public boolean info(final CallbackContext callbackContext) {

		if ((smViewer != null) && (smViewer.smCamera != null)) {

			cordova.getThreadPool().execute(new Runnable() {
				public void run() {

					JSONObject result = new JSONObject();
					
					try {

						int version = 999;

						try {
							version = cordova.getActivity().getPackageManager().getPackageInfo(cordova.getActivity().getApplicationContext().getPackageName(), 0).versionCode;
						} catch (NameNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						JSONObject cacheInfo = new JSONObject();
						File cache = cordova.getActivity().getCacheDir();
						cacheInfo.put("freeSpace", cache.getUsableSpace());
						result.put("version", version);
						result.put("cache", cacheInfo);
						if(smViewer.smCamera.camera != null){
							synchronized(smViewer.smCamera) {
								result.put("camera", smViewer.smCamera.info());
							}
						}
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
				try {
					synchronized(smViewer.smCamera){
						smViewer.smCamera.focus();
					}
					callbackContext.success();
				} catch (Exception e) {
					callbackContext.error(e.getLocalizedMessage());
				}
			}
		});
		return true;
	}
	
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		
		try {
			if (action.equals("getLaunchURI")) {
				return getLaunchURI(callbackContext);
			}
			else if (action.equals("camera")) {
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
			} else if (action.equals("getImageSpecs")) {
				return getImageSpecs(callbackContext);
			} else if (action.equals("setImageSpecs")) {
				return setImageSpecs(args.getString(0), args.getString(1), args.getString(2), callbackContext);
			} else if (action.equals("sound")) {
				return sound(args.getString(0), callbackContext);
			} else if (action.equals("deleteResource")) {
				return deleteResource(args.getString(0), callbackContext);
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
		} catch (Exception e) {
			callbackContext.error(e.getLocalizedMessage());
		}

		return false;
	}
}
