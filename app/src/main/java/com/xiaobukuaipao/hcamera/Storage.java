package com.xiaobukuaipao.hcamera;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by xiaobu1 on 15-4-20.
 */
public class Storage {
    private static final String TAG = "Camera Storage";
    public static final String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static final String DIRECTORY = DCIM + "/Youngmam/Camera";

    // Match the code in MediaProvider.computeBucketValues()
    public static final String BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD = 50000000;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaStore.MediaColumns.WIDTH, width);
            values.put(MediaStore.MediaColumns.HEIGHT, height);
        }
    }

    /**
     * 写文件
     * @param path
     * @param data
     */
    public static void writeFile(String path, byte[] data) {

        FileOutputStream out = null;

        /*try {
            out = new FileOutputStream(path);
            out.write(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {

            }
        }*/
        try {
            Bitmap bitmap = null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            // inPurgeable is used to free up memory while required
            options.inPurgeable = true;

            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            ExifInterface exifInterface = new ExifInterface(path);

            int rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Log.d(TAG, "rotation :" + rotation);

            // 偏转90度
            switch (rotation) {
                case 0:
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                    break;
                case 90:
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    break;
                case 180:
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_180));
                    break;
                case 270:
                    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
                    break;
            }

            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            out = new FileOutputStream(path);

            BufferedOutputStream bos = new BufferedOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();

            exifInterface.saveAttributes();
            bitmap.recycle();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save the iamge and add it to media store
    public static Uri addImage(ContentResolver resolver, byte[] jpeg, String title, long date,
                               int width, int height, int orientation) {
        // Save the image
        String path = generateFilepath(title);

        writeFile(path, jpeg);

        return addImage(resolver, jpeg.length, path, title, date, width, height, orientation);
    }

    // Add the image to media store
    public static Uri addImage(ContentResolver resolver, int jpegLength, String path, String title,
                               long date, int width, int height, int orientation) {

        int degree = readPictureDegree(path);

        // Insert into MediaStore
        ContentValues values = new ContentValues(7);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");

        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, degree);

        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th) {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }

        return uri;
    }

    // 生成文件路径
    public static String generateFilepath(String title) {
        return DIRECTORY + "/" + title + ".jpg";
    }

    // 删除图片
    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to delete image : " + uri);
        }
    }

    /* 读取照片exif信息中的旋转角度
    * @param path 照片路径
    * @return角度
    */
    public static int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
}
