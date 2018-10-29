package com.huawei.hiaidemo.ModelSDK;

import android.content.res.AssetManager;
import android.util.Log;

import static com.huawei.hiaidemo.Utils.Constant.MODEL_NAME;


public class MobileNetModel {
    /**** user load model manager sync interfaces ****/
    public static int load(AssetManager mgr){
            return ModelManager.loadModelSync(MODEL_NAME, mgr);
    }

    public static float[] predict(float[] buf){
        return ModelManager.runModelSync(MODEL_NAME,buf);
    }

    public static int unload(){ return ModelManager.unloadModelSync();}

    /**** load user model async interfaces ****/
    public static int registerListenerJNI(ModelManagerListener listener){
        return ModelManager.registerListenerJNI(listener);
    }

    public static void loadAsync(AssetManager mgr){
        ModelManager.loadModelAsync(MODEL_NAME, mgr);
    }

    public static void predictAsync(float[] buf) {
        ModelManager.runModelAsync(MODEL_NAME, buf);
    }

    public static void unloadAsync(){
        ModelManager.unloadModelAsync();
    }
}
