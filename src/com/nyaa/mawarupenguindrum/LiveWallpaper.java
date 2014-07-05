package com.nyaa.mawarupenguindrum;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.SurfaceHolder;

public class LiveWallpaper extends WallpaperService {
	
	private static final String TAG = LiveWallpaper.class.getName();
	
	@Override
	public Engine onCreateEngine() {
		return new LiveWallpaperEngine(this);
	}
	
	private class LiveWallpaperEngine extends Engine implements Runnable, OnSharedPreferenceChangeListener {
		
		private final static int THEME_RED = 0;
		private final static int THEME_GREY = 1;
		private final static int THEME_RANDOM = 2;
		
		private Context mContext;
		private Handler mHandler;
		
		private SurfaceHolder mSurfaceHolder;
		private int mWidth;
		private int mHeight;
		
		private int mIndex;
		
		private Paint mPaint;
		private int mBackgroundColour;
		private Bitmap mCircle;
		private Bitmap mNumber;
		
		private int mCols;
		private int mRows;
		
		private int mTheme;
		
		public LiveWallpaperEngine(Context context) {
			mContext = context;
		}
		
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			
			mHandler = new Handler();
			
			mPaint = new Paint();
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);			
			prefs.registerOnSharedPreferenceChangeListener(this);	
			setupPreferences(prefs);
		}
		
		private void reloadState() {
			if(mCircle != null) {
				mCircle.recycle();
				mCircle = null;
			}
			
			if(mNumber != null) {
				mNumber.recycle();
				mNumber = null;
			}
			
			if(Math.random() < 0.5f) {
				mCols = (int)Math.round(Math.random()*3)+1;
				if(mCols < 3) {
					mRows = (int)Math.round(Math.random()*2)+1;
				}
				else {
					mRows = (int)Math.round(Math.random()*3)+1;
				}
			}
			else {
				mRows = (int)Math.round(Math.random()*3)+1;
				if(mRows < 3) {
					mCols = (int)Math.round(Math.random()*2)+1;
				}
				else {
					mCols = (int)Math.round(Math.random()*3)+1;
				}
			}
			
			float padding = dipToPx(20);
			float circleSize;
			float circleSizeX = (mWidth - (mCols * padding)) / mCols;
			float circleSizeY = (mHeight - (mRows * padding)) / mRows;
			
			circleSize = (circleSizeX < circleSizeY) ? circleSizeX : circleSizeY;
			
			Resources res = mContext.getResources();
			
			Bitmap bmpCircle;
			Bitmap bmpNumber;
			
			//redo random theme
			int theme = mTheme;
			if(mTheme == THEME_RANDOM) {
				theme = (Math.random() < 0.5f) ? THEME_RED : THEME_GREY;
			}
			
			if(theme == THEME_RED) {
				bmpCircle = BitmapFactory.decodeResource(res, R.drawable.circle);
				bmpNumber = BitmapFactory.decodeResource(res, R.drawable.number);
				mBackgroundColour = Color.BLACK;
			}
			else {
				bmpCircle = BitmapFactory.decodeResource(res, R.drawable.circle_grey);
				bmpNumber = BitmapFactory.decodeResource(res, R.drawable.number_grey);
				mBackgroundColour = Color.WHITE;
			}
			
			if(bmpCircle.getWidth() != circleSize) {
				mCircle = Bitmap.createScaledBitmap(bmpCircle, (int)circleSize, (int)circleSize, true);
				mNumber = Bitmap.createScaledBitmap(bmpNumber, (int)circleSize, (int)circleSize, true);
				
				bmpCircle.recycle();
				bmpNumber.recycle();
				bmpCircle = null;
				bmpNumber = null;
			}
			else {
				mCircle = bmpCircle;
				mNumber = bmpNumber;
			}
		}		
		
        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
			mSurfaceHolder = surfaceHolder;
		}
		
        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
			mWidth = width;
			mHeight = height;
		}
		
        @Override
        public void onVisibilityChanged(boolean visible) {
			mHandler.removeCallbacks(this);
			
			if(visible) {
				reloadState();
				mHandler.post(this);
			}
		}
		
		private void draw(Canvas canvas) {
			canvas.drawColor(mBackgroundColour);
			
			float circleSize = mCircle.getWidth();
			
			Rect dst = new Rect();
			
			float pady = (mHeight - (circleSize * mRows)) / (mRows + 1);
			float padx = (mWidth - (circleSize * mCols)) / (mCols + 1);
			
			for(int i = 0; i < (mRows * mCols); i++) {
				Matrix circleMatrix = new Matrix();
				
				int x = i % mCols;
				int y = i / mCols;
				
				float posx = (circleSize * x) + (padx * (x + 1));
				float posy = (circleSize * y) + (pady * (y + 1));
				
				circleMatrix.postTranslate(posx, posy);
				circleMatrix.preRotate(mIndex, mCircle.getWidth() / 2, mCircle.getWidth() / 2);
				
				dst.top = (int)posy;
				dst.left = (int)posx;
				dst.right = dst.left+(int)circleSize;
				dst.bottom = dst.top+(int)circleSize;
				
				canvas.drawBitmap(mCircle, circleMatrix, mPaint);
				canvas.drawBitmap(mNumber, null, dst, mPaint);
			}
			
			mIndex -= 2;
		}

		@Override
		public void run() {
			Canvas canvas = mSurfaceHolder.lockCanvas();
			if(canvas != null) {
				try {
					draw(canvas);
				}
				finally {
					if(canvas != null) {
						mSurfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
			
			mHandler.postDelayed(this, 22);
		}
		
		private float dipToPx(int dip) {
		    DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			setupPreferences(sharedPreferences);
		}
		
		protected void setupPreferences(SharedPreferences sharedPreferences) {
			mTheme = Integer.parseInt(sharedPreferences.getString("theme", "0"));
		}
	}

}
