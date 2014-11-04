package com.fiskur.palette;

import com.fiskur.palette.PhotoHelper.UriToBitmapObserver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final int ACTION_GALLERY = 100;
	
	private static final int MODE_MUTED = 1;
	private static final int MODE_VIBRANT = 2;
	private int mMode = MODE_MUTED;
	
	private static final int LIGHT_MODE_BRIGHT = 1;
	private static final int LIGHT_MODE_DARK = 2;
	private int mLightMode = LIGHT_MODE_DARK;
	
	private Uri mUri;
	private String mImagePath;
	private ImageView mImageView;
	private Palette mPalette = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mImageView = ImageView.class.cast(findViewById(R.id.palette_image_view));
        
        launchChooser();
    }
    
    private void launchChooser(){
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, ACTION_GALLERY);
	}
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_GALLERY:
			if (resultCode == RESULT_OK && data != null) {
				
				mUri = data.getData();
				mImagePath = PhotoHelper.getRealPathFromURI(MainActivity.this, mUri);
				
				PhotoHelper.uriToBitmap(MainActivity.this, mUri, new UriToBitmapObserver() {
					
					@Override
					public void bitmapReady(Bitmap bitmap) {
						if(bitmap != null){
							l("Updating preview image...");
							mImageView.setImageBitmap(bitmap);
							mImageView.setVisibility(View.VISIBLE);
							Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
								public void onGenerated(Palette p) {
									mPalette = p;
									l("Palette generated");
									applyPalette();
								}
							});
						}
					}
				});
				
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
    
    private void applyPalette() {
    	TextView titleView = TextView.class.cast(findViewById(R.id.palette_image_title));
    	TextView backgroundColorLabel = TextView.class.cast(findViewById(R.id.palette_background_label));
    	if(mPalette == null){
    		titleView.setText("Could not extract palette");
    	}else{
    		LinearLayout mTextContainer = LinearLayout.class.cast(findViewById(R.id.palette_text_container));
    		
    		Swatch backgroundSwatch = null;
    		Swatch foregroundSwatch = null;
    		
    		switch(mMode){
	    		case MODE_MUTED:
	    			if(mLightMode == LIGHT_MODE_BRIGHT){
		    			backgroundSwatch = mPalette.getLightMutedSwatch();
		    			foregroundSwatch = mPalette.getDarkMutedSwatch();
	    			}else{
		    			backgroundSwatch = mPalette.getDarkMutedSwatch();
		    			foregroundSwatch = mPalette.getLightMutedSwatch();
	    			}

	    			break;	
	    		case MODE_VIBRANT:
	    			if(mLightMode == LIGHT_MODE_BRIGHT){
		    			backgroundSwatch = mPalette.getLightVibrantSwatch();
		    			foregroundSwatch = mPalette.getDarkVibrantSwatch();
	    			}else{
		    			backgroundSwatch = mPalette.getDarkVibrantSwatch();
		    			foregroundSwatch = mPalette.getLightVibrantSwatch();
	    			}

	    			break;
    		}
    		
    		if(backgroundSwatch != null){
    			int backgroundColor = backgroundSwatch.getRgb();
    			backgroundColorLabel.setTextColor(lighten(backgroundColor));
    			backgroundColorLabel.setText(getColorLabel(backgroundSwatch.getRgb()));
    			getActionBar().setBackgroundDrawable(new ColorDrawable(backgroundSwatch.getRgb()));
    			mTextContainer.setBackgroundColor(backgroundSwatch.getRgb()); 
    		}else{
    			backgroundColorLabel.setTextColor(getResources().getColor(R.color.white));
    			backgroundColorLabel.setText("null");
    			getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.default_background)));
    			mTextContainer.setBackgroundColor(getResources().getColor(R.color.default_background)); 
    		}
	        
	        if(foregroundSwatch != null){
	        	titleView.setText(getColorLabel(foregroundSwatch.getRgb()));
	        	titleView.setTextColor(foregroundSwatch.getRgb());
	        }else{
	        	titleView.setText(getColorLabel(getResources().getColor(R.color.white)));
	        	titleView.setTextColor(getResources().getColor(R.color.white));
	        }
    	}
    }
    
    private String getColorLabel(int color){
    	String label = Integer.toHexString(color);
    	if(label.startsWith("ff") && label.length() == 8){
    		label = label.substring(2, label.length());
    	}
    	label = "0x" + label;
    	return label;
    }
    
    private int lighten(int color){
    	float[] hsv = new float[3];
    	Color.colorToHSV(color, hsv);
    	hsv[2] *= 1.6f;
    	int lighter = Color.HSVToColor(hsv);
    	return lighter;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == R.id.choose_image){
    		launchChooser();
    	}else if(item.getItemId() == R.id.change_mode){
    		if(mMode == MODE_MUTED){
    			mMode = MODE_VIBRANT;
    			item.setTitle("Vibrant");
    		}else{
    			mMode = MODE_MUTED;
    			item.setTitle("Muted");
    		}
    		applyPalette();
    	}else if(item.getItemId() == R.id.change_light_mode){
    		if(mLightMode == LIGHT_MODE_BRIGHT){
    			mLightMode = LIGHT_MODE_DARK;
    			item.setTitle("Dark");
    		}else{
    			mLightMode = LIGHT_MODE_BRIGHT;
    			item.setTitle("Light");
    		}
    		applyPalette();
    	}
    	
    	return super.onOptionsItemSelected(item);
    }

    
    private void l(String message){
    	L.l(message);
    }
}
