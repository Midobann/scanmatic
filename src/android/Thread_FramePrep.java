package com.spendmatic.scanmatic;

import java.io.ByteArrayOutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;

public class Thread_FramePrep extends AsyncTask<byte[], Void, Void> //<params, progress, result>
{
	SMViewer smViewer;
	CallbackContext callback;
	
	public Thread_FramePrep(SMViewer viewer) {
		smViewer = viewer;
	}

	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) 
	{
	    byte [] yuv = new byte[imageWidth*imageHeight*3/2];
	    // Rotate the Y luma
	    int i = 0;
	    for(int x = 0;x < imageWidth;x++)
	    {
	        for(int y = imageHeight-1;y >= 0;y--)                               
	        {
	            yuv[i] = data[y*imageWidth+x];
	            i++;
	        }
	    }
	    // Rotate the U and V color components 
	    i = imageWidth*imageHeight*3/2-1;
	    for(int x = imageWidth-1;x > 0;x=x-2)
	    {
	        for(int y = 0;y < imageHeight/2;y++)                                
	        {
	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
	            i--;
	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
	            i--;
	        }
	    }
	    return yuv;
	}
	
	protected Void doInBackground(byte[]... in) {

		// Convert to JPG
	    Size previewSize = smViewer.smCamera.camera.getParameters().getPreviewSize(); 
	    	
	    byte[] rotated = rotateYUV420Degree90(in[0], previewSize.width, previewSize.height);
	    
	    YuvImage yuvimage = new YuvImage(rotated, ImageFormat.NV21, previewSize.height, previewSize.width, null);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    yuvimage.compressToJpeg(new Rect(0, 0, previewSize.height, previewSize.width), 40, baos);
	    byte[] jdata = baos.toByteArray();
	    		
	    //Convert to Bitmap
//	    Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
//		
//		//rotate bitmap
//		Matrix matrix = new Matrix();
//		matrix.postRotate((float)(90));
//		bmp = Bitmap.createBitmap(bmp,0,0,bmp.getWidth(), bmp.getHeight(),matrix,true);
		
		//crop bitmap
		//Bitmap preview = Bitmap.createBitmap(bmp, 0,(int)(2*bmp.getHeight()/3),bmp.getWidth(), (int)(bmp.getHeight()/5));
		//smViewer.smCamera.overlaysnap = preview;
		//save bitmap
		//Log.e("Framer", "we're good");
		//smViewer.smCamera.newoverlay = true;

		if (smViewer != null) {
			if (smViewer.previewCallback != null) {
				//smViewer.previewCallback.success(preview);
				PluginResult pr = new PluginResult(PluginResult.Status.OK, jdata);
				pr.setKeepCallback(true);
				smViewer.previewCallback.sendPluginResult(pr);
			}
		}

		return null;
	}
	

	protected void onPostExecute() {

    }

	
}
