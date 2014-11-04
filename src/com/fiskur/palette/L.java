package com.fiskur.palette;

import android.util.Log;

public class L {
	
	private static final String TAG = "Palette";
	
	public static void l(String message){
		Log.d(TAG, message);
	}
	
	public static void l(String tag, String message){
		Log.d(tag, message);
	}
	
	public static void e(String message){
		Log.e(TAG, message);
	}
	
	public static void e(String tag, String message){
		Log.e(tag, message);
	}
}
