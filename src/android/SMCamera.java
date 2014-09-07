package com.spendmatic.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;

@SuppressLint("NewApi")
public class SMCamera implements Camera.PreviewCallback, Camera.PictureCallback {

	Camera camera;
	Thread_FramePrep framePrepThread;

	public int capturedFrames = 0;
    public Camera.PreviewCallback previewCallback;
    public boolean record = false;
    public boolean newoverlay = false;
    public Bitmap overlaysnap;
    
    String flash = null;
    boolean active = false;

    SMViewer smViewer;

    public SMCamera(SMViewer viewer) {
    	smViewer = viewer;
    	camera = Camera.open();	
	}
    
    public JSONObject info() {
    	
    	JSONObject result = new JSONObject();
    	Camera.Parameters params = camera.getParameters();
    	
    	try {
    		
    		result.put("focusModes", new JSONArray(params.getSupportedFocusModes()));
    		result.put("sceneModes", new JSONArray(params.getSupportedSceneModes()));
    		result.put("colorEffects", new JSONArray(params.getSupportedColorEffects()));
    		result.put("flashModes", new JSONArray(params.getSupportedFlashModes()));
    		result.put("antibanding", new JSONArray(params.getSupportedAntibanding()));
    		result.put("whiteBalance", new JSONArray(params.getSupportedWhiteBalance()));
    		result.put("pictureFormats", new JSONArray(params.getSupportedPictureFormats()));
    		result.put("previewFormats", new JSONArray(params.getSupportedPreviewFormats()));
    		
    	} catch (JSONException ex) {
    		
    		return null;
    		
    	}
    	
    	return result;
    	
    }
    
    public void focus() {
    	camera.autoFocus(new Camera.AutoFocusCallback() {
    		public void onAutoFocus(boolean success, Camera c) {
    			if (smViewer != null) {
    	    		if (smViewer.autoFocusCallback != null) {
    	    			PluginResult pr = new PluginResult(PluginResult.Status.OK, success);
    					pr.setKeepCallback(true);
    					smViewer.autoFocusCallback.sendPluginResult(pr);
    	    		}
    	    	}
    		}
    	});
    }

	public void setFlash(String state) {
		if (camera != null) {
			flash = state;
			Camera.Parameters cp = camera.getParameters();
			if (flash != null) {
				cp.setFlashMode(state);
			} else {
				cp.setFlashMode("off");
			}
			camera.setParameters(cp);
		} else {
			Log.e("toggleFlash", "camera is null");
		}
	}
	
	public void resumePreview() {
		if (active) {
			startPreview();
		}
	}
	
	public void pausePreview() {
		if (active) {
			stopPreview(active);
		}
	}
	
	public void stopPreview() {
		stopPreview(false);
	}
	
	public void stopPreview(boolean active) {
		if (camera != null) {
			//camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
			active = false || active;
		}
	}

	@SuppressLint("NewApi")
	public void startPreview() {
		if (camera == null) {
			camera = Camera.open();
			try {
				camera.setPreviewDisplay(smViewer.surfaceHolder);
			} catch (IOException x) {
				Log.e("Problem setting camera holder", x.toString());
			}
			
		}
		configureCamera();
		camera.startPreview();
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			camera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
				public void onAutoFocusMoving(boolean start, Camera camera) {
					if (smViewer != null) {
						if (smViewer.autoFocusMovedCallback != null) {
							PluginResult pr = new PluginResult(PluginResult.Status.OK, start);
	    					pr.setKeepCallback(true);
	    					smViewer.autoFocusMovedCallback.sendPluginResult(pr);
						}
					}
				}
			});
		}
		active = true;
	}

	public void configureCamera() {
		//Focus
		boolean autofocus 		= false;
		boolean macrofocus		= false;
		boolean edof			= false;
		boolean continuousfocus = true;
		//Scenes
		boolean avoidblurr		= false;
		boolean barcode			= false;
		boolean hdr				= false;
		//Effects
		boolean whiteboardeffect= false;
		boolean monoeffect		= false;
		
		//Camera Parameters Container
		Camera.Parameters params = camera.getParameters();

//		params.setPictureFormat(ImageFormat.JPEG);
//		params.set("jpeg-quality", 20);
//		params.setJpegQuality(20);

		List<String> focusModes = params.getSupportedFocusModes();

		if (autofocus && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) 
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		

		if (macrofocus && focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) 
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
		
		if (edof && focusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF)) 
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
		
		if (continuousfocus && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		
		List<String> sceneModes = params.getSupportedSceneModes();

		if (avoidblurr && sceneModes.contains(Camera.Parameters.SCENE_MODE_STEADYPHOTO)) 
			params.setSceneMode("steadyphoto");
		
		if (barcode && sceneModes.contains(Camera.Parameters.SCENE_MODE_BARCODE)) 
			params.setSceneMode("barcode");

		if (hdr && sceneModes.contains(Camera.Parameters.SCENE_MODE_HDR)) 
			params.setSceneMode("hdr");
			

		List<String> colorEffects = params.getSupportedColorEffects();

		if (whiteboardeffect && colorEffects.contains(Camera.Parameters.EFFECT_WHITEBOARD)) 
			params.setColorEffect("whiteboard");
			
		if (monoeffect && colorEffects.contains(Camera.Parameters.EFFECT_MONO)) 
			params.setColorEffect("mono");
			
		camera.setParameters(params);

		camera.setDisplayOrientation(90);

		int imageFormat = params.getPreviewFormat();
    	int bufferSize  = params.getPreviewSize().width * 
    		   			  params.getPreviewSize().height * 
      		 			  ImageFormat.getBitsPerPixel(imageFormat) / 8;

		byte [] cameraBuffer = new byte[bufferSize];
		camera.addCallbackBuffer(cameraBuffer);

		camera.setPreviewCallback(null);
		camera.setPreviewCallback(this);
		
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
    public void onPreviewFrame(byte[] data, Camera camera) {
    	
    	// if (newoverlay) {
    	// 	newoverlay = false;
    	// 	smViewer.overlayView.setImageDrawable(null);
    	// 	smViewer.overlayView.setImageBitmap(overlaysnap);
    	// 	smViewer.overlayView.setAlpha(100);
    	// 	smViewer.overlayView.setVisibility(View.VISIBLE);
    	// 	smViewer.overlayView.bringToFront();
    		
		//  }
        
        if (record) {
        	Log.e("Preview Frame", "New Frame Stored...");
        	record = false;
        	//capturedFrames++;
        	framePrepThread = null;
        	framePrepThread = new Thread_FramePrep(smViewer);
        	framePrepThread.execute(data);
        	this.capture();
        }

        camera.addCallbackBuffer(data);
    }

    public void capture() {
    	camera.takePicture(null, null, this);
    	Log.e("Preview Frame", "New Picture Captured...");
    }


	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		if (data != null) {
			if (smViewer != null) {
				if (smViewer.captureCallback != null) {
					
					Bitmap inImg = BitmapFactory.decodeByteArray(data, 0, data.length);
					
//					Matrix matrix = new Matrix();
//				    matrix.postRotate(90);
//				    WeakReference<Bitmap> rotateBitmap;
//				    rotateBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(inImg, 0, 0,inImg.getWidth(), inImg.getHeight(), matrix, true));
					
//					boolean sharp = this.sharpness(inImg);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					inImg.compress(Bitmap.CompressFormat.JPEG, 55, stream);
				    byte[] byteArray = stream.toByteArray();
					
					PluginResult pr = new PluginResult(PluginResult.Status.OK, byteArray);
					pr.setKeepCallback(true);
					smViewer.captureCallback.sendPluginResult(pr);
				}
			}
        	camera.startPreview();
        } else { Log.e("PictureCallback","picture data is null"); }
	}
	
	
	//image sharpness calculation
	public boolean sharpness (Bitmap in)
	{
		int THRESHOLD = 170;
		boolean result = false;
		
		double[][] laplacian = new double[][] {
			    {0, 1, 0},
			    {1,-4, 1},
			    {0, 1, 0}};
			  
		//convolution method
		int MAX_MEAS = computeConvolution(in, laplacian);
		
		if (MAX_MEAS >= THRESHOLD) {result = true;}
		return result;
	}
	
	public static int computeConvolution(Bitmap src, double[][] matrix) {
        int width = src.getWidth();
        int height = src.getHeight();
        int MAX = 0;
        int SIZE = 3;
        int sumR;
        int[][] pixels = new int[SIZE][SIZE];
  
        for(int y = 2; y < height - 2; ++y) {
            for(int x = 2; x < width - 2; ++x) {
  
                // get pixel matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        pixels[i][j] = src.getPixel(x + i, y + j);
                    }
                }
  
                // init color sum
                sumR = 0;
  
                // get sum of RGB on matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        sumR += (Color.red(pixels[i][j]) * matrix[i][j]);
                    }
                }
  
                // get final Red
                if (sumR > MAX) {MAX = sumR;}
  
            }
        }
  
        // final image
        return MAX;
    }
	
	
}
