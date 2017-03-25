package com.orb.green;

import org.openni.AlternativeViewpointCapability;
import org.openni.DepthGenerator;
import org.openni.DepthMap;
import org.openni.DepthMetaData;
import org.openni.GeneralException;
import org.openni.MapOutputMode;
import org.openni.Point3D;
import org.openni.ProductionNode;
import org.openni.StatusException;

import com.asus.xtionstartkernel.XtionContext;

public class MyDepthData {
	private static DepthGenerator m_depthGen;

	public MyDepthData(XtionContext context) throws GeneralException {
		m_depthGen = DepthGenerator.create(context.m_opennicontext);
	}

	public void setMapOutputMode(int xRes, int yRes, int fps)
			throws GeneralException {
		MapOutputMode mapmode = m_depthGen.getMapOutputMode();

		mapmode.setXRes(xRes);
		mapmode.setYRes(yRes);
		mapmode.setFPS(fps);

		m_depthGen.setMapOutputMode(mapmode);
	}

	public DepthGenerator getGenerator() {
		return m_depthGen;
	}
	
	public DepthMap GetDepthMap() throws StatusException {
		return m_depthGen.getMetaData().getData();
	}

	public void Close() {
		m_depthGen.dispose();
	}

	public void setViewpoint(ProductionNode paramProductionNode) {
		try {
			m_depthGen.getAlternativeViewpointCapability().setViewpoint(
					paramProductionNode);
		} catch (StatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Point3D[] convertProjectiveToRealWorld(Point3D[] arg0)
			throws StatusException {
		return m_depthGen.convertProjectiveToRealWorld(arg0);
	}

	public Point3D convertProjectiveToRealWorld(Point3D arg0)
			throws StatusException {
		return m_depthGen.convertProjectiveToRealWorld(arg0);
	}

	public Point3D convertRealWorldToProjective(Point3D arg0)
			throws StatusException {
		return m_depthGen.convertRealWorldToProjective(arg0);
	}

	public Point3D[] convertRealWorldToProjective(Point3D[] arg0)
			throws StatusException {
		return m_depthGen.convertRealWorldToProjective(arg0);
	}
}

/*
 * Location:
 * C:\Users\vincentz\Documents\asus_xtion_rgb_output_test\libs\xtionsdk20131010
 * .jar Qualified Name: com.asus.xtionstartkernel.DepthData Java Class Version:
 * 6 (50.0) JD-Core Version: 0.7.0.1
 */