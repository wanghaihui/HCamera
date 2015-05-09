package com.xiaobukuaipao.hcamera;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiaobu1 on 15-4-15.
 */
public class PhotoCaptureActivity extends FragmentActivity implements CameraView.OnCameraSelectListener {

    private CameraView cameraView;

    private ImageButton cameraChange;
    private ImageButton cameraFlash;
    private ImageButton takePicture;

    // 画廊
    private LinearLayout gallery;
    private LayoutInflater inflater;

    // 拍照的照片路径
    private List<String> mPhotoCaptureList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_capture);

        // Camera工具类初始化
        CameraUtil.initialize(this);

        try {
            // 设置Context
            cameraView = new CameraView(this);
            cameraView.setOnCameraSelectListener(this);

            // Set Focus View
            cameraView.setFocusView((FocusView) findViewById(R.id.camera_focus));

            cameraView.setCameraView((SurfaceView) findViewById(R.id.camera_preview), CameraView.MODE4T3);
            cameraView.setPicQuality(70);
        } catch (Exception e) {
            e.printStackTrace();
        }

        inflater = LayoutInflater.from(this);
        mPhotoCaptureList = new ArrayList<String>();

        initViews();

    }

    /**
     * 初始化Views
     */
    private void initViews() {
        cameraChange = (ImageButton) findViewById(R.id.camera_change);
        cameraFlash = (ImageButton) findViewById(R.id.camera_flash);
        takePicture = (ImageButton) findViewById(R.id.camera_take_picture);

        gallery = (LinearLayout) findViewById(R.id.gallery);
    }

    /**
     * 设置监听器
     */
    public void setUIListeners() {
        cameraChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.changeCamera();
            }
        });

        cameraFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.changeFlashMode();
            }
        });

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 默认不是正方形
                cameraView.takePicture(false);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置监听器
        setUIListeners();
    }

    /**
     * Activity生命周期中, onCreate, onStart, onResume都不是真正visible的时间点，真正的visible时间点是onWindowFocusChanged()函数被执行时
     * 这个onWindowFocusChanged指的是这个Activity得到或者失去焦点的时候,就会调用
     * 如果你想要做一个Activity一加载完毕，就触发什么的话 完全可以用这个
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 如果,获得焦点
        if (hasFocus) {
            // 打开相机
            cameraView.onResume();
            // 设置顶部将距离
            // cameraView.setTopDistance()
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (cameraView != null) {
            cameraView.onPause();
        }
    }


    /**
     * 拍照后,成功与否进行处理
     * @param success
     * @param filePath
     */
    @Override
    public void onTakePicture(final boolean success, final String filePath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 如果成功
                if (success) {
                    addImageToGallery(filePath);
                }
            }
        });
    }

    /**
     * 闪光灯改变
     * @param flashMode
     */
    @Override
    public void onChangeFlashMode(int flashMode) {
        switch (flashMode) {
            case CameraView.FLASH_AUTO:
                cameraFlash.setBackgroundResource(R.mipmap.camera_flash_auto);
                break;
            case CameraView.FLASH_OFF:
                cameraFlash.setBackgroundResource(R.mipmap.camera_flash_off);
                break;
            case CameraView.FLASH_ON:
                cameraFlash.setBackgroundResource(R.mipmap.camera_flash_on);
                break;
        }
    }

    /**
     * 前后摄像头切换
     * @param cameraPosition
     */
    @Override
    public void onChangeCameraPosition(int cameraPosition) {

    }

    @Override
    public void onShake(int orientation) {

    }

    /**
     * 将图片加入到Gallery中
     */
    private void addImageToGallery(final String filePath) {
        int position = 0;
        // 缓存
        mPhotoCaptureList.add(filePath);

        // 当前图片是第几张
        position = mPhotoCaptureList.size() - 1;

        View view = inflater.inflate(R.layout.item_horizontal_gallery, gallery, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.image_item);

        /**
         * 给每一个View设置监听器
         */
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) v.getTag();
                /*ViewPagerDialogFragment fragment = new ViewPagerDialogFragment(mPhotoCaptureList, position);
                fragment.setStyle(DialogFragment.STYLE_NO_TITLE,
                        android.R.style.Theme_Holo_NoActionBar_Fullscreen);
                fragment.show(getSupportFragmentManager(), "viewpager");*/
            }
        });

        gallery.addView(view);


    }

}
