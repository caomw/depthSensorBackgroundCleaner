/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>

#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_WARN, "BackRemoval", __VA_ARGS__);

const int kernelSize = 10;
const int neighborCount = (kernelSize * 2 + 1) * (kernelSize * 2 + 1);
int holeOffsets[neighborCount];
bool holeOffsets_inited = false;

int mcolorwidth, mcolorheight;
int mdepthwidth, mdepthheight;
int colorPixelCount;
unsigned char* mAlphaArray;
const unsigned char kAddValue = 1;

float colorToDepthX = 1;
float colorToDepthY = 1;

#define clamp(a,min,max) \
    ({__typeof__ (a) _a__ = (a); \
      __typeof__ (min) _min__ = (min); \
      __typeof__ (max) _max__ = (max); \
      _a__ < _min__ ? _min__ : _a__ > _max__ ? _max__ : _a__; })

// Based heavily on http://vitiy.info/Code/stackblur.cpp
// See http://vitiy.info/stackblur-algorithm-multi-threaded-blur-for-cpp/
// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

static unsigned short const stackblur_mul[255] =
{
    512, 512, 456, 512, 328, 456, 335, 512, 405, 328, 271, 456, 388, 335, 292, 512,
    454, 405, 364, 328, 298, 271, 496, 456, 420, 388, 360, 335, 312, 292, 273, 512,
    482, 454, 428, 405, 383, 364, 345, 328, 312, 298, 284, 271, 259, 496, 475, 456,
    437, 420, 404, 388, 374, 360, 347, 335, 323, 312, 302, 292, 282, 273, 265, 512,
    497, 482, 468, 454, 441, 428, 417, 405, 394, 383, 373, 364, 354, 345, 337, 328,
    320, 312, 305, 298, 291, 284, 278, 271, 265, 259, 507, 496, 485, 475, 465, 456,
    446, 437, 428, 420, 412, 404, 396, 388, 381, 374, 367, 360, 354, 347, 341, 335,
    329, 323, 318, 312, 307, 302, 297, 292, 287, 282, 278, 273, 269, 265, 261, 512,
    505, 497, 489, 482, 475, 468, 461, 454, 447, 441, 435, 428, 422, 417, 411, 405,
    399, 394, 389, 383, 378, 373, 368, 364, 359, 354, 350, 345, 341, 337, 332, 328,
    324, 320, 316, 312, 309, 305, 301, 298, 294, 291, 287, 284, 281, 278, 274, 271,
    268, 265, 262, 259, 257, 507, 501, 496, 491, 485, 480, 475, 470, 465, 460, 456,
    451, 446, 442, 437, 433, 428, 424, 420, 416, 412, 408, 404, 400, 396, 392, 388,
    385, 381, 377, 374, 370, 367, 363, 360, 357, 354, 350, 347, 344, 341, 338, 335,
    332, 329, 326, 323, 320, 318, 315, 312, 310, 307, 304, 302, 299, 297, 294, 292,
    289, 287, 285, 282, 280, 278, 275, 273, 271, 269, 267, 265, 263, 261, 259
};

static unsigned char const stackblur_shr[255] =
{
    9, 11, 12, 13, 13, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 17,
    17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19,
    19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20,
    20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 21,
    21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
    21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22,
    22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
    22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23,
    23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
    23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
    23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
    23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24
};

void fillHolesJob(unsigned char* src,                ///< input image data
                  unsigned int w,                    ///< image width
                  unsigned int h,                    ///< image height
                  unsigned int radius,               ///< blur intensity (should be in 2..254 range)
                  int cores,                         ///< total number of working threads
                  int core,                          ///< current thread number
                  int step                           ///< step of processing (1,2)
                 )
{
    unsigned int x, y, xp, yp, i;
    unsigned int sp;
    unsigned int stack_start;
    unsigned char* stack_ptr;

    unsigned char* src_ptr;
    unsigned char* dst_ptr;

    unsigned long sum_r;
    unsigned long sum_in_r;
    unsigned long sum_out_r;

    unsigned int wm = w - 1;
    unsigned int hm = h - 1;
    unsigned int w4 = w * 1;
    unsigned int div = (radius * 2) + 1;
    unsigned int mul_sum = stackblur_mul[radius];
    unsigned char shr_sum = stackblur_shr[radius];
    unsigned char stack[div * 1];

    if (step == 1)
    {
        int minY = core * h / cores;
        int maxY = (core + 1) * h / cores;

        for (y = minY; y < maxY; y++)
        {
            sum_r  =
                sum_in_r =
                    sum_out_r = 0;

            src_ptr = src + w4 * y; // start of line (0,y)

            for (i = 0; i <= radius; i++)
            {
                stack_ptr    = &stack[ 1 * i ];
                stack_ptr[0] = src_ptr[0];
                sum_r += src_ptr[0] * (i + 1);
                sum_out_r += src_ptr[0];
            }


            for (i = 1; i <= radius; i++)
            {
                if (i <= wm) src_ptr += 1;
                stack_ptr = &stack[ 1 * (i + radius) ];
                stack_ptr[0] = src_ptr[0];
                sum_r += src_ptr[0] * (radius + 1 - i);
                sum_in_r += src_ptr[0];
            }


            sp = radius;
            xp = radius;
            if (xp > wm) xp = wm;
            src_ptr = src + 1 * (xp + y * w); //   img.pix_ptr(xp, y);
            dst_ptr = src + y * w4; // img.pix_ptr(0, y);
            for (x = 0; x < w; x++)
            {
                int alpha = 255;
                dst_ptr[0] = clamp((sum_r * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr += 1;

                sum_r -= sum_out_r;

                stack_start = sp + div - radius;
                if (stack_start >= div) stack_start -= div;
                stack_ptr = &stack[1 * stack_start];

                sum_out_r -= stack_ptr[0];

                if (xp < wm)
                {
                    src_ptr += 1;
                    ++xp;
                }

                stack_ptr[0] = src_ptr[0];

                sum_in_r += src_ptr[0];
                sum_r    += sum_in_r;

                ++sp;
                if (sp >= div) sp = 0;
                stack_ptr = &stack[sp * 1];

                sum_out_r += stack_ptr[0];
                sum_in_r  -= stack_ptr[0];
            }

        }
    }

    // step 2
    if (step == 2)
    {
        int minX = core * w / cores;
        int maxX = (core + 1) * w / cores;

        for (x = minX; x < maxX; x++)
        {
            sum_r =
                sum_in_r =
                    sum_out_r = 0;

            src_ptr = src + 1 * x; // x,0
            for (i = 0; i <= radius; i++)
            {
                stack_ptr    = &stack[i * 1];
                stack_ptr[0] = src_ptr[0];
                sum_r           += src_ptr[0] * (i + 1);
                sum_out_r       += src_ptr[0];
            }
            for (i = 1; i <= radius; i++)
            {
                if (i <= hm) src_ptr += w4; // +stride

                stack_ptr = &stack[1 * (i + radius)];
                stack_ptr[0] = src_ptr[0];
                sum_r += src_ptr[0] * (radius + 1 - i);
                sum_in_r += src_ptr[0];
            }

            sp = radius;
            yp = radius;
            if (yp > hm) yp = hm;
            src_ptr = src + 1 * (x + yp * w); // img.pix_ptr(x, yp);
            dst_ptr = src + 1 * x;               // img.pix_ptr(x, 0);
            for (y = 0; y < h; y++)
            {
                int alpha = 255;
                dst_ptr[0] = clamp((sum_r * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr += w4;

                sum_r -= sum_out_r;

                stack_start = sp + div - radius;
                if (stack_start >= div) stack_start -= div;
                stack_ptr = &stack[1 * stack_start];

                sum_out_r -= stack_ptr[0];

                if (yp < hm)
                {
                    src_ptr += w4; // stride
                    ++yp;
                }

                stack_ptr[0] = src_ptr[0];

                sum_in_r += src_ptr[0];
                sum_r    += sum_in_r;

                ++sp;
                if (sp >= div) sp = 0;
                stack_ptr = &stack[sp * 1];

                sum_out_r += stack_ptr[0];
                sum_in_r  -= stack_ptr[0];
            }
        }
    }
}

extern "C"
{
    static void Release()
    {
        delete[] mAlphaArray;
        mAlphaArray = 0;
    }

    JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved)
    {
        Release();
    }

    static void SafeCreate(int colorwidth, int colorheight, int depthwidth, int depthheight)
    {
        if (mcolorwidth != colorwidth || mcolorheight != colorheight
            || mdepthwidth != depthwidth || mdepthheight != depthheight)
        {
            mcolorwidth = colorwidth;
            mcolorheight = colorheight;
            mdepthwidth = depthwidth;
            mdepthheight = depthheight;
            colorPixelCount = mcolorwidth * mcolorheight;

            colorToDepthX  = mdepthwidth / (float) mcolorwidth;
            colorToDepthY  = mdepthheight / (float) mcolorheight;

            Release();

            mAlphaArray = new unsigned char[colorPixelCount];
        }
    }

    static void InternalGetBackRemovedColorData( int outColorArray[],             // output: the result image with alpha transparency
            const int colorArray[],         // input: color data
            const short depthArray[],       // input: depth data
            const short userLabelArray[],   // input: user label
            int trackingID, int blurEffect) // input: the user label id to track, specify 0 to track all the possible users
    {
        memset(mAlphaArray, 0, sizeof mAlphaArray[0] * colorPixelCount );

        // scaling, maybe downscale, maybe upscale
        for (int y = 0; y < mcolorheight; y++) {
            int depthY = y * colorToDepthY;
            for (int x = 0; x < mcolorwidth; x++) {
                int depthX = x * colorToDepthX;
                int labelIdx = depthY * mdepthwidth + depthX;
                if ((trackingID <= 0 && userLabelArray[labelIdx] > 0) ||
                        (trackingID > 0 && userLabelArray[labelIdx] == trackingID)) {
                    mAlphaArray[y * mcolorwidth + x] = 255;
                }
            }
        }

        fillHolesJob(mAlphaArray,
                     mcolorwidth, mcolorheight,
                     blurEffect,
                     1, 0, 1);
        fillHolesJob(mAlphaArray,
                     mcolorwidth, mcolorheight,
                     blurEffect,
                     1, 0, 2);
        const unsigned char* src = (const unsigned char*)colorArray;
        unsigned char* dst = (unsigned char*)outColorArray;
        for (int i = 0; i < colorPixelCount; i++) {
            unsigned char alpha = clamp(mAlphaArray[i], 0, 255);
#if 1
            dst[i * 4 + 0] = src[i * 4 + 0]; // b
            dst[i * 4 + 1] = src[i * 4 + 1]; // g
            dst[i * 4 + 2] = src[i * 4 + 2]; // r
            dst[i * 4 + 3] = alpha;    // a
#else
            dst[i * 4 + 0] = alpha * src[i * 4 + 0]; // b
            dst[i * 4 + 1] = alpha * src[i * 4 + 1]; // g
            dst[i * 4 + 2] = alpha * src[i * 4 + 2]; // r
            dst[i * 4 + 3] = 255;       // a
#endif
        }
    }


    void GetBackRemovedColorData(int outColorArray[], int colorArray[], short depthArray[], short userLabelArray[],
                                 int colorwidth, int colorheight,
                                 int depthwidth, int depthheight,
                                 int trackingID, int blurEffect)
    {
        SafeCreate(colorwidth, colorheight, depthwidth, depthheight);
        InternalGetBackRemovedColorData(outColorArray, colorArray, depthArray, userLabelArray, trackingID, blurEffect);
    }


    void Java_com_orbbec_NativeNI_BackRemoval_GetBackRemovedColorData(JNIEnv* env, jobject thiz,
            jintArray jOutColorArray,             // output: the result image with alpha transparency
            jintArray jColorArray,         // input: color data
            jshortArray jDepthArray,       // input: depth data
            jshortArray jUserLabelArray,   // input: user label
            int colorwidth, int colorheight, //  input: color width & height
            int depthwidth, int depthheight, //  input: depth width & height
            int trackingID, int blurEffect) // input: the user label id to track, specify 0 to track all the possible users
    {
        SafeCreate(colorwidth, colorheight, depthwidth, depthheight);

        jint* colorArray = env->GetIntArrayElements(jColorArray, NULL);
        jshort* depthArray = env->GetShortArrayElements(jDepthArray, NULL);
        jshort* userLabelArray = env->GetShortArrayElements(jUserLabelArray, NULL);
        jint* outColorArray = env->GetIntArrayElements(jOutColorArray, NULL);

        InternalGetBackRemovedColorData(outColorArray, colorArray, depthArray, userLabelArray, trackingID, blurEffect);

        env->ReleaseIntArrayElements(jColorArray, colorArray, 0);
        env->ReleaseShortArrayElements(jDepthArray, depthArray, 0);
        env->ReleaseShortArrayElements(jUserLabelArray, userLabelArray, 0);
        env->ReleaseIntArrayElements(jOutColorArray, outColorArray, 0);
    }
} // extern "C"
