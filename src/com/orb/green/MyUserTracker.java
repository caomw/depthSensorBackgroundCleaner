package com.orb.green;

import com.asus.xtionstartkernel.NativeMethods;
import com.asus.xtionstartkernel.XtionContext;
import java.nio.ShortBuffer;
import org.openni.CalibrationProgressEventArgs;
import org.openni.Context;
import org.openni.GeneralException;
import org.openni.IObservable;
import org.openni.IObserver;
import org.openni.Point3D;
import org.openni.PoseDetectionCapability;
import org.openni.PoseDetectionEventArgs;
import org.openni.SceneMap;
import org.openni.SceneMetaData;
import org.openni.SkeletonCapability;
import org.openni.SkeletonJoint;
import org.openni.SkeletonJointPosition;
import org.openni.SkeletonProfile;
import org.openni.StatusException;
import org.openni.UserEventArgs;
import org.openni.UserGenerator;

public class MyUserTracker {
	private UserGenerator m_userGen;
	public SkeletonCapability m_skeletonCap;
	private PoseDetectionCapability m_poseDetectionCap;
	private String m_calibPose = null;
	SceneMetaData m_sceneMD = new SceneMetaData();

	public MyUserTracker(XtionContext context) throws GeneralException {
		this.m_userGen = UserGenerator.create(context.m_opennicontext);
		NativeMethods.initFromContext(context.m_opennicontext.toNative());
		this.m_skeletonCap = this.m_userGen.getSkeletonCapability();
		this.m_poseDetectionCap = this.m_userGen.getPoseDetectionCapability();

		this.m_skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);
		this.m_calibPose = this.m_skeletonCap.getSkeletonCalibrationPose();
	}

	public SceneMap getUsersPixelsData(int userID) {
		m_userGen.getUserPixels(userID, m_sceneMD);
		return m_sceneMD.getData();
	}
	
	public UserGenerator getGenerator() {
		return m_userGen;
	}

	public void addUserDetectObserver(IObserver<UserEventArgs> iobserver)
			throws StatusException {
		this.m_userGen.getNewUserEvent().addObserver(iobserver);
	}

	public void addPositionObserver(IObserver<PoseDetectionEventArgs> iobserver)
			throws StatusException {
		this.m_poseDetectionCap.getPoseDetectedEvent().addObserver(iobserver);
	}

	public void addCalibrationObserver(
			IObserver<CalibrationProgressEventArgs> iobserver)
			throws StatusException {
		this.m_skeletonCap.getCalibrationCompleteEvent().addObserver(iobserver);
	}

	public boolean needPoseForCalibration() {
		return this.m_skeletonCap.needPoseForCalibration();
	}

	public void startPoseDetection(String arg0, int arg1)
			throws StatusException {
		this.m_poseDetectionCap.startPoseDetection(arg0, arg1);
	}

	public void requestSkeletonCalibration(int arg0, boolean arg1)
			throws StatusException {
		this.m_skeletonCap.requestSkeletonCalibration(arg0, arg1);
	}

	public void stopPoseDetection(int arg0) throws StatusException {
		this.m_poseDetectionCap.stopPoseDetection(arg0);
	}

	public int[] GetUsers() throws StatusException {
		return this.m_userGen.getUsers();
	}

	public Point3D getCoM(int userID) throws StatusException {
		return this.m_userGen.getUserCoM(userID);
	}

	public ShortBuffer getUserLabel(int userID) {
		return this.m_userGen.getUserPixels(userID).getData()
				.createShortBuffer();
	}

	public String getSkeletonCalibrationPose() {
		return this.m_calibPose;
	}

	public void startTracking(int arg0) throws StatusException {
		this.m_skeletonCap.startTracking(arg0);
	}

	public boolean isSkeletonTracking(int iuser) {
		return this.m_skeletonCap.isSkeletonTracking(iuser);
	}

	public Point3D GetJointPos(int iuser, SkeletonJoint joint)
			throws StatusException {
		return this.m_skeletonCap.getSkeletonJointPosition(iuser, joint)
				.getPosition();
	}

	public void Close() {
		this.m_userGen.dispose();
		this.m_skeletonCap.dispose();
		this.m_poseDetectionCap.dispose();
	}
}

/*
 * Location:
 * C:\Users\vincentz\Documents\asus_xtion_rgb_output_test\libs\xtionsdk20131010
 * .jar Qualified Name: com.asus.xtionstartlibs.UserTracker Java Class Version:
 * 6 (50.0) JD-Core Version: 0.7.0.1
 */