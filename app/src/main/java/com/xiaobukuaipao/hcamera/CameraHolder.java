package com.xiaobukuaipao.hcamera;

import android.hardware.Camera.CameraInfo;

/**
 * Created by xiaobu1 on 15-4-20.
 */
public class CameraHolder {
    // current camera id
    private int mCameraId = -1;
    private int mBackCameraId = -1;
    private int mFrontCameraId = -1;


    private final int mNumberOfCameras;
    private final CameraInfo[] mInfo;

    // Use a singleton
    private static CameraHolder sHolder;

    public static synchronized CameraHolder instance() {
        if (sHolder == null) {
            sHolder = new CameraHolder();
        }
        return sHolder;
    }

    private CameraHolder() {
        mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
        mInfo = new CameraInfo[mNumberOfCameras];

        for (int i=0; i < mNumberOfCameras; i++) {
            mInfo[i] = new CameraInfo();
            android.hardware.Camera.getCameraInfo(i, mInfo[i]);
        }

        // get the first (smallest) back and first front camera id
        for (int i=0; i < mNumberOfCameras; i++) {
            if (mBackCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraId = i;
            } else if (mFrontCameraId == -1 && mInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraId = i;
            }
        }
    }

    public int getNumberOfCameras() {
        return mNumberOfCameras;
    }

    public CameraInfo[] getCameraInfo() {
        return mInfo;
    }
}
