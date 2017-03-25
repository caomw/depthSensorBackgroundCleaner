package com.orb.green;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import com.asus.xtionstartkernel.NativeMethods;
import com.asus.xtionstartkernel.XtionContext;
import org.openni.Context;
import org.openni.GeneralException;
import org.openni.ImageGenerator;
import org.openni.ImageMap;
import org.openni.ImageMetaData;
import org.openni.MapOutputMode;
import org.openni.StatusException;

public class MyRGBData {
	private static ImageGenerator m_imgGen;
	private Bitmap m_bitmap;
	public int[] m_pixels;
	final int WIDTHSIZE = OrbConfig.rgbW;
	final int HEIGHTSIZE = OrbConfig.rgbH;

	public MyRGBData(XtionContext context) throws GeneralException {
		m_imgGen = ImageGenerator.create(context.m_opennicontext);
		this.m_bitmap = Bitmap.createBitmap(WIDTHSIZE, HEIGHTSIZE,
				Bitmap.Config.ARGB_8888);
		NativeMethods.initFromContext(context.m_opennicontext.toNative());
		this.m_pixels = new int[WIDTHSIZE * HEIGHTSIZE];
	}

	public void setMapOutputMode(int xRes, int yRes, int fps)
			throws GeneralException {
		MapOutputMode mapmode = m_imgGen.getMapOutputMode();

		mapmode.setXRes(xRes);
		mapmode.setYRes(yRes);
		mapmode.setFPS(fps);

		m_imgGen.setMapOutputMode(mapmode);
	}

	public ImageGenerator getGenerator() {
		return m_imgGen;
	}

	public ImageMap GetImageMap() throws StatusException {
		return m_imgGen.getMetaData().getData();
	}

	public Bitmap GetImage() {
		NativeMethods.readLocalBitmap2JavaBuffer(this.m_pixels, WIDTHSIZE);
		this.m_bitmap.setPixels(this.m_pixels, 0, WIDTHSIZE, 0, 0, WIDTHSIZE,
				HEIGHTSIZE);
		return this.m_bitmap;
	}

	public void videoEncodeRegister(String filename) {
		NativeMethods.videoEncodeRegister(filename);
	}

	public void videoEncodeFill() {
		NativeMethods.videoEncodeFill();
	}

	public void vedioEndcodeClose() {
		NativeMethods.vedioEndcodeClose();
	}

	public void Close() {
		m_imgGen.dispose();
	}
}

/*
 * Location:
 * C:\Users\vincentz\Documents\asus_xtion_rgb_output_test\libs\xtionsdk20131010
 * .jar Qualified Name: com.asus.xtionstartlibs.RGBData Java Class Version: 6
 * (50.0) JD-Core Version: 0.7.0.1
 */