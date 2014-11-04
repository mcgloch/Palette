package com.fiskur.palette;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;

public class PhotoHelper {
	
	public static Location getExifLocation(String path) {
		Location location = new Location(path);
		
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			float[] latLonArr = new float[2];
			
			if(exifInterface.getLatLong(latLonArr)){
				location.setLatitude(latLonArr[0]);
				location.setLongitude(latLonArr[1]);
			}else{
				L.e("No exif lat/lon values");
			}
			return location;
		} catch (IOException e) {
			L.e(e.toString());
			return location;
		}
	}
	
	public static String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try { 
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
		    if (cursor != null) {
		      cursor.close();
		    }
		}
	}
	
	public static File getSaveFile(){
		File pathFile =  new File(Environment.getExternalStorageDirectory().toString() + File.separator + "Palette");
		if(!pathFile.isDirectory()){
			pathFile.mkdir();
		}
		return new File(pathFile, "/tempGeoSharePhoto.png");
	}
	
	public static String createSharePhotoNoThread(Context context, Uri contentUri, Drawable drawable){
		File tweetImageFile = getSaveFile();
		FileOutputStream fos = null;
		try {
			if (!tweetImageFile.exists()) {
				tweetImageFile.createNewFile();
			}
			fos = new FileOutputStream(tweetImageFile);
			Bitmap tempPhotoBmp = drawableToBitmap(drawable);
			tempPhotoBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
			L.l("Screenshot saved");
		} catch (Exception e) {
			tweetImageFile.delete();
			L.e("Exception on grabbing screen: " + e);
		}finally{
			try {
				if(fos != null){
					fos.flush();
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return tweetImageFile.getPath();
	}
	
	public static Bitmap drawableToBitmap (Drawable drawable) {
	    if (drawable instanceof BitmapDrawable) {
	        return ((BitmapDrawable)drawable).getBitmap();
	    }

	    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap); 
	    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
	    drawable.draw(canvas);

	    return bitmap;
	}
	
	public static void uriToBitmap(Context context, Uri imageUri, UriToBitmapObserver observer){
		new CreateBitmapTask(context, imageUri, observer).execute();
	}
	
	private static class CreateBitmapTask extends AsyncTask<Void, Void, Void>{
		private Context mContext;
		private Uri mUri;
		private UriToBitmapObserver mObserver = null;
		private int mSampleSize = 2;
		private Bitmap mBitmap = null;

		CreateBitmapTask(Context context, Uri imageUri, UriToBitmapObserver observer){
			mContext = context;
			mUri = imageUri;
			mObserver = observer;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = mSampleSize;
			try {
				Drawable drawable = Drawable.createFromResourceStream(null, null, mContext.getContentResolver().openInputStream(mUri), "", options);
				mBitmap = drawableToBitmap(drawable);
			} catch (FileNotFoundException e) {
				L.e("Error creating bitmap: " + e.toString());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(mObserver != null){
				mObserver.bitmapReady(mBitmap);
			}
		}
		
	}
	
	public static void createSharePhoto(Context context, Uri contentUri, int sampleSize, SharePhotoObserver sharePhotoObserver){
		new CreateSharePhotoTask(context, contentUri, sampleSize, sharePhotoObserver).execute();
	}
	
	private static class CreateSharePhotoTask extends AsyncTask<Void, Void, Void>{
		
		private SharePhotoObserver mSharePhotoObserver;
		private boolean previewImageSuccess = true;
		private String mError = null;
		private Context mContext;
		private Uri mUri;
		private int mSampleSize = 1;
		private Drawable mPreviewDrawable;
		
		CreateSharePhotoTask(Context context, Uri contentUri, int sampleSize, SharePhotoObserver sharePhotoObserver){
			mContext = context;
			mUri = contentUri;
			mSampleSize = sampleSize;
			
			if(mSampleSize == 1){
				mSampleSize = 2;
			}
			
			mSharePhotoObserver = sharePhotoObserver;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			File tweetImageFile = null;
			FileOutputStream fos = null;
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = mSampleSize;
				mPreviewDrawable = Drawable.createFromResourceStream(null, null, mContext.getContentResolver().openInputStream(mUri), "", options);
				tweetImageFile = getSaveFile();
				
				if (!tweetImageFile.exists()) {
					tweetImageFile.createNewFile();
				}
					
				fos = new FileOutputStream(tweetImageFile);
				Bitmap tempPhotoBmp = drawableToBitmap(mPreviewDrawable);
				tempPhotoBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
				L.l("Screenshot saved");
			} catch (Exception e) {
				if(tweetImageFile != null){
					tweetImageFile.delete();
				}
				L.e("Exception on grabbing screen: " + e);
			}finally{
				try {
					if(fos != null){
						fos.flush();
						fos.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(mSharePhotoObserver != null){
				if(previewImageSuccess){
					mSharePhotoObserver.sharePhotoReady(mPreviewDrawable);
				}else{
					mSharePhotoObserver.sharePhotoError(mError);
				}
			}
		}
	}
	
	public interface SharePhotoObserver{
		public void sharePhotoReady(Drawable preview);
		public void sharePhotoError(String error);
	}
	
	public interface UriToBitmapObserver{
		public void bitmapReady(Bitmap bitmap);
	}
}
