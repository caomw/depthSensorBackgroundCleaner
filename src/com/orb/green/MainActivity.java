package com.orb.green;

import org.openni.CalibrationProgressEventArgs;
import org.openni.CalibrationProgressStatus;
import org.openni.IObservable;
import org.openni.IObserver;
import org.openni.PoseDetectionEventArgs;
import org.openni.StatusException;
import org.openni.UserEventArgs;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.asus.xtionstartkernel.PermissionCallbacks;
import com.asus.xtionstartkernel.XtionContext;

public class MainActivity extends Activity {

	XtionContext mSensor;
	MyRGBData mRgbData;
	MyDepthData mDepthData;
	MyUserTracker mUserTracker;
	
	String mCalibPose;

	private GLSurfaceView mGLView;
	String TAG = "xtion";
	public String mInfo = "";

	private PermissionCallbacks mCallbacks = new PermissionCallbacks() {

		@Override
		public void onDevicePermissionGranted() {
			try {
				mRgbData = new MyRGBData(mSensor);
				mRgbData.setMapOutputMode(OrbConfig.rgbW, OrbConfig.rgbH, 30);

				mDepthData = new MyDepthData(mSensor);
				mDepthData.setMapOutputMode(OrbConfig.depthW, OrbConfig.depthH, 30);
				mDepthData.setViewpoint(mRgbData.getGenerator());

				mUserTracker = new MyUserTracker(mSensor);
				mCalibPose = mUserTracker.getSkeletonCalibrationPose();
				mUserTracker.addUserDetectObserver(new NewUserObserver());
				mUserTracker.addUserDetectObserver(new LostUserObserver());
				mUserTracker.addPositionObserver(new PoseDetectedObserver());
				mUserTracker
						.addCalibrationObserver(new CalibrationCompleteObserver());

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				mSensor.start();
			} catch (StatusException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void onDevicePermissionDenied() {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mGLView = new GLSurfaceView(this);
		mGLView.setEGLContextClientVersion(3);
		mGLView.setRenderer(new GreenRenderer(this, this));

		LinearLayout layout = new LinearLayout(this);
		// layout.addView(mInfoView);
		layout.addView(mGLView);

		setContentView(layout);

		try {
			mSensor = new XtionContext(this, mCallbacks);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		mRgbData.Close();
		mDepthData.Close();
		mUserTracker.Close();
		mSensor.Close();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	class NewUserObserver implements IObserver<UserEventArgs> {
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {
			mInfo = "New user " + args.getId();
			try {
				if (mUserTracker.needPoseForCalibration()) {
					mUserTracker.startPoseDetection(mCalibPose, args.getId());
				} else {
					mUserTracker.requestSkeletonCalibration(args.getId(), true);
				}
			} catch (StatusException e) {
				Log.e(TAG, "Exception!", e);
			}
		}
	}

	class LostUserObserver implements IObserver<UserEventArgs> {
		public void update(IObservable<UserEventArgs> observable,
				UserEventArgs args) {
			mInfo = "Lost user " + args.getId();
		}
	}

	class CalibrationCompleteObserver implements
			IObserver<CalibrationProgressEventArgs> {
		public void update(
				IObservable<CalibrationProgressEventArgs> observable,
				CalibrationProgressEventArgs args) {
			mInfo = "Calibraion complete: " + args.getStatus();
			try {
				if (args.getStatus() == CalibrationProgressStatus.OK) {
					mInfo = "starting tracking " + args.getUser();
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
			} catch (StatusException e) {
				Log.e(TAG, "Exception!", e);
			}
		}
	}

	class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs> {
		public void update(IObservable<PoseDetectionEventArgs> observable,
				PoseDetectionEventArgs args) {
			mInfo = "Pose " + args.getPose() + " detected for "
					+ args.getUser();
			try {
				mUserTracker.stopPoseDetection(args.getUser());
				mUserTracker.requestSkeletonCalibration(args.getUser(), true);
			} catch (StatusException e) {
				Log.e(TAG, "Exception!", e);
			}
		}
	}

}
