package com.xiaobukuaipao.hcamera;

/**
 * Created by xiaobu1 on 15-4-20.
 */
import android.provider.MediaStore.MediaColumns;import java.lang.Class;import java.lang.NoSuchFieldException;import java.lang.String;
/**
 * Android系统API帮助类
 */
public class ApiHelper {

    public static interface VERSION_CODES {
        // These value are copied from Build.VERSION_CODES
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
    }

    public static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT = hasField(
            MediaColumns.class, "WIDTH");

    private static boolean hasField(Class<?> klass, String fieldName) {
        try {
            klass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
