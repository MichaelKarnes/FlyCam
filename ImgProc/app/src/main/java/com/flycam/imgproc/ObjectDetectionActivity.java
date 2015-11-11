package com.flycam.imgproc;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

public class ObjectDetectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;


    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;

    private CameraBridgeViewBase   mOpenCvCameraView;


    private HOGDescriptor           descriptor;

    private boolean                 nativeLibraryLoaded = false;

    private Mat                     mDetectionFrame;
    private long                    prevTime;
    private double                  detectionFps = 30;
    private double                  detectionSizeRatio = 1;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("opencv_java3");
                    onNativeLibraryLoaded();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ObjectDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        prevTime = System.nanoTime();
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_object_detection);

        mViewMode = VIEW_MODE_GRAY;

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.setCvCameraViewListener(this);

        setImmersiveScreen();
        //setImmersive(true);
    }

    private void onNativeLibraryLoaded() {
        /*Size _winSize = new Size(64, 128);
        Size _blockSize = new Size(16, 16);
        Size _blockStride = new Size(8, 8);
        Size _cellSize = new Size(8, 8);
        int _nbins = 9;
        descriptor = new HOGDescriptor(_winSize, _blockSize, _blockStride, _cellSize, _nbins);*/
        descriptor = new HOGDescriptor();
        descriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());

        nativeLibraryLoaded = true;
        mOpenCvCameraView.enableView();
    }

    private void setImmersiveScreen() {
        View v = new View(this);
        mOpenCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mDetectionFrame = new Mat((int)(height * detectionSizeRatio), (int) (width * detectionSizeRatio), CvType.CV_8UC3);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        long fpsPeriod = (long) (1000000000 / detectionFps);
        if(fpsPeriod > System.nanoTime() - prevTime)
            return mRgba;
        prevTime = System.nanoTime();

        final int viewMode = mViewMode;

        switch (viewMode) {
            case VIEW_MODE_GRAY:
                // input frame has gray scale format
                mRgba = inputFrame.gray();
                break;
            case VIEW_MODE_RGBA:
                // input frame has RBGA format
                mRgba = inputFrame.rgba();
                break;
            case VIEW_MODE_CANNY:
                // input frame has gray scale format
                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_FEATURES:
                // input frame has RGBA format
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();
                FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
                break;
        }

        Rect[] locationsArr = new Rect[0];

        // Detection
        if (descriptor != null) {
            //Imgproc.cvtColor(inputFrame.rgba(), mDetectionFrame, Imgproc.COLOR_RGB2GRAY, 4);
            mDetectionFrame = inputFrame.gray();
            Imgproc.resize(mDetectionFrame, mDetectionFrame, new Size(0, 0), detectionSizeRatio, detectionSizeRatio, Imgproc.INTER_LINEAR);
            //Log.d(TAG, "origFrame:" + inputFrame.gray().size().toString() + " resizedFrame:" + mDetectionFrame.size().toString());
            MatOfRect locations = new MatOfRect();
            MatOfDouble weights = new MatOfDouble();
            double hitThreshold = 0;
            Size winStride = new Size(64, 64);
            Size padding = new Size(0, 0);
            double scale = 1.05;
            double finalThreshold = 2;
            boolean useMeanshiftGrouping = true;
            descriptor.detectMultiScale(mDetectionFrame, locations, weights, hitThreshold, winStride, padding, scale, finalThreshold, useMeanshiftGrouping);
            //descriptor.detectMultiScale(mDetectionFrame, locations, weights);
            locationsArr = locations.toArray();
        }

        //Log.d(TAG, Thread.currentThread().toString() + "locations: " + (int) (locations.size().width * locations.size().height));
        //Imgproc.cvtColor(mDetectionFrame, mDetectionFrame, Imgproc.COLOR_GRAY2RGB, 4);
        //Imgproc.resize(mRgba, mRgba, new Size(mRgba.width(), mRgba.height()), 0, 0, Imgproc.INTER_LINEAR);
        for (int j = 0; j < locationsArr.length; j++) {
            Rect rect = locationsArr[j];
            Imgproc.rectangle(mRgba, new Point(rect.x/detectionSizeRatio, rect.y/detectionSizeRatio), new Point((rect.x + rect.width)/detectionSizeRatio, (rect.y + rect.height)/detectionSizeRatio), new Scalar(0, 255, 0), 2);
            //Log.i("Locations", "Height " + rect.height + ", Width " + rect.width);
        }

        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        }

        return true;
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba);
}
