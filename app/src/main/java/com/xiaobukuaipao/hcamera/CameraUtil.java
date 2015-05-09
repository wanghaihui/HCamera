package com.xiaobukuaipao.hcamera;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.view.OrientationEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xiaobu1 on 15-4-20.
 */
public class CameraUtil {
    // See android.hardware.Camera.ACTION_NEW_PICTURE.
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";

    private static ImageFileNamer sImageFileNamer;

    private CameraUtil() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static void initialize(Context context) {
        sImageFileNamer = new ImageFileNamer(
                context.getString(R.string.image_file_name_format));
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        int rotation = 0;
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            Camera.CameraInfo info = CameraHolder.instance().getCameraInfo()[cameraId];
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {
                rotation = (info.orientation + orientation) % 360;
            }
        }
        return rotation;
    }

    public static String createJpegName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateName(dateTaken);
        }
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        // Keep compatibility
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }


    private static class ImageFileNamer {
        private SimpleDateFormat mFormat;

        // The date (in milliseconds) used to generate the last name
        private long mLastDate;

        // Number of names generated for the same second
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            // If the last name was generated for the same second, we append _1, _2, etc to the name
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }
    }
}
