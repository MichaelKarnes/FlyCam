package com.parrot.freeflight.ui.gl;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;

public class GLBGVideoSprite extends GLSprite
{
	private static final String TAG = GLBGVideoSprite.class.getSimpleName();

	private android.graphics.Matrix matrix;
	private Object videoFrameLock;
	public Bitmap video;
	private float videoSize[]  = {0,0, //image width, height
			0,0}; //
	public int screenWidth;
	public int screenHeight;
	private int videoWidth;
	private int videoHeight;
	private boolean isVideoReady = false;
	private boolean frameDrawn = false;

	private int prevImgWidth;
	private int prevImgHeight;

	private int x;
	private int y;

	public GLSpriteUpdateListener listener;

	public interface GLSpriteUpdateListener {
		void onFrameUpdated ();
	}

	public GLBGVideoSprite(Resources resources)
	{
		super(resources, null);
		videoFrameLock = new Object();
		video = Bitmap.createBitmap(1080, 960, Bitmap.Config.RGB_565);
	}

	public Bitmap getCurrentBitMap(){
		return super.getVideo();
	}

	@Override
	protected void onUpdateTexture()
	{
		if (onUpdateVideoTextureNative(program, textures[0]) && (prevImgWidth != imageWidth || prevImgHeight != imageHeight)) {
				if (!isVideoReady) {
				isVideoReady = true;
			}

			float coef = ((float)screenWidth / (float)imageWidth);

			setSize((int)(imageWidth * coef), (int)(imageHeight * coef));
			x = (screenWidth - width) / 2;
			y = (screenHeight - height) / 2;

			prevImgWidth = imageWidth;
			prevImgHeight = imageHeight;
		}

		if(listener != null) {
			listener.onFrameUpdated();
		}
	}


	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		this.screenWidth = width;
		this.screenHeight = height;

		onSurfaceChangedNative(width, height);

		super.onSurfaceChanged(gl, width, height);
	}


	@Override
	public void onDraw(GL10 gl, float x, float y)
	{   if (!isVideoReady) {
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		}

		super.onDraw(gl, this.x, this.y);
	}


	@Override
	public void onDraw(Canvas canvas, float x, float y) {
		if (!isVideoReady) {
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			super.onDraw(canvas, x, y);
		} else {
			canvas.drawBitmap(video, matrix, null);
		}
	}

	public boolean updateVideoFrame()
	{
		boolean success = false;

		synchronized (videoFrameLock) {

			if (getVideoFrameNative(video, videoSize)) {

				if (matrix == null) {
					matrix = new android.graphics.Matrix();
				}

				float newVideoWidth = videoSize[0];
				float newVideoHeight = videoSize[1];
//				float scaleX = videoSize[2];
//				float scaleY = videoSize[3];

				if (newVideoWidth != videoWidth || newVideoHeight != videoHeight) {
					videoWidth = (int)newVideoWidth;
					videoHeight = (int)newVideoHeight;

					if (video != null) {
						video.recycle();
					}

					video = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565);
					matrix.reset();

					if (videoWidth != 0 && videoHeight != 0) {
						matrix.setScale((float)screenHeight / (float)videoHeight, (float)screenHeight / (float)videoHeight);
					}
				}

				isVideoReady = true;
				success = true;
			}
		}

		return success;
	}


	private native boolean onUpdateVideoTextureNative(int program, int textureId);
	private native void onSurfaceCreatedNative();
	private native void onSurfaceChangedNative(int width, int height);
	private native boolean getVideoFrameNative(Bitmap bitmap, float[] ret);
}



//package com.parrot.freeflight.ui.gl;
//
//import javax.microedition.khronos.opengles.GL10;
//
//import android.content.res.Resources;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.opengl.GLES20;
//import android.util.Log;
//
//public class GLBGVideoSprite extends GLSprite
//{
//	private static final String TAG = GLBGVideoSprite.class.getSimpleName();
//
//	private android.graphics.Matrix matrix;
//	private Object videoFrameLock;
//	private Bitmap video;
//	private float videoSize[]  = {0,0, //image width, height
//			                    0,0}; //
//	public int screenWidth;
//	public int screenHeight;
//	private int videoWidth;
//	private int videoHeight;
//	private boolean isVideoReady = false;
//	private boolean frameDrawn = false;
//
//	private int prevImgWidth;
//	private int prevImgHeight;
//
//	private int x;
//	private int y;
//
//
//	public GLBGVideoSprite(Resources resources)
//	{
//		super(resources, null);
//		videoFrameLock = new Object();
//		video = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
//	}
//
//
//	@Override
//	protected void onUpdateTexture()
//	{
//		if (onUpdateVideoTextureNative(program, textures[0]) && (prevImgWidth != imageWidth || prevImgHeight != imageHeight)) {
//		    if (!isVideoReady) {
//		        isVideoReady = true;
//		    }
//
//			float coef = ((float)screenWidth / (float)imageWidth);
//
//			setSize((int)(imageWidth * coef), (int)(imageHeight * coef));
//			x = (screenWidth - width) / 2;
//			y = (screenHeight - height) / 2;
//
//			prevImgWidth = imageWidth;
//			prevImgHeight = imageHeight;
//		}
//	}
//
//
//	@Override
//	public void onSurfaceChanged(GL10 gl, int width, int height)
//	{
//		this.screenWidth = width;
//		this.screenHeight = height;
//
//		onSurfaceChangedNative(width, height);
//
//		super.onSurfaceChanged(gl, width, height);
//	}
//
//
//	@Override
//	public void onDraw(GL10 gl, float x, float y)
//	{   if (!isVideoReady) {
//	        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//	    }
//
//		super.onDraw(gl, this.x, this.y);
//	}
//
//
//	@Override
//	public void onDraw(Canvas canvas, float x, float y) {
//		if (!isVideoReady) {
//		    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//			super.onDraw(canvas, x, y);
//		} else {
//			canvas.drawBitmap(video, matrix, null);
//		}
//	}
//
//	public boolean updateVideoFrame()
//	{
//		Log.d("GLBGVideoSprite", "updateViewFrame()");
//		boolean success = false;
//
//		synchronized (videoFrameLock) {
//
//			boolean didGetVideoFrameNative = getVideoFrameNative(video, videoSize);
//
//			Log.d("GLBGSprite", "didGetVideoFrameNative = " + didGetVideoFrameNative);
//
//			if (true) {
//
//				if (matrix == null) {
//					matrix = new android.graphics.Matrix();
//				}
//
//				float newVideoWidth = videoSize[0];
//				float newVideoHeight = videoSize[1];
////				float scaleX = videoSize[2];
////				float scaleY = videoSize[3];
//
//				Log.d("GLBGSprite", "newWidth "+ newVideoWidth + " newHeight " + newVideoHeight);
//
//				if (newVideoWidth != videoWidth || newVideoHeight != videoHeight) {
//					videoWidth = (int)newVideoWidth;
//					videoHeight = (int)newVideoHeight;
//
//					Log.d("GLBGSprite", "1");
//
//					if (video != null) {
//						video.recycle();
//						Log.d("GLBGSprite", "2");
//					}
//
//					video = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565);
//					matrix.reset();
//
//					if (videoWidth != 0 && videoHeight != 0) {
//						matrix.setScale((float)screenHeight / (float)videoHeight, (float)screenHeight / (float)videoHeight);
//						Log.d("GLBGSprite", "3");
//					}
//				}
//
//				isVideoReady = true;
//				success = true;
//			}
//		}
//
//		return success;
//	}
//
//
//	private native boolean onUpdateVideoTextureNative(int program, int textureId);
//	private native void onSurfaceCreatedNative();
//	private native void onSurfaceChangedNative(int width, int height);
//	private native boolean getVideoFrameNative(Bitmap bitmap, float[] ret);
//}
