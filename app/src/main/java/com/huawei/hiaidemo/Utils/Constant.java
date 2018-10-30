package com.huawei.hiaidemo.Utils;

import android.Manifest;

public class Constant {
    public static String MODEL_NAME = "MobileNet";

    public static final int RESIZED_WIDTH = 224;
    public static final int RESIZED_HEIGHT = 224;

    public static final int GALLERY_REQUEST_CODE = 0;
    public static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    public static final int CODE_SELECT_IMAGE = 2;
    public static final int CAMERA_AND_STORAGE = 3;

    public static final String[] PERMISSION = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    public static final String IMAGE_PREFIX= "HiAI_SmartPhoto_";

}
