package com.orb.green;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE2;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.openni.ShortMap;
import org.openni.StatusException;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.openglesbook.common.ESShader;
import com.openglesbook.common.GLUT;
import com.openglesbook.common.NativeRBuffer;
import com.openglesbook.common.Texture;

class GreenRenderer implements GLSurfaceView.Renderer {

	private final MainActivity mActivity;
	boolean mReadyForRead = false;
	boolean mReadyForWrite = true;

	// /
	// Constructor
	//
	public GreenRenderer(MainActivity mainActivity, Context context) {
		mActivity = mainActivity;
		mContext = context;
		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(mVerticesData).position(0);
		mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		mIndices.put(mIndicesData).position(0);

		mDepthRBuffer = new NativeRBuffer(OrbConfig.depthW, OrbConfig.depthH);
		mLabelRunningArray = new float[OrbConfig.depthW * OrbConfig.depthH];
		mLabelSmoothArray = new float[OrbConfig.depthW * OrbConfig.depthH];

		mLabelArrayRaw = new short[OrbConfig.depthW * OrbConfig.depthH];
		mLabelArray = new float[OrbConfig.depthW * OrbConfig.depthH];

		mDepthArray = new short[OrbConfig.depthW * OrbConfig.depthH];
		mRgbArray = new int[OrbConfig.rgbW * OrbConfig.rgbH];

		new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (mActivity.mUserTracker == null || !mReadyForWrite) {
						Thread.yield();
						continue;
					}

					mReadyForWrite = false;

					try {
						mActivity.mSensor.waitforupdate();
					} catch (StatusException e1) {
						e1.printStackTrace();
					}
					try {
						mDepthMap = mActivity.mDepthData.GetDepthMap();
						mRgbBitmap = mActivity.mRgbData.GetImage();

						// mRgbBitmap.getPixels(mRgbArray, 0, OrbConfig.rgbW, 0,
						// 0, OrbConfig.rgbW, OrbConfig.rgbH);

						mLabelMap = null;
						MyUserTracker userTracker = mActivity.mUserTracker;
						for (int userId : userTracker.GetUsers()) {
							if (userTracker.isSkeletonTracking(userId)) {
								mLabelMap = userTracker
										.getUsersPixelsData(userId);
								break;
							}
						}
					} catch (StatusException e) {
						e.printStackTrace();
					}
					mReadyForRead = true;
				}
			}
		}).start();
	}

	// /
	// Initialize the shader and program object
	//
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Load shaders from 'assets' and get a linked program object
		mProgramObject = ESShader.loadProgramFromAsset(mContext,
				"shaders/vertexShader.vert", "shaders/fragmentShader.frag");

		// Get the sampler locations
		mBaseMapLoc = glGetUniformLocation(mProgramObject, "s_rgbMap");
		mDepthMapLoc = glGetUniformLocation(mProgramObject, "s_depthMap");
		mBackgroundMapLoc = glGetUniformLocation(mProgramObject,
				"s_backgroundMap");
		mIsTrackedLoc = glGetUniformLocation(mProgramObject, "u_isTracked");

		// Load the texture images from 'assets'
		mRgbTexture = new Texture();
		mBackgroundTexture = new Texture();
		mBackgroundTexture.update(GLUT.loadBitmapFromAsset(mContext,
				"textures/sky.jpg"));

		mDepthTexture = new Texture();

		glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
	}

	// /
	// Draw a triangle using the shader pair created in onSurfaceCreated()
	//
	String info = "";

	int holeOffsets[];

	void fillHoles(short[] depthArray, float input[], float out[], int w,
			int h, int kernelX, int kernelY) {
		if (holeOffsets == null) {
			holeOffsets = new int[(kernelX * 2 + 1) * (kernelY * 2 + 1)];
			int index = 0;
			for (int dy = -kernelY; dy < kernelY + 1; dy++) {
				for (int dx = -kernelX; dx < kernelX + 1; dx++) {
					holeOffsets[index++] = dy * w + dx;
				}
			}
		}

		for (int y = kernelY; y < h - kernelY; y++) {
			for (int x = kernelX; x < w - kernelX; x++) {
				int baseIndex = y * w + x;
				float v = input[baseIndex];
				if (v > 0) {
					out[baseIndex] = 1;
					for (int offset : holeOffsets) {
						int idx = baseIndex + offset;
						if (depthArray[idx] == 0) {
							out[idx] += v;
						}
					}
				}
			}
		}
	}

	public void onDrawFrame(GL10 glUnused) {

		if (!info.equals(mActivity.mInfo)) {
			Log.w("info", mActivity.mInfo);
			info = mActivity.mInfo;
		}
		if (mActivity.mUserTracker == null) {
			return;
		}

		if (mReadyForRead) {
			mReadyForRead = false;
			mRgbBitmapClone = Bitmap.createBitmap(mRgbBitmap);
			mRgbTexture.update(mRgbBitmapClone);

			// Update depth texture
			if (mDepthMap != null) {
				ShortBuffer shortBuffer = mDepthMap.createShortBuffer();
				shortBuffer.get(mDepthArray);
			}

			// http://people.clarkson.edu/~hudsonb/courses/cs611/
			// http://www.radfordparker.com/papers/kinectinpainting.pdf
			// https://github.com/golanlevin/DepthHoleFiller/blob/master/src/DepthHoleFiller.cpp
			if (mLabelMap != null) {
				ShortBuffer shortBuffer = mLabelMap.createShortBuffer();
				shortBuffer.get(mLabelArrayRaw);
			}

			mReadyForWrite = true;

			long startTime = System.currentTimeMillis();

			// inpaintRgbD(mDepthArray, mRgbArray, mLabelArrayRaw);

			final int neighborCount = (OrbConfig.kernelSize * 2 + 1)
					* (OrbConfig.kernelSize * 2 + 1);

			Arrays.fill(mLabelArray, 0);
			for (int i = 0; i < mLabelArrayRaw.length; i++) {
				if (mLabelArrayRaw[i] > 0) {
					mLabelArray[i] = 1.0f / neighborCount;
				}
			}

			Arrays.fill(mLabelSmoothArray, 0);
			// dialate(mDepthArray, mRgbArray, mLabelArrayRaw);
			fillHoles(mDepthArray, mLabelArray, mLabelSmoothArray,
					OrbConfig.depthW, OrbConfig.depthH, OrbConfig.kernelSize,
					OrbConfig.kernelSize);

			final float blending = 0.1f;
			for (int i = 0; i < mLabelRunningArray.length; i++) {
				mLabelRunningArray[i] = blending * mLabelRunningArray[i]
						+ (1.0f - blending) * mLabelSmoothArray[i];
			}

			for (int i = 0; i < mLabelRunningArray.length; i++) {
				mDepthRBuffer.javaArray[i] = (byte) (mLabelRunningArray[i] * 255);
			}

			mDepthRBuffer.apply();

			mDepthTexture.update(mDepthRBuffer);
			mDepthTexture.genMipmap();

			long elapsed = System.currentTimeMillis() - startTime;
			Log.i("Green", "====Algorithm cost " + elapsed + "ms");
		}

		// Set the view-port
		glViewport(0, 0, mWidth, mHeight);

		// Clear the color buffer
		glClear(GL_COLOR_BUFFER_BIT);

		// Use the program object
		glUseProgram(mProgramObject);

		// Load the vertex position
		mVertices.position(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, mVertices);
		// Load the texture coordinate
		mVertices.position(3);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, mVertices);

		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);

		// Bind the base map
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, mRgbTexture.id);

		// Set the base map sampler to texture unit to 0
		glUniform1i(mBaseMapLoc, 0);

		// Bind the light map
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, mDepthTexture.id);

		// Set the light map sampler to texture unit 1
		glUniform1i(mDepthMapLoc, 1);

		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, mBackgroundTexture.id);

		// Set the light map sampler to texture unit 1
		glUniform1i(mBackgroundMapLoc, 2);

		glUniform1i(mIsTrackedLoc, mLabelMap != null ? 1 : 0);

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, mIndices);
	}

	void inpaintHead(short[] depthArray, int[] rgbArray, short[] labelArray) {
		// left to right
		// down to up
		int w = OrbConfig.depthW;
		int h = OrbConfig.depthH;

		int threshHair = 100;
		int blackT = 20;
		int threshBody = 40;

		for (int x = 1; x < w - 1; x++) {
			for (int y = h - 1; y > 1; y--) {
				int index = y * w + x;
				// output.pixels[index] = 0xffffff;
				if (labelArray[index] > 0) {
					int clr = rgbArray[index];
					int r = (clr >> 16) & 0xff;
					int g = (clr >> 8) & 0xff;
					int b = clr & 0xff;

					// check y-1
					if (depthArray[index - w] == 0) {
						int clr2 = rgbArray[index - w];
						int r2 = (clr2 >> 16) & 0xff;
						int g2 = (clr2 >> 8) & 0xff;
						int b2 = clr2 & 0xff;

						if (r2 < blackT
								&& g2 < blackT
								&& b2 < blackT
								&& Math.abs(r - r2) + Math.abs(g - g2)
										+ Math.abs(b - b2) < threshHair) {
							labelArray[index - w] = 1;
						}
					}
				}
			}
		}
	}

	void inpaintRgbD(short[] depthArray, int[] rgbArray, short[] labelArray) {
		// left to right
		// down to up
		int w = OrbConfig.depthW;
		int h = OrbConfig.depthH;

		int threshHair = 100;
		int blackT = 20;
		int threshBody = 40;

		for (int x = 1; x < w - 1; x++) {
			for (int y = h - 1; y > 1; y--) {
				int index = y * w + x;
				// output.pixels[index] = 0xffffff;
				if (labelArray[index] > 0) {
					int clr = rgbArray[index];
					int r = (clr >> 16) & 0xff;
					int g = (clr >> 8) & 0xff;
					int b = clr & 0xff;

					// check y-1
					if (depthArray[index - w] == 0) {
						int clr2 = rgbArray[index - w];
						int r2 = (clr2 >> 16) & 0xff;
						int g2 = (clr2 >> 8) & 0xff;
						int b2 = clr2 & 0xff;

						if (r2 < blackT
								&& g2 < blackT
								&& b2 < blackT
								&& Math.abs(r - r2) + Math.abs(g - g2)
										+ Math.abs(b - b2) < threshHair) {
							labelArray[index - w] = 1;
						}
					}

					// check x+1
					if (depthArray[index + 1] == 0) {
						int clr2 = rgbArray[index + 1];
						int r2 = (clr2 >> 16) & 0xff;
						int g2 = (clr2 >> 8) & 0xff;
						int b2 = clr2 & 0xff;
						if (r2 < blackT
								&& g2 < blackT
								&& b2 < blackT
								&& Math.abs(r - r2) + Math.abs(g - g2)
										+ Math.abs(b - b2) < threshHair) {
							labelArray[index + 1] = 1;
						}
						if (Math.abs(r - r2) + Math.abs(g - g2)
								+ Math.abs(b - b2) < threshBody) {
							labelArray[index - w] = 1;
						}
					}
				}
			}
		}

		// check x-1
		for (int x = w - 1; x > 1; x--) {
			for (int y = h - 1; y > 1; y--) {
				int index = y * w + x;
				if (labelArray[index] > 0 && depthArray[index - 1] == 0) {
					int clr = rgbArray[index];
					int r = (clr >> 16) & 0xff;
					int g = (clr >> 8) & 0xff;
					int b = clr & 0xff;

					int clr2 = rgbArray[index - 1];
					int r2 = (clr2 >> 16) & 0xff;
					int g2 = (clr2 >> 8) & 0xff;
					int b2 = clr2 & 0xff;
					if (r2 < blackT
							&& g2 < blackT
							&& b2 < blackT
							&& Math.abs(r - r2) + Math.abs(g - g2)
									+ Math.abs(b - b2) < threshHair) {
						labelArray[index - 1] = 1;
					}
				}
			}
		}
	}

	// /
	// Handle surface changes
	//
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	// Handle to a program object
	private int mProgramObject;

	// Sampler location
	private int mBaseMapLoc;
	private int mDepthMapLoc;
	private int mBackgroundMapLoc;
	private int mIsTrackedLoc;

	// Additional member variables
	private int mWidth;
	private int mHeight;
	private FloatBuffer mVertices;
	private ShortBuffer mIndices;
	private Context mContext;

	//
	Texture mDepthTexture;
	Texture mBackgroundTexture;
	Texture mRgbTexture;

	ShortMap mDepthMap;
	private short[] mDepthArray;

	NativeRBuffer mDepthRBuffer;
	Bitmap mRgbBitmap;
	Bitmap mRgbBitmapClone;

	private int[] mRgbArray;

	private float[] mLabelRunningArray;
	private float[] mLabelSmoothArray;

	private float[] mLabelArray;
	private short[] mLabelArrayRaw;
	ShortMap mLabelMap;

	private final float[] mVerticesData = { -1f, 1f, 0.0f, // Position 0
			0.0f, 0.0f, // TexCoord 0
			-1f, -1f, 0.0f, // Position 1
			0.0f, 1.0f, // TexCoord 1
			1f, -1f, 0.0f, // Position 2
			1.0f, 1.0f, // TexCoord 2
			1f, 1f, 0.0f, // Position 3
			1.0f, 0.0f // TexCoord 3
	};

	private final short[] mIndicesData = { 0, 1, 2, 0, 2, 3 };
}