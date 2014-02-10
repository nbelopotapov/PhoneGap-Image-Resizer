/**
 * An Image Resizer Plugin for Cordova/PhoneGap.
 * 
 * More Information : https://github.com/raananw/
 * 
 * The android version of the file stores the images using the local storage.
 * 
 * The software is open source, MIT Licensed.
 * Copyright (C) 2012, webXells GmbH All Rights Reserved.
 * 
 * @author Raanan Weber, webXells GmbH, http://www.webxells.com
 */
package com.synconset;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.DisplayMetrics;

public class ImageResizePlugin extends CordovaPlugin {

	public static String IMAGE_DATA_TYPE_BASE64 = "base64Image";
	public static String IMAGE_DATA_TYPE_URL = "urlImage";
	public static String RESIZE_TYPE_FACTOR = "factorResize";
	public static String RESIZE_TYPE_MIN_PIXEL = "minPixelResize";
	public static String RESIZE_TYPE_MAX_PIXEL = "maxPixelResize";
	public static String RETURN_BASE64 = "returnBase64";
	public static String RETURN_URI = "returnUri";
	public static String FORMAT_JPG = "jpg";
	public static String FORMAT_PNG = "png";
	public static String DEFAULT_FORMAT = "jpg";
	public static String DEFAULT_IMAGE_DATA_TYPE = IMAGE_DATA_TYPE_BASE64;
	public static String DEFAULT_RESIZE_TYPE = RESIZE_TYPE_FACTOR;

	@Override
	public boolean execute(String action, JSONArray data,
			CallbackContext callbackContext) {
		JSONObject params;
		String imageData;
		String imageDataType;
		String format;
		Bitmap bmp;
		try {
			// parameters (first object of the json array)
			params = data.getJSONObject(0);
			// image data, either base64 or url
			imageData = params.getString("data");
			// which data type is that, defaults to base64
			imageDataType = params.has("imageDataType") ? params
					.getString("imageDataType") : DEFAULT_IMAGE_DATA_TYPE;
			// which format should be used, defaults to jpg
			format = params.has("format") ? params.getString("format")
					: DEFAULT_FORMAT;
			// create the Bitmap object, needed for all functions
			bmp = getBitmap(imageData, imageDataType);
			
			if (action.equals("resizeImage")) {
    			return resizeImage(params, format, bmp, callbackContext);
    		} else if (action.equals("imageSize")) {
    			return getImageSize(bmp, callbackContext);
    		} else if (action.equals("storeImage")) {
    			return storeImage(params, format, bmp, callbackContext);
    		} else {
    		    Log.d("PLUGIN", "unknown action");
    		    return false;
    		}
		} catch (JSONException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		} catch (IOException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		} catch (URISyntaxException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
			return false;
		}
	}
	
	private boolean getImageSize(Bitmap bmp, CallbackContext callbackContext) {
	    try {
			JSONObject res = new JSONObject();
			res.put("width", bmp.getWidth());
			res.put("height", bmp.getHeight());
			callbackContext.success(res);
			return true;
		} catch (JSONException e) {
			callbackContext.error(e.getMessage());
			return false;
		}
	}
	
	private boolean resizeImage(JSONObject params, String format, Bitmap bmp, 
	        CallbackContext callbackContext) throws JSONException, IOException, 
	        URISyntaxException {
		double widthFactor;
		double heightFactor;

		// Pixels or Factor resize
		String resizeType = params.getString("resizeType");

		// Get width and height parameters
		double width = params.getDouble("width");
		double height = params.getDouble("height");

		if (resizeType.equals(RESIZE_TYPE_MIN_PIXEL)) {
			widthFactor = width / ((double) bmp.getWidth());
			heightFactor = height / ((double) bmp.getHeight());
			if (widthFactor > heightFactor && widthFactor <= 1.0) {
	            heightFactor = widthFactor;
			} else if (heightFactor <= 1.0) {
			    widthFactor = heightFactor;
			} else {
			    widthFactor = 1.0;
			    heightFactor = 1.0;
			}
		} else if (resizeType.equals(RESIZE_TYPE_MAX_PIXEL)) {
		    widthFactor = width / ((double) bmp.getWidth());
			heightFactor = height / ((double) bmp.getHeight());
			if (widthFactor == 0.0) {
	            widthFactor = heightFactor;
            } else if (heightFactor == 0.0) {
            	heightFactor = widthFactor;
            } else if (widthFactor > heightFactor) {
                widthFactor = heightFactor; // scale to fit height
            } else {
                heightFactor = widthFactor; // scale to fit width
            }
		} else {
			widthFactor = width;
			heightFactor = height;
		}
		
		if (params.getBoolean("pixelDensity")) {
		    DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
		    if (metrics.density > 1) {
		        if (widthFactor * metrics.density < 1.0 && heightFactor * metrics.density < 1.0) {
		            widthFactor *= metrics.density;
		            heightFactor *= metrics.density;
		        } else {
		            widthFactor = 1.0;
		            heightFactor = 1.0;
		        }
		    }
		}

		Bitmap resized = getResizedBitmap(bmp, (float) widthFactor,
				(float) heightFactor);
				
		if (params.getBoolean("storeImage")) {
		    return storeImage(params, format, resized, callbackContext);
		} else {
		    int quality = params.getInt("quality");
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (format.equals(FORMAT_PNG)) {
				resized.compress(Bitmap.CompressFormat.PNG, quality, baos);
			} else {
				resized.compress(Bitmap.CompressFormat.JPEG, quality, baos);
			}
			byte[] b = baos.toByteArray();
			String returnString = Base64.encodeToString(b, Base64.DEFAULT);
			// return object
			JSONObject res = new JSONObject();
			res.put("imageData", returnString);
			res.put("width", resized.getWidth());
			res.put("height", resized.getHeight());
			callbackContext.success(res);
		}
		return true;
	}
	
	private boolean storeImage(JSONObject params, String format, Bitmap bmp, 
	        CallbackContext callbackContext) throws JSONException, IOException, 
	        URISyntaxException {
	    int quality = params.getInt("quality");
	    String filename = params.getString("filename");
	    URI folderUri = new URI(params.getString("directory"));
	    URI pictureUri = new URI(params.getString("directory") + "/" + filename);
	    File folder = new File(folderUri);
	    folder.mkdirs();
	    File file = new File(pictureUri);
	    OutputStream outStream = new FileOutputStream(file);
		if (format.equals(FORMAT_PNG)) {
			bmp.compress(Bitmap.CompressFormat.PNG, quality,
					outStream);
		} else {
			bmp.compress(Bitmap.CompressFormat.JPEG, quality,
					outStream);
		}
		outStream.flush();
		outStream.close();
		JSONObject res = new JSONObject();
		res.put("filename", filename);
		res.put("width", bmp.getWidth());
		res.put("height", bmp.getHeight());
		callbackContext.success(res);
		return true;
	}

	private Bitmap getResizedBitmap(Bitmap bm, float widthFactor,
			float heightFactor) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(widthFactor, heightFactor);
		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);
		return resizedBitmap;
	}

	private Bitmap getBitmap(String imageData, String imageDataType)
			throws IOException, URISyntaxException {
		Bitmap bmp;
		if (imageDataType.equals(IMAGE_DATA_TYPE_BASE64)) {
			byte[] blob = Base64.decode(imageData, Base64.DEFAULT);
			bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
		} else {
		    URI uri = new URI(imageData);
		    File imageFile = new File(uri);
		    bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		    if (bmp == null) {
		        throw new IOException("The image file could not be opened.");
		    }
		}
		return bmp;
	}
}