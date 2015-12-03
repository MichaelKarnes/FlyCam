/*
 * VideoStageRenderer
 *
 *  Created on: May 20, 2011
 *      Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.video;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.parrot.freeflight.ui.gl.GLBGVideoSprite;
import com.parrot.freeflight.ui.hud.Sprite;

import org.opencv.ml.Boost;

public class VideoStageRenderer implements Renderer {

	private GLBGVideoSprite bgSprite;

	private ArrayList<Sprite> sprites;
	private Map<Integer, Sprite> idSpriteMap;

	private float fps;

	private int screenWidth;
	private int screenHeight;

	//**********************
	private float[] mVMatrix = new float[16];
	private float[] mProjMatrix = new float[16];

	private int program;

	private Bitmap video;
	private Boolean captureBitmap;

	private int count;

	private Canvas can;
	private boolean canInitialized;

	public BitmapReadyListener bitmap_listener;

	private final String vertexShaderCode =
			"uniform mat4 uMVPMatrix;   \n" +
					"attribute vec4 vPosition; \n" +
					"attribute vec2 aTextureCoord;\n" +
					"varying vec2 vTextureCoord;\n" +
					"void main(){              \n" +
					"  gl_Position = uMVPMatrix * vPosition; \n" +
					"  vTextureCoord = aTextureCoord;\n" +
					"}                         \n";

	private final String fragmentShaderCode =
			"precision mediump float;  \n" +
					"varying vec2 vTextureCoord;\n" +
					"uniform sampler2D sTexture;\n" +
					"uniform float fAlpha ;\n" +
					"void main(){              \n" +
					" vec4 color = texture2D(sTexture, vTextureCoord); \n" +
					" gl_FragColor = vec4(color.xyz, color.w * fAlpha );\n" +
					" //gl_FragColor = vec4(0.6, 0.7, 0.2, 1.0); \n" +
					"}                         \n";

	private long startTime;

	private long endTime;

	public interface BitmapReadyListener{
		public void onBitmapReady(Bitmap bp);
	}

	//***********************

	public VideoStageRenderer(Context context, Bitmap initialTexture)
	{
		bgSprite = new GLBGVideoSprite(context.getResources());
		if(bgSprite==null){
			Log.d("VideoStageRenderer.java", "NULL0");
		}

		bgSprite.setAlpha(1.0f);

		if(bgSprite==null){
			Log.d("VideoStageRenderer.java", "NULL1");
		}

		idSpriteMap = new Hashtable<Integer, Sprite>();
		canInitialized = false;
		sprites = new ArrayList<Sprite>(4);
		captureBitmap = false;
		count = 0;
	}


	public void addSprite(Integer id, Sprite sprite)
	{
		if (!idSpriteMap.containsKey(id)) {
			idSpriteMap.put(id, sprite);
			synchronized (sprites) {
				sprites.add(sprite);
			}
		}
	}

	public Sprite getSprite(Integer id)
	{
		return idSpriteMap.get(id);
	}

	public GLBGVideoSprite getBgSprite(){
		return bgSprite;
	}

	public void removeSprite(Integer id)
	{
		if (idSpriteMap.containsKey(id)) {
			Sprite sprite = idSpriteMap.get(id);
			synchronized (sprites) {
				sprites.remove(sprite);
				idSpriteMap.remove(id);
			}
		}
	}

	public int getScreenWidth(){
		return screenWidth;
	}

	public int getScreenHeight(){
		return screenWidth;
	}

	public void onDrawFrame(Canvas canvas)
	{
		bgSprite.onDraw(canvas, 0, 0);

		synchronized (sprites) {
			int spritesSize = sprites.size();

			for (int i=0; i<spritesSize; ++i) {
				Sprite sprite = sprites.get(i);

				if (!sprite.isInitialized() && screenWidth != 0 && screenHeight != 0) {
					onSurfaceChanged(canvas, screenWidth, screenHeight);
					sprite.surfaceChanged(canvas);
				}

				if (sprite != null) {
					sprite.draw(canvas);
				}
			}
		}
	}


	public void onDrawFrame(GL10 gl)
	{
		// Limiting framerate in order to save some CPU time
		endTime = System.currentTimeMillis();
		long dt = endTime - startTime;

		if (dt < 33) try {
			Thread.sleep(33 - dt);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		startTime = System.currentTimeMillis();

		// Drawing scene
		bgSprite.onDraw(gl, 0, 0);

		synchronized (sprites) {
			int spritesSize = sprites.size();

			for (int i=0; i<spritesSize; ++i) {
				Sprite sprite = sprites.get(i);
				if (sprite != null) {
					if (!sprite.isInitialized() && screenWidth != 0 && screenHeight != 0) {
						sprite.init(gl, program);
						sprite.surfaceChanged(null, screenWidth, screenHeight);
						sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
					}

					sprite.draw(gl);
				}
			}
		}

		if(captureBitmap){
			double startTime = System.currentTimeMillis();
			video = createBitmapFromGLSurface(0, 0, screenWidth, screenHeight, gl);
			double totalTime = System.currentTimeMillis() - startTime;
			Log.d("VideoStageRenderer", "TIME TAKEN FOR BITMAP CREATION = " + totalTime);
			Log.d("VideoStageRenderer", "Bitmap " + count + " Created!!!!");
			count += 1;
			bitmap_listener.onBitmapReady(video);
		}

	}

	public void setGLSpriteListener(GLBGVideoSprite.GLSpriteUpdateListener listener) {
		bgSprite.listener = listener;
	}


	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		screenWidth = width;
		screenHeight = height;

		GLES20.glViewport(0, 0, width, height);
		Matrix.orthoM(mProjMatrix, 0, 0, width, 0, height, 0, 2f);

		bgSprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
		bgSprite.onSurfaceChanged(gl, width, height);

		synchronized (sprites) {
			int size = sprites.size();
			for (int i=0; i<size; ++i) {
				Sprite sprite = sprites.get(i);

				if (sprite != null) {
					sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
					sprite.surfaceChanged(null, width, height);
				}
			}
		}
	}


	public void onSurfaceChanged (Canvas canvas, int width, int height)
	{
		screenWidth = width;
		screenHeight = height;

		bgSprite.onSurfaceChanged(null, width, height);
	}


	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		startTime = System.currentTimeMillis();

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);

		GLES20.glLinkProgram(program);
		bgSprite.init(gl, program);

		// Init sprites
		synchronized (sprites) {
			for (int i=0; i<sprites.size(); ++i) {
				sprites.get(i).init(gl, program);
			}
		}

		Matrix.setLookAtM(mVMatrix, 0, /*x*/0, /*y*/0, /*z*/1.5f, 0f, 0f, -5f, 0, 1f, 0.0f);
	}


	public float getFPS()
	{
		return fps;
	}


	public boolean updateVideoFrame()
	{
		return bgSprite.updateVideoFrame();
	}


	private int loadShader(int type, String code)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0)
		{
			Log.e("opengl", "Could not compile shader");
			Log.e("opengl", GLES20.glGetShaderInfoLog(shader));
			Log.e("opengl", code);
		}

		return shader;
	}


	public void clearSprites()
	{
		synchronized (sprites) {
			for (int i=0; i<sprites.size(); ++i) {
				Sprite sprite = sprites.get(i);
				sprite.freeResources();
			}

			sprites.clear();
		}
	}

	private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl)
			throws OutOfMemoryError {
		int bitmapBuffer[] = new int[w * h];
		int bitmapSource[] = new int[w * h];
		IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
		intBuffer.position(0);

		try {
			gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
			int offset1, offset2;
			for (int i = 0; i < h; i++) {
				offset1 = i * w;
				offset2 = (h - i - 1) * w;
				for (int j = 0; j < w; j++) {
					int texturePixel = bitmapBuffer[offset1 + j];
					int blue = (texturePixel >> 16) & 0xff;
					int red = (texturePixel << 16) & 0x00ff0000;
					int pixel = (texturePixel & 0xff00ff00) | red | blue;
					bitmapSource[offset2 + j] = pixel;
				}
			}
		} catch (GLException e) {
			return null;
		}

		return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);


//		int width = w;
//		int height = h;
//		int screenshotSize = width * height;
//		ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
//		bb.order(ByteOrder.nativeOrder());
//		gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA,
//				GL10.GL_UNSIGNED_BYTE, bb);
//		int pixelsBuffer[] = new int[screenshotSize];
//		bb.asIntBuffer().get(pixelsBuffer);
//		bb = null;
//		Bitmap bitmap = Bitmap.createBitmap(width, height,
//				Bitmap.Config.RGB_565);
//		bitmap.setPixels(pixelsBuffer, screenshotSize - width, -width, 0,
//				0, width, height);
//		pixelsBuffer = null;
//
//		short sBuffer[] = new short[screenshotSize];
//		ShortBuffer sb = ShortBuffer.wrap(sBuffer);
//		bitmap.copyPixelsToBuffer(sb);
//
//		// Making created bitmap (from OpenGL points) compatible with
//		// Android bitmap
//		for (int i = 0; i < screenshotSize; i++) {
//			short v = sBuffer[i];
//			sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
//		}
//
//		sb.rewind();
//		bitmap.copyPixelsFromBuffer(sb);
//		Bitmap bp = bitmap.copy(Bitmap.Config.ARGB_8888,false);
//		return bp;

	}

	public Bitmap getVideo(){
		return video;
	}

	public int getNum(){
		return count;
	}

	public Bitmap getVideoObject(){
		return video;
	}

	public int getWidth(){
		return screenWidth;
	}

	public int getHeight(){
		return screenHeight;
	}

	public void setBitmapCapture(Boolean b){
		captureBitmap = b;
	}
}
