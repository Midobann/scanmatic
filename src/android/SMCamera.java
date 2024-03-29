package com.spendmatic.scanmatic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;
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
    
    public int jpegCompression = 60;
    public int pixelsTarget = 2200000;
    public int pixelsCaptureTarget = 2200000;

    String flash = null;
    public boolean active = false;

    SMViewer smViewer;

    public SMCamera(SMViewer viewer) {
    	try
    	{
    		smViewer = viewer;
    		camera = Camera.open();	
    	}
    	catch (Exception e)
    	{
    		Log.e("SMCamera", "cannot open camera");
    	}
    	
	}
    
    public JSONObject info() {
    	
    	JSONObject result = new JSONObject();

    	try {
    		if (camera == null) {
	    		camera = Camera.open();
	    	}
    	} catch (Exception e) {
    		return null;
    	}

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
	
	public void stopPreview(boolean force) {
		try {
			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
				active = false || force;
			}

			Handler handler = new Handler(smViewer.getContext().getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					smViewer.setAlpha(0.0f);
				}
			});
			
		}
		catch (Exception e)
		{
			Log.e("SMCamera", e.getLocalizedMessage());
		}
	}

	@SuppressLint("NewApi")
	public void startPreview() {
		if (camera == null) {
			try {
				camera = Camera.open();
			} catch (Exception x) {
				Log.e("Problem getting camera", x.getLocalizedMessage());
				return;
			}
		}

		try {
			camera.setPreviewDisplay(smViewer.surfaceHolder);
		} catch (Exception e) {
			Log.e("Problem getting camera", e.getLocalizedMessage());
			return;
		}

		Handler handler = new Handler(smViewer.getContext().getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				smViewer.setAlpha(1.0f);
			}
		});
		
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
		boolean autofocus 		= true;
		boolean macrofocus		= false;
		boolean edof			= false;
		boolean continuousfocus = false;
		//Scenes
		boolean avoidblurr		= false;
		boolean barcode			= false;
		boolean hdr				= false;
		//Effects
		boolean whiteboardeffect= false;
		boolean monoeffect		= false;
		
		//Camera Parameters Container
		Camera.Parameters params = camera.getParameters();


		//Set picture size
		int desiredArea = pixelsCaptureTarget;
		List<Camera.Size> supportedSizes = params.getSupportedPictureSizes();
		
		Camera.Size best1 = supportedSizes.get(0);
		Camera.Size best2 = supportedSizes.get(0);
		
		for (int i=0; i<supportedSizes.size(); i++)
		{
			int currentArea = supportedSizes.get(i).width * supportedSizes.get(i).height;
			int currentAreaDiff = Math.abs(desiredArea - currentArea);
			int best1AreaDiff = Math.abs(desiredArea - (best1.width * best1.height));
			int best2AreaDiff = Math.abs(desiredArea - (best2.width * best2.height));

			if (currentAreaDiff < best2AreaDiff)
			{
				if (currentAreaDiff < best1AreaDiff)
				{
					best2 = best1;
					best1 = supportedSizes.get(i);
				}
				else
				{
					best2 = supportedSizes.get(i);
				}
			}
		}

		if (((best2.width * best2.height) > (2.5 * desiredArea)) || 
			((best2.width * best2.height) < (0.7 * desiredArea)))
		{
			params.setPictureSize(best1.width, best1.height);
		}
		else if (((best1.width * best1.height) > (2.5 * desiredArea)) || 
				 ((best1.width * best1.height) < (0.7 * desiredArea)))
		{
			params.setPictureSize(best2.width, best2.height);
		}
		else
		{
			Camera.Size previewSize = params.getPreviewSize();
			float previewRatio = previewSize.width / previewSize.height;
			float best1Ratio = best1.width / best1.height;
			float best2Ratio = best2.width / best2.height;
			float best1RatioDelta = Math.abs(previewRatio - best1Ratio);
			float best2RatioDelta = Math.abs(previewRatio - best2Ratio);

			if (best1RatioDelta <= best2RatioDelta)
			{
				params.setPictureSize(best1.width, best1.height);
			}
			else
			{
				params.setPictureSize(best2.width, best2.height);
			}
		}

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
        if (camera == null)
        {
        	return;
        }
       	try{ 
	        if (record) {
	        	Log.e("SMCamera", "Capture preview");
	        	record = false;
	        	framePrepThread = null;
	        	framePrepThread = new Thread_FramePrep(smViewer);
	        	framePrepThread.execute(data);
	        	this.capture();
	        }
        	camera.addCallbackBuffer(data);
        }catch (Exception e)
        {
        	Log.e("SMCamera", "onPreviewFrame Issue");
        }
        
    }

    public void capture() {
    	camera.takePicture(null, null, this);
    	Log.e("SMCamera", "Capture requested...");
    }


	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		if (data != null) {
			if (smViewer != null) {
				if (smViewer.captureCallback != null) {
					
					Bitmap inImg = BitmapFactory.decodeByteArray(data, 0, data.length);
				    
				    //scale if too big
				    int imgArea = inImg.getWidth() * inImg.getHeight();
					if ((1.5 * pixelsTarget) < imgArea)
					{
						double scaler = (double)imgArea / (double)pixelsTarget;
						scaler = Math.sqrt(scaler);
						int w = (int)((double)(inImg.getWidth()) / scaler);
						int h = (int)((double)(inImg.getHeight()) / scaler);
						inImg = Bitmap.createScaledBitmap(inImg, w, h, true);
					}
					
					//rotate
					Matrix matrix = new Matrix();
				    matrix.postRotate(90);
					inImg = Bitmap.createBitmap(inImg, 0, 0, inImg.getWidth(), inImg.getHeight(), matrix, true);
					
					//compress
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					inImg.compress(Bitmap.CompressFormat.JPEG, jpegCompression, stream);
					
					Context context = smViewer.getContext();
					File cache = context.getCacheDir();
					
					String path = cache.getAbsolutePath();
					File imageFile = null;
					PluginResult pr = null;
					FileOutputStream output = null;
					
					try {
						imageFile = File.createTempFile("original", ".jpg", cache);
					} catch (IOException e) {
						pr = new PluginResult(PluginResult.Status.ERROR, "failed to create a file for image");
						pr.setKeepCallback(true);
						smViewer.captureCallback.sendPluginResult(pr);
					}
					
					if (imageFile != null) {
						try {
							output = new FileOutputStream(imageFile);
							stream.writeTo(output);
							output.close();
							
							JSONObject fileRecord = new JSONObject();
							fileRecord.put("name", imageFile.getName());
							fileRecord.put("size", imageFile.length());
							fileRecord.put("type", "image/jpeg");
							fileRecord.put("lastModified", imageFile.lastModified());
							
							pr = new PluginResult(PluginResult.Status.OK, fileRecord);
							pr.setKeepCallback(true);
							smViewer.captureCallback.sendPluginResult(pr);
						} catch (Exception ex) {
							pr = new PluginResult(PluginResult.Status.ERROR, "failed to save image to file");
							pr.setKeepCallback(true);
							smViewer.captureCallback.sendPluginResult(pr);
						}
					}
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
