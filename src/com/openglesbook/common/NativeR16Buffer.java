package com.openglesbook.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class NativeR16Buffer {
	public ShortBuffer nativeBuffer;
	public short[] javaArray;
	int width, height;

	public NativeR16Buffer(int w, int h) {
		width = w;
		height = h;
		int bytes = width * height * 2;
		nativeBuffer = ByteBuffer.allocateDirect(bytes)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		javaArray = new short[bytes];
	}

	public void apply() {
		nativeBuffer.put(javaArray).position(0);
	}
}
