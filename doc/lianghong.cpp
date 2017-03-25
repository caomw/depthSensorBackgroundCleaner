RGB跟深度对齐的接口是org.openni.AlternativeViewpointCapability


const XnLabel* UserTracker::GetUsersPixelsData()
{
    xn::SceneMetaData sceneMD;
    m_UserGenerator.GetUserPixels(0, sceneMD);
    return sceneMD.Data();
}

const XnLabel* pLabels = GetUsersPixelsData(); // holds a label map, i.e. the label (user ID) for each pixel

// Prepare the texture map, i.e. go over all relevant elements and set their value based
// on the depth and user.
// NOTE: we go over the original map and the assumption is that the texture size is equal or larger
// to the depth map resolution and that anything larger (e.g. because we increase to the closest,
// larger power of two) is not changed (therefore will remain whatever color was set before which
// in the initialization is probably 0). 
for (XnUInt16 nY=0; nY<yRes; nY++)
{
    for (XnUInt16 nX=0; nX < xRes; nX++)
    {
        // init to 0 (just in case we don't have better data.
        pTexBuf[0] = 0;
        pTexBuf[1] = 0;
        pTexBuf[2] = 0;
        // if pLabels is 0 it is background. Therefore we only set the color of the background
        // if bDrawBackground is true.
        if (bDrawBackground || *pLabels != 0)
        {
            nValue = *pDepth; // the depth of the current pixel
            XnLabel label = *pLabels; // the label of the current pixel
            XnUInt32 nColorID = label % s_nColors; // the color for the current label
            if (label == 0)
            {
                nColorID = s_nColors; // special background color
            }

            if (nValue != 0)
            {
                XnFloat newValue = s_pDepthHist[nValue]; // translate to the multiplier from the histogram

                pTexBuf[0] = (unsigned char)(newValue * s_Colors[nColorID][0]); 
                pTexBuf[1] = (unsigned char)(newValue * s_Colors[nColorID][1]);
                pTexBuf[2] = (unsigned char)(newValue * s_Colors[nColorID][2]);
            }
        }

        pDepth++;
        pLabels++;
        pTexBuf+=3; // each element is RGB so we move 3 at a time.
    }

    pTexBuf += (nTexWidth - xRes) *3; // move to the next line
}
