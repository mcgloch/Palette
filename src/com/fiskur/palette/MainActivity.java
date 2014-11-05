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
import android.text.Html;
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
	private ImageView mImageView;
	private Palette mPalette = null;
	
	private int mBackgroundColor = -1;
	
	private MenuItem brightnessMenuItem;
	private MenuItem vibrantMenuItem;
	private MenuItem chooseMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getActionBar().setTitle("");
        getActionBar().setIcon(android.R.color.transparent);
        
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
    			mBackgroundColor = backgroundSwatch.getRgb();
    			updateMenuIcons();
    			backgroundColorLabel.setTextColor(lighten(mBackgroundColor));
    			backgroundColorLabel.setText(getColorLabel(true, backgroundSwatch.getRgb()));
    			getActionBar().setBackgroundDrawable(new ColorDrawable(backgroundSwatch.getRgb()));
    			mTextContainer.setBackgroundColor(backgroundSwatch.getRgb()); 
    		}else{
    			mBackgroundColor = getResources().getColor(R.color.default_background);
    			backgroundColorLabel.setTextColor(getResources().getColor(R.color.white));
    			backgroundColorLabel.setText("n/a");
    			getActionBar().setBackgroundDrawable(new ColorDrawable(mBackgroundColor));
    			updateMenuIcons();
    			mTextContainer.setBackgroundColor(getResources().getColor(R.color.default_background)); 
    		}
	        
	        if(foregroundSwatch != null){
	        	titleView.setText(getColorLabel(true, foregroundSwatch.getRgb()));
	        	titleView.setTextColor(foregroundSwatch.getRgb());
	        }else{
	        	titleView.setText(getColorLabel(true, getResources().getColor(R.color.white)));
	        	titleView.setText("n/a");
	        	titleView.setTextColor(getResources().getColor(R.color.white));
	        }
    	}
    }
    
    
    
    private String getColorLabel(boolean prefix, int color){
    	String label = Integer.toHexString(color);
    	if(label.startsWith("ff") && label.length() == 8){
    		label = label.substring(2, label.length());
    	}
    	if(prefix){
    		label = "0x" + label;
    	}
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
    	brightnessMenuItem = menu.findItem(R.id.change_light_mode);
    	vibrantMenuItem = menu.findItem(R.id.change_mode);
    	chooseMenuItem = menu.findItem(R.id.choose_image);
        updateMenuIcons();
        return true;
    }
    
    private void updateMenuIcons(){

    	int r = (mBackgroundColor >> 16) & 0xFF;
    	int g = (mBackgroundColor >> 8) & 0xFF;
    	int b = (mBackgroundColor >> 0) & 0xFF;
    	
    	int brightness = (int) (r*0.299 + g*0.587 + b*0.114);
    	
    	l("Brightness: " + brightness);
    	
    	if (brightness > 100){
    		l("Use dark icons");
    		brightnessMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_brightness));
    		vibrantMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_palette));
    		chooseMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_choose));
		} else{ 
			l("Use light icons");
			brightnessMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_brightness_white));
			vibrantMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_palette_white));
			chooseMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_action_choose_white));
    	}
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
