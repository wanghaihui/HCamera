package com.xiaobukuaipao.hcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by xiaobu1 on 15-4-15.
 */
public class FocusView extends SurfaceView implements SurfaceHolder.Callback {

    private Context context;

    private SurfaceHolder holder;

    private Bitmap bitmap;

    private Paint paint = new Paint();

    private float rectWidth;

    public FocusView(Context context) {
        super(context);
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * A client may implement this interface to receive information about changes to the surface
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
