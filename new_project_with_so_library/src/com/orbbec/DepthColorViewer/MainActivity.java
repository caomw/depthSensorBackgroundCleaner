package com.orbbec.DepthColorViewer;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.orbbec.NativeNI.BackRemoval;
import com.orbbec.NativeNI.NativeMethod;
import com.orbbec.astrakernel.AstraContext;
import com.orbbec.astrakernel.DepthData;
import com.orbbec.astrakernel.PermissionCallbacks;
import com.orbbec.astrastartlibs.RGBData;
import com.orbbec.astrastartlibs.UserTracker;

import org.openni.*;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class MainActivity extends Activity {

	String VERSION = "0.0.3";

	String TAG = "obDepthColor";

	float pDepthHist[];

	AstraContext mContext;
	DepthData mDepthData;
	RGBData m_rgbdata;
	UserTracker mUserTracker;
	String mCalibPose = null;

	Bitmap mUserBitmap;
	int[] mColorPixels;
	short[] mDepthPixels;
	short[] mUserLabels;
	int[] mFinalPixels;
	private byte[] mBytePixels;

	DepthMap getDepthData;

	boolean mExit = false;
	int mMaxDepth = 10000;

	int ColorWidth = 640;
	int ColorHeight = 480;
	int DepthWidth = 320;
	int DepthHeight = 240;

	private PermissionCallbacks m_callbacks = new PermissionCallbacks() {

		@Override
		public void onDevicePermissionGranted() {
			try {

				m_rgbdata = new RGBData(mContext);
				m_rgbdata.setMapOutputMode(ColorWidth, ColorHeight, 30);

				mDepthData = new DepthData(mContext);
				mDepthData.setMapOutputMode(DepthWidth, DepthHeight, 30);

				mDepthData.setViewpoint(m_rgbdata.getGenerator());

				Log.i(TAG, "DefaultWidth  = " + ColorWidth);
				Log.i(TAG, "DefaultHeight  = " + ColorHeight);

				mUserTracker = new UserTracker(mContext);
				mCalibPose = mUserTracker.getSkeletonCalibrationPose();
				mUserTracker.addUserDetectObserver(new NewUserObserver());
				mUserTracker.addPositionObserver(new PoseDetectedObserver());
				mUserTracker
						.addCalibrationObserver(new CalibrationCompleteObserver());

				ColorWidth = m_rgbdata.GetImageMap().getXRes();
				ColorHeight = m_rgbdata.GetImageMap().getYRes();

				DepthWidth = mDepthData.GetDepthMap().getXRes();
				DepthHeight = mDepthData.GetDepthMap().getYRes();

				mUserBitmap = Bitmap.createBitmap(ColorWidth, ColorHeight,
						Bitmap.Config.ARGB_8888);
				mColorPixels = new int[ColorWidth * ColorHeight];
				mDepthPixels = new short[DepthWidth * DepthHeight];
				mUserLabels = new short[DepthWidth * DepthHeight];
				mFinalPixels = new int[ColorWidth * ColorHeight];

			} catch (Exception e) {
				// e.printStackTrace();
			}

			try {
				mContext.start();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				// e1.printStackTrace();
			}

			AstraView xv = new AstraView(MainActivity.this);
			FrameLayout cursorPanel = (FrameLayout) findViewById(R.id.DepthPanel);
			cursorPanel.addView(xv);

		}

		@Override
		public void onDevicePermissionDenied() {

		}
	};

	class NewUserObserver implements IObserver<UserEventArgs> {

		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {
			// mUserInfo = "New user " + args.getId();

			try {
				if (mUserTracker.needPoseForCalibration()) {
					mUserTracker.startPoseDetection(mCalibPose, args.getId());
				} else {
					mUserTracker.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class LostUserObserver implements IObserver<UserEventArgs> {
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {

		}
	}

	class CalibrationCompleteObserver implements
			IObserver<CalibrationProgressEventArgs> {
		public void update(
				IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args) {

			try {
				if (args.getStatus() == CalibrationProgressStatus.OK) {

					mUserTracker.startTracking(args.getUser());
				} else {
					if (mUserTracker.needPoseForCalibration()) {
						mUserTracker.startPoseDetection(mCalibPose,
								args.getUser());
					} else {
						mUserTracker.requestSkeletonCalibration(args.getUser(),
								true);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs> {
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args) {

			try {
				mUserTracker.stopPoseDetection(args.getUser());
				mUserTracker.requestSkeletonCalibration(args.getUser(), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.i(TAG, "version  = " + VERSION);

		try {
			mContext = new AstraContext(this, m_callbacks);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		pDepthHist = new float[mMaxDepth];

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mUserTracker != null && mDepthData != null && m_rgbdata != null
				&& mContext != null) {
			m_rgbdata.Close();
			mUserTracker.Close();
			mDepthData.Close();

			Log.i(TAG, "onStop:onDestroy begin!");
			mExit = true;
			mContext.Close();
			Log.i(TAG, "onStop:Activity Destory!");
		}
		System.exit(0);

	}

	protected void onDestroy() {
		super.onDestroy();
		mExit = true;
		mContext.Close();
	}

	public class AstraView extends SurfaceView implements
			SurfaceHolder.Callback, Runnable {

		private SurfaceHolder m_holder;
		private Thread m_thread = new Thread(this);
		private Canvas m_canvas;

		public AstraView(Context context) {
			super(context);
			m_holder = this.getHolder();
			m_holder.addCallback(this);
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
			m_thread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {

			mExit = true;
		}

		@Override
		public void run() {
			while (!mExit) {
				try {

					mContext.waitforupdate();

					/* Update Color */
					ImageMap imageMap = m_rgbdata.GetImageMap();

					ByteBuffer byteBufferTemp = imageMap.createByteBuffer();

					mBytePixels = byteBufferTemp.array();

					NativeMethod.GetRGBData(mBytePixels, mColorPixels,
							ColorWidth, ColorHeight);

					/* Update Depth */
					getDepthData = mDepthData.GetDepthMap();
					ShortBuffer shortBuffer = getDepthData.createShortBuffer();
					shortBuffer.get(mDepthPixels);

					// NativeMethod.CoventFromDepthTORGB(mDepthPixels, mWidth,
					// mHeight);

					int trackingId = -1;
					int[] data = mUserTracker.GetUsers();
					for (int i = 0; i < data.length; i++) {
						boolean bTrack = mUserTracker
								.isSkeletonTracking(data[i]);
						if (bTrack) {
							trackingId = data[i];
							ShortBuffer userLabel = mUserTracker
									.getUserLabel(0);
							userLabel.get(mUserLabels);
							break;
						}
					}

					if (trackingId != -1) {
						long startTime = System.currentTimeMillis();

						BackRemoval.GetBackRemovedColorData(mFinalPixels,
								mColorPixels, mDepthPixels, mUserLabels,
								ColorWidth, ColorHeight, DepthWidth,
								DepthHeight, 0, 10);
						long elapsed = System.currentTimeMillis() - startTime;
						Log.i("BackRemoval", "====Algorithm cost " + elapsed
								+ "ms");
						mUserBitmap.setPixels(mFinalPixels, 0, ColorWidth, 0,
								0, ColorWidth, ColorHeight);

					} else {
						mUserBitmap.setPixels(mColorPixels, 0, ColorWidth, 0,
								0, ColorWidth, ColorHeight);
					}

					m_canvas = m_holder.lockCanvas();

					Paint paint = new Paint();
					paint.setXfermode(new PorterDuffXfermode(
							PorterDuff.Mode.CLEAR));
					m_canvas.drawPaint(paint);

					if (mUserBitmap != null) {

						m_canvas.drawBitmap(mUserBitmap, new Rect(0, 0,
								ColorWidth, ColorHeight), new RectF(0f, 0f,
								getWidth(), getHeight()), null);
					}

					m_holder.unlockCanvasAndPost(m_canvas);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}

			}
		}
	}

}
