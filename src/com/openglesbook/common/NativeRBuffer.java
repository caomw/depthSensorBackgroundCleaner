package com.openglesbook.common;

import java.nio.ByteBuffer;

public class NativeRBuffer {
	public ByteBuffer nativeBuffer;
	public byte[] javaArray;
	int width, height;

	public NativeRBuffer(int w, int h) {
		width = w;
		height = h;
		int bytes = width * height;
		nativeBuffer = ByteBuffer.allocateDirect(bytes);
		javaArray = new byte[bytes];
	}

	public void apply() {
		nativeBuffer.put(javaArray).position(0);
	}
}
