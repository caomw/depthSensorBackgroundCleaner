package com.openglesbook.common;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

public class GLUT {
	/**
	 * Utility method for debugging OpenGL calls. Provide the name of the call
	 * just after making it:
	 * 
	 * <pre>
	 * mColorHandle = GLES20.glGetUniformLocation(mProgram, &quot;vColor&quot;);
	 * MyGLRenderer.checkGlError(&quot;glGetUniformLocation&quot;);
	 * </pre>
	 * 
	 * If the operation is not successful, the check throws an error.
	 * 
	 * @param glOperation
	 *            - Name of the OpenGL call to check.
	 */
	public static void checkGlError(String glOperation) {
		int error;
		while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
			Log.e("GL", glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
	}

	// /
	// Load texture from asset
	//

	public static Bitmap loadBitmapFromAsset(Context context, String fileName) {
		Bitmap bitmap = null;
		InputStream is = null;

		try {
			is = context.getAssets().open(fileName);
		} catch (IOException ioe) {
			is = null;
		}

		if (is != null) {
			bitmap = BitmapFactory.decodeStream(is);
		}

		return bitmap;
	}

	public static void setupTextureFromBitmap(int texId, Bitmap bitmap) {
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
		GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
	}

}
