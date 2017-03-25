package com.orbbec.NativeNI;

public class BackRemoval {

	static {
		System.loadLibrary("BackRemoval");
	}

	/**
	 * @param outRGBAFrame
	 *            输出的抠像结果
	 * @param colorArray
	 *            输入的颜色数组
	 * @param depthArray
	 *            输入的深度数组
	 * @param userLabelArray
	 *            输入的跟踪人物标签数组
	 * @param colorWidth
	 *            RGB 图像宽度
	 * @param colorHeight
	 *            RGB 图像高度
	 * @param depthWidth
	 *            深度图像宽度
	 * @param depthHeight
	 *            深度图像高度
	 * @param trackingID
	 *            跟踪人物的 ID 如果为 0 ，表示跟踪所有的人物
	 * @param blurEffect
	 *            人物边缘的羽化模糊程度，可选范围（2 - 254）
	 * @return
	 */
	public native static int GetBackRemovedColorData(int outRGBAFrame[],
			int colorArray[], short depthArray[], short userLabelArray[],
			int colorWidth, int colorHeight, int depthWidth, int depthHeight,
			int trackingID, int blurEffect);
}