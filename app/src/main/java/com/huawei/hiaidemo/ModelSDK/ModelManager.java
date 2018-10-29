package com.huawei.hiaidemo.ModelSDK;

import android.content.res.AssetManager;
import android.util.Log;


public class ModelManager {

    static {
        System.loadLibrary("hiai");
    }

    /* DDK model manager sync interfaces */
    public static native int loadModelSync(String modelName, AssetManager mgr);

    public static native float[] runModelSync(String modelName, float[] buf);

    public static native int unloadModelSync();



    /* DDK model manager async interfaces */
    public static native int registerListenerJNI(ModelManagerListener listener);

    public static native void loadModelAsync(String modelName, AssetManager mgr);

    public static native void runModelAsync(String modelName, float[] buf);

    public static native void unloadModelAsync();
}
