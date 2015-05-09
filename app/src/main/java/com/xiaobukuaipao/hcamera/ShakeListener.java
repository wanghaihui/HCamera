package com.xiaobukuaipao.hcamera;

/**
 * Created by xiaobu1 on 15-4-15.
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * 抖动监听器--继承自传感器事件监听器
 */
public class ShakeListener implements SensorEventListener {
    public static final int LANDSCAPE_LEFT = 0;
    public static final int LANDSCAPE_RIGHT = 180;
    public static final int PORTRAIT = 90;

    private OnShakeListener onShakeListener;

    public void setOnShakeListener(OnShakeListener onShakeListener) {
        this.onShakeListener = onShakeListener;
    }

    /**
     * 单例模式
     */
    private static ShakeListener instance;

    public static ShakeListener newInstance() {
        if (instance == null) {
            instance = new ShakeListener();
        }
        return instance;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    /**
     * 精准性
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public interface OnShakeListener {
        public void onShake(int orientation);
    }
}
