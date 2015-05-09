package com.xiaobukuaipao.hcamera;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xiaobu1 on 15-4-15.
 */
public class CameraView implements SurfaceHolder.Callback, Camera.PictureCallback,
                            Camera.AutoFocusCallback, View.OnTouchListener, ShakeListener.OnShakeListener {
    private static final String TAG = CameraView.class.getSimpleName();

    /**
     * Camera flash mode
     */
    public static final int FLASH_OFF = 0;
    public static final int FLASH_ON = 1;
    public static final int FLASH_AUTO = 2;

    /**
     * Camera preview size
     */
    public static final int MODE4T3 = 43;
    public static final int MODE16T9 = 169;

    // 当前的模式
    private int currentMode = MODE4T3;

    // holding a display surface
    private SurfaceHolder mHolder;

    // 相机
    private Camera camera;

    // 相机大小比较器
    private CameraSizeComparator sizeComparator = new CameraSizeComparator();

    // 聚焦View
    private FocusView focusView;

    // 0 Close, 1 Open, 2 Auto
    private int flash_type = FLASH_AUTO;
    // 0 Back camera, 1 Front camera
    private static int camera_position = Camera.CameraInfo.CAMERA_FACING_BACK;
    // 拍照时,旋转90度
    private int takePhotoOrientation = 90;

    /**
     * if you need square(正方形) picture , you can set true while you take a picture
     */
    private boolean isSequare;

    // 顶部距离
    private int topDistance;
    // Zoom--放大缩小标志
    private int zoomFlag = 0;

    // SurfaceView是用来预览Camera的，当全屏时就是Screen的大小
    private SurfaceView surfaceView;

    // 拍照目录
    private String PATH_DIR_PIC = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM) + "/Youngmam/Camera/";

    private String PATH_DIR_PIC_THUMBNAIL = PATH_DIR_PIC + "Thumbnail/";
    private String PATH_FILE;
    private String PATH_FILE_THUMBNAIL;
    // 目录的路径
    private String dirPath;

    // 屏幕DPI
    private int screenDpi;

    // 是否使用Android L
    private boolean using_android_l = false;
    private boolean using_texture_view = false;

    private OnCameraSelectListener onCameraSelectListener;

    public void setOnCameraSelectListener(OnCameraSelectListener onCameraSelectListener) {
        this.onCameraSelectListener = onCameraSelectListener;
    }

    private int picQuality = 80;

    public void setPicQuality(int picQuality) {
        if (picQuality > 0 && picQuality < 101) {
            this.picQuality = picQuality;
        }
    }

    /**
     * 两个手指之间的距离
     */
    private float mDistance;

    private ContentResolver mContentResolver;

    // We use a thread in MediaSaver to do the work of saving images. This
    // reduces the shot-to-shot time.
    private MediaSaver mMediaSaver;

    // The degrees of the device rotated clockwise from its natural orientation
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    // The value for android.hardware.Camera.Parameters.setRotation
    private int mJpegRotation;

    private MediaSaver.OnMediaSavedListener mOnMediaSavedListener = new MediaSaver.OnMediaSavedListener() {
        @Override
        public void onMediaSaved(Uri uri) {
            if(uri != null) {
                Log.d(TAG, "onMediaSaved");
                CameraUtil.broadcastNewPicture(context, uri);

                if (onCameraSelectListener != null) {
                    onCameraSelectListener.onTakePicture(true, PATH_FILE);
                }

                // 然后打开相机
                openCamera();
            }
        }
    };


    // 上下文
    private Context context;

    public CameraView(Context context) {
        this.context = context;
    }

    /**
     * 设置屏幕取景View
     */
    public void setCameraView(SurfaceView surfaceView) throws NullPointerException, ClassCastException {
        this.setCameraView(surfaceView, MODE4T3);
    }

    /**
     *
     * @param surfaceView the camera view you should give it
     * @param cameraMode set the camera preview proportion, default is MODE4T3
     * @throws NullPointerException
     * @throws ClassCastException
     */
    public void setCameraView(SurfaceView surfaceView, int cameraMode) throws NullPointerException, ClassCastException {
        this.surfaceView = surfaceView;
        this.currentMode = cameraMode;
        // 屏幕宽度
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (currentMode == MODE4T3) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.surfaceView.getLayoutParams();
            layoutParams.width = screenWidth;
            layoutParams.height = screenWidth * 4 / 3;
            this.surfaceView.setLayoutParams(layoutParams);
        } else if (currentMode == MODE16T9) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.surfaceView.getLayoutParams();
            layoutParams.width = screenWidth;
            layoutParams.height = screenWidth * 16 / 9;
            this.surfaceView.setLayoutParams(layoutParams);
        }

        ShakeListener.newInstance().setOnShakeListener(this);

        mContentResolver = context.getContentResolver();
        mMediaSaver  = new MediaSaver(mContentResolver);

        screenDpi = context.getResources().getDisplayMetrics().densityDpi;

        mHolder = surfaceView.getHolder();
        surfaceView.setOnTouchListener(this);
        mHolder.addCallback(this);
        // 保持屏幕始终开着
        mHolder.setKeepScreenOn(true);

        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 设置FocusView
     */
    public void setFocusView(FocusView focusView) {
        this.focusView = focusView;
    }

    /**
     * Use with activtiy or fragment life circle
     */
    public final void onResume() {
        if (surfaceView == null) {
            throw new NullPointerException("not init SurfaceView for camera view");
        }
        // 打开相机
        openCamera();
    }

    /**
     * Seem to onResume
     */
    public final void onPause() {
        closeCamera();
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        try {
            // 首先关闭相机
            closeCamera();

            camera = Camera.open(camera_position);

            // 设置相机的显示方向--openCamera()
            // setCameraDisplayOrientation();
            // 相机Portrait,设置显示角度偏90
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(mHolder);

            // 设置拍照后的PictureSize尺寸
            setCameraPictureSize();
            // 设置预览时,帧数据的尺寸
            setCameraPreviewSize();

            // 改变Flash Mode
            changeFlashMode(flash_type);
            camera.startPreview();

            // 设置自动聚焦
            handleFocus();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 关闭相机
     */
    private void closeCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }

        flash_type = FLASH_AUTO;
        camera = null;
    }

    /**
     * 相机Reset
     */
    private void resetCamera() {
        if (onCameraSelectListener != null) {
            onCameraSelectListener.onChangeCameraPosition(camera_position);
        }

        // 先关闭相机
        closeCamera();
        // 打开相机
        openCamera();
    }

    /**
     * 设置拍照后的PictureSize尺寸
     */
    private void setCameraPictureSize() {
        Camera.Parameters params = camera.getParameters();
        List<Size> sizes = params.getSupportedPictureSizes();
        // 排序
        Collections.sort(sizes, sizeComparator);
        for (Size size : sizes) {
            params.setPictureSize(size.width, size.height);

            if (size.width * 1.0 / size.height * 1.0 == 4.0 / 3.0 && currentMode == MODE4T3 && size.height < 2000) {
                break;
            } else if (size.width * 1.0 / size.height * 1.0 == 16.0 / 9.0 && currentMode == MODE16T9 && size.height < 2000) {
                break;
            }
        }

        params.setJpegQuality(picQuality);

        params.setPictureFormat(ImageFormat.JPEG);

        camera.setParameters(params);
    }

    /**
     * 设置预览帧大小
     */
    private void setCameraPreviewSize() {
        Camera.Parameters params = camera.getParameters();
        List<Size> sizes = params.getSupportedPreviewSizes();
        Collections.sort(sizes, sizeComparator);

        for (Size size : sizes) {
            params.setPreviewSize(size.width, size.height);

            if (size.width * 1.0 / size.height * 1.0 == 4.0 / 3.0 && currentMode == MODE4T3) {
                break;
            } else if (size.width * 1.0 / size.height * 1.0 == 16.0 / 9.0 && currentMode == MODE16T9) {
                break;
            }
        }
        camera.setParameters(params);
    }

    /**
     * change camera facing
     */
    public final int changeCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for(int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);

            if (camera_position == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera_position = Camera.CameraInfo.CAMERA_FACING_FRONT;

                resetCamera();
                return camera_position;
            } else if (camera_position == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                camera_position = Camera.CameraInfo.CAMERA_FACING_BACK;
                resetCamera();
                return camera_position;
            }
        }
        return camera_position;
    }

    /**
     * change camera flash mode
     */
    public final int changeFlashMode(int flash_type) {
        this.flash_type = flash_type;
        return changeFlashMode();
    }

    /**
     * change camera flash mode
     * @return
     */
    public final int changeFlashMode() {
        if (camera == null) {
            return -1;
        }

        Camera.Parameters parameters = camera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes == null || flashModes.size() <= 1) {
            return 0;
        }

        // 改变模式
        if (onCameraSelectListener != null) {
            onCameraSelectListener.onChangeFlashMode((flash_type) % 3);
        }

        switch (flash_type % 3) {
            case FLASH_ON:
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    flash_type++;
                    camera.setParameters(parameters);


                }
                break;
            case FLASH_OFF:
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    flash_type++;
                    camera.setParameters(parameters);
                }
                break;
            case FLASH_AUTO:
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    flash_type++;
                    camera.setParameters(parameters);
                }
                break;
            default:
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    flash_type++;
                    camera.setParameters(parameters);
                }
                break;
        }

        return flash_type;
    }

    /**
     * 拍照
     */
    public final void takePicture(boolean isSequare) {
        if (camera != null) {
            this.isSequare = isSequare;

            // 设置照片的拍摄角度
            mJpegRotation = CameraUtil.getJpegRotation(camera_position, mOrientation);

            /*mJpegRotation += 90;

            Log.i(TAG, "take picture orientation : " + mJpegRotation);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setRotation(mJpegRotation);
            camera.setParameters(parameters);*/

            camera.takePicture(null, null, this);
        }
    }


    /**
     * A client may implement this interface to receive information about changes to the surface
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHolder = holder;
        mHolder.setKeepScreenOn(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (screenDpi == DisplayMetrics.DENSITY_HIGH) {
            zoomFlag = 10;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    /**
     * supply image data from a photo capture--照相机拍完照后,对得到的数据进行处理
     * @param data
     * @param camera
     */
    // 此时,已经得到拍到的照片,需要将照片登记到MediaStore中
    @Override
    public void onPictureTaken(final byte[] data, final Camera camera) {
        try {
            if (dirPath != null && !dirPath.equals("")) {
                PATH_DIR_PIC = dirPath;
            }

            long currentTime = System.currentTimeMillis();
            String title = CameraUtil.createJpegName(currentTime);
            long date = currentTime;

            PATH_FILE = PATH_DIR_PIC + title + ".jpg";

            // 首先创建文件夹
            createFolder(PATH_DIR_PIC);
            // 创建文件
            createFile(PATH_FILE);

            Size s = camera.getParameters().getPictureSize();

            mMediaSaver.addImage(data, title, date, s.width, s.height, 0, mOnMediaSavedListener);

            // closeCamera();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createParentFolder(File file) throws Exception {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new Exception("create parent directory failure!");
            }
        }
    }

    /**
     * 创建文件夹
     */
    private void createFolder(String path) throws Exception {
        path = separatorReplace(path);
        File folder = new File(path);
        if (folder.isDirectory()) {
            return;
        } else if (folder.isFile()) {
            deleteFile(path);
        }

        folder.mkdirs();
    }

    private File createFile(String path) throws Exception {
        path = separatorReplace(path);
        File file = new File(path);
        if (file.isFile()) {
            return file;
        } else if (file.isDirectory()) {
            deleteFolder(path);
        }

        return createFile(file);
    }

    private File createFile(File file) throws Exception {
        createParentFolder(file);
        if (!file.createNewFile()) {
            throw new Exception("create file failure!");
        }
        return file;
    }

    private String separatorReplace(String path) {
        return path.replace("\\", "/");
    }

    private void deleteFolder(String path) throws Exception {
        path = separatorReplace(path);
        File folder = getFolder(path);
        File[] files = folder.listFiles();
        for(File file : files) {
            if (file.isDirectory()) {
                deleteFolder(file.getAbsolutePath());
            } else if (file.isFile()) {
                deleteFile(file.getAbsolutePath());
            }
        }

        folder.delete();
    }

    private File getFolder(String path) throws FileNotFoundException {
        path = separatorReplace(path);
        File folder = new File(path);
        if (!folder.isDirectory()) {
            throw new FileNotFoundException("folder not found!");
        }

        return folder;
    }

    private void deleteFile(String path) throws Exception {
        path = separatorReplace(path);
        File file = getFile(path);
        if (!file.delete()) {
            throw new Exception("delete file failure");
        }
    }

    private File getFile(String path) throws FileNotFoundException {
        path = separatorReplace(path);
        File file = new File(path);
        if (!file.isFile()) {
            throw new FileNotFoundException("file not found!");
        }
        return file;
    }

    /**
     * used to notify on completion of camera auto focus
     * @param success
     * @param camera
     */
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            //  如果聚焦成功

        }
    }

    /**
     * a callback to be invoked when a touch event is dispatched to this view
     * 主要用于聚焦
     * @param v
     * @param event
     * @return
     */
    // true if the listener has consumed the event, false otherwise
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (camera == null) {
            return false;
        }

        Camera.Parameters params = camera.getParameters();
        int action = event.getAction();

        if (event.getPointerCount() > 1) {
            // 此时是非单手指
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDistance = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                // 取消自动聚焦
                camera.cancelAutoFocus();
                // 此时,进行缩放
                handleZoom(event, params);
            }

            // Focus View
            if (focusView != null) {
                // Clear
            }
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                if (focusView != null) {
                    // Clear
                }
            }

            if (action == MotionEvent.ACTION_UP) {
                // 处理Focus
                handleFocus(event);
            }
        }
        return true;
    }

    /**
     * 得到两个手指之间的距离
     */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /**
     * 处理缩放
     */
    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();

        float newDist = getFingerSpacing(event);

        if (newDist > mDistance && (newDist - mDistance > zoomFlag)) {
            if (zoom < maxZoom) {
                zoom++;
            }
        } else if (newDist < mDistance && (mDistance - newDist > zoomFlag)) {
            if (zoom > 0) {
                zoom--;
            }
        }

        mDistance = newDist;
        params.setZoom(zoom);
        camera.setParameters(params);
    }

    /**
     * 处理聚焦--默认中间
     */
    public void handleFocus() {
        if (camera != null) {
            camera.autoFocus(this);
        }
    }

    /**
     * 处理聚焦--点击事件
     */
    public void handleFocus(MotionEvent event) {
        if (camera != null) {
            camera.cancelAutoFocus();

            // set Camera parameters
            Camera.Parameters params = camera.getParameters();

            if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

                Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
                meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
                Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
                meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
                params.setMeteringAreas(meteringAreas);
            }

            camera.setParameters(params);

            camera.autoFocus(this);
        }
    }

    /**
     * 镜头晃动方向
     * @param orientation
     */
    @Override
    public void onShake(int orientation) {

    }


    /**
     * 照相选择监听器
     */
    public interface OnCameraSelectListener {
        /**
         * 拍照时执行
         * @param success
         * @param filePath
         */
        public void onTakePicture(boolean success, String filePath);

        /**
         * 闪光灯改变
         * @param flashMode
         */
        public void onChangeFlashMode(int flashMode);

        /**
         * 前后摄像头切换
         * @param cameraPosition
         */
        public void onChangeCameraPosition(int cameraPosition);

        public void onShake(int orientation);
    }

    /**
     * Camera Size比较器
     */
    public static class CameraSizeComparator implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width < rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }


    /**
     * 相机的方向问题
     */
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 设置相机的显示方向--openCamera()
     */
    private void setCameraDisplayOrientation() {
        if (using_android_l) {
            // need to configure the textureview
            // 待定
        } else {
            int rotation = getDisplayRotation();

            Log.d(TAG, "rotation : " + rotation);

            int degree = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degree = 0;
                    break;
                case Surface.ROTATION_90:
                    degree = 90;
                    break;
                case Surface.ROTATION_180:
                    degree = 180;
                    break;
                case Surface.ROTATION_270:
                    degree = 270;
                    break;
            }

            camera.setDisplayOrientation(degree);
        }
    }

    /**
     * 得到显示角度
     */
    private int getDisplayRotation() {
        // gets the display rotation (as a Surface.ROTATION_* constant)
        Activity activity = (Activity) context;
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Log.d(TAG, "Display Rotation : " + rotation);

        // 默认是0,以后可以通过Preference来配置
        String rotate_preview = "0";
        if (rotate_preview.equals("180")) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotation = Surface.ROTATION_180;
                    break;
                case Surface.ROTATION_90:
                    rotation = Surface.ROTATION_270;
                    break;
                case Surface.ROTATION_180:
                    rotation = Surface.ROTATION_0;
                    break;
                case Surface.ROTATION_270:
                    rotation = Surface.ROTATION_90;
                    break;
            }
        }

        // rotation = 1 = Surface.ROTATION_90
        return rotation;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

}
