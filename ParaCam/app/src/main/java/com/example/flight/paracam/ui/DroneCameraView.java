package com.example.flight.paracam.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import com.parrot.freeflight.video.VideoStageRenderer;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class DroneCameraView extends CameraBridgeViewBase{

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "DroneCameraView";

    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;

    protected DroneCameraFrame[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;

    private VideoStageRenderer renderer;

    private Bitmap test;

    public DroneCameraView(Context context, int cameraId, VideoStageRenderer renderer) {
        super(context, cameraId);
        this.renderer = renderer;
        test = renderer.getVideo();
    }

    public DroneCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean connectCamera(int width, int height) {
        return true;
    }

    @Override
    protected void disconnectCamera() {

    }

    public Bitmap getBit(){
        test = renderer.getVideo();
        return  test;
    }

//    @Override
//    protected boolean initializeCamera(int width, int height) {
//        Log.d(TAG, "Initialize java camera");
//        boolean result = true;
//
//            if (renderer == null)
//                return false;
//
//            /* Now set camera parameters */
//            try {
//                Camera.Parameters params = mCamera.getParameters();
//                Log.d(TAG, "getSupportedPreviewSizes()");
//                List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();
//
//                if (sizes != null) {
//                    /* Select the size that fits surface considering maximum size allowed */
//                    Size frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height);
//
//                    params.setPreviewFormat(ImageFormat.NV21);
//                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
//                    params.setPreviewSize((int)frameSize.width, (int)frameSize.height);
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
//                        params.setRecordingHint(true);
//
//                    List<String> FocusModes = params.getSupportedFocusModes();
//                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
//                    {
//                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//                    }
//
//                    mCamera.setParameters(params);
//                    params = mCamera.getParameters();
//
//                    mFrameWidth = renderer.getScreenWidth();
//                    mFrameHeight = renderer.getScreenHeight();
//
//                    if ((getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) && (getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT))
//                        mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
//                    else
//                        mScale = 0;
//
//                    if (mFpsMeter != null) {
//                        mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
//                    }
//
//                    int size = mFrameWidth * mFrameHeight;
//                    size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
//                    mBuffer = new byte[size];
//
//                    mFrameChain = new Mat[2];
//                    mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
//                    mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
//
//                    AllocateCache();
//
//                    mCameraFrame = new DroneCameraFrame[2];
//                    mCameraFrame[0] = new DroneCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
//                    mCameraFrame[1] = new DroneCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
//                    } else;
//
//                }
//                else
//                    result = false;
//            } catch (Exception e) {
//                result = false;
//                e.printStackTrace();
//            }
//        }
//
//        return result;
//    }

    class DroneCameraFrame implements CameraBridgeViewBase.CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            return mRgba;
        }

        public DroneCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };
}
