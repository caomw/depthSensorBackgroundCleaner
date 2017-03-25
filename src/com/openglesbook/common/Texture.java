package com.openglesbook.common;

import static android.opengl.GLES20.GL_INT;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexSubImage2D;
import static android.opengl.GLES30.glTexStorage2D;
import static android.opengl.GLES30.GL_R32I;
import static android.opengl.GLES30.GL_R8;
import static android.opengl.GLES30.GL_RGBA8;
import static android.opengl.GLES30.GL_RED;
import static android.opengl.GLES30.GL_RED_INTEGER;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;

public class Texture {
	public int id;
	boolean init = true;

	public Texture() {
		id = genTexture();
	}

	final static int kMipmapLevels = 5;

	public static int genTexture() {
		int[] textureId = new int[1];

		GLES30.glGenTextures(1, textureId, 0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId[0]);

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
				GLES30.GL_CLAMP_TO_EDGE);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
				GLES30.GL_CLAMP_TO_EDGE);

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_BASE_LEVEL, 0);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
				GLES30.GL_TEXTURE_MAX_LEVEL, kMipmapLevels);
		// GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
		// GLES30.GL_TEXTURE_MIN_LOD, 0);
		// GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
		// GLES30.GL_TEXTURE_MAX_LOD, mipmapLevels);

		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

		return textureId[0];
	}

	public void genMipmap() {
		glBindTexture(GL_TEXTURE_2D, id);
		glGenerateMipmap(GL_TEXTURE_2D);
	}

	public void update(Bitmap bitmap) {
		glBindTexture(GL_TEXTURE_2D, id);
		if (init) {
			glTexStorage2D(GL_TEXTURE_2D, kMipmapLevels, GL_RGBA8,
					bitmap.getWidth(), bitmap.getHeight());
			init = false;
		}
		GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap);
	}

	public void update(NativeIntBuffer intBuffer) {
		glBindTexture(GL_TEXTURE_2D, id);
		if (init) {
			glTexStorage2D(GL_TEXTURE_2D, kMipmapLevels, GL_R32I,
					intBuffer.width, intBuffer.height);
			init = false;
		}
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, intBuffer.width,
				intBuffer.height, GL_RED_INTEGER, GL_INT,
				intBuffer.nativeBuffer);
	}

	void update(NativeRGBABuffer rgbaBuffer) {
		glBindTexture(GL_TEXTURE_2D, id);
		if (init) {
			glTexStorage2D(GL_TEXTURE_2D, kMipmapLevels, GL_RGBA,
					rgbaBuffer.width, rgbaBuffer.height);
			init = false;
		}
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, rgbaBuffer.width,
				rgbaBuffer.height, GL_RGBA, GL_UNSIGNED_BYTE,
				rgbaBuffer.nativeBuffer);
	}

	public void update(NativeRBuffer rBuffer) {
		glBindTexture(GL_TEXTURE_2D, id);
		if (init) {
			glTexStorage2D(GL_TEXTURE_2D, kMipmapLevels, GL_R8, rBuffer.width,
					rBuffer.height);
			init = false;
		}
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, rBuffer.width, rBuffer.height,
				GL_RED, GL_UNSIGNED_BYTE, rBuffer.nativeBuffer);
	}

	// TODO:
	public void update(NativeR16Buffer rBuffer) {
		glBindTexture(GL_TEXTURE_2D, id);
		if (init) {
			glTexStorage2D(GL_TEXTURE_2D, kMipmapLevels, GL_R8, rBuffer.width,
					rBuffer.height);
			init = false;
		}

		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, rBuffer.width, rBuffer.height,
				GL_RED, GL_UNSIGNED_BYTE, rBuffer.nativeBuffer);
	}
}

class NativeIntBuffer {
	public IntBuffer nativeBuffer;
	public int[] javaArray;
	final static int BYTES_PER_INT = 4;
	int width, height;

	NativeIntBuffer(int w, int h) {
		width = w;
		height = h;
		int bytes = w * h * BYTES_PER_INT;
		nativeBuffer = ByteBuffer.allocateDirect(bytes)
				.order(ByteOrder.nativeOrder()).asIntBuffer();
		javaArray = new int[bytes];
	}

	void apply() {
		nativeBuffer.put(javaArray).position(0);
	}
}

// http://stackoverflow.com/questions/14290096/how-to-create-a-opengl-texture-from-byte-array-in-android
class NativeRGBABuffer {
	public ByteBuffer nativeBuffer;
	public byte[] javaArray;
	int width, height;

	NativeRGBABuffer(int w, int h) {
		width = w;
		height = h;
		int bytes = width * height * 4;
		nativeBuffer = ByteBuffer.allocateDirect(bytes);
		javaArray = new byte[bytes];
	}

	void apply() {
		nativeBuffer.put(javaArray).position(0);
	}
}
