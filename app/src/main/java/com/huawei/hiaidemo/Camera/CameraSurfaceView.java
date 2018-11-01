package com.huawei.hiaidemo.Camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.huawei.hiaidemo.Utils.Constant.PERMISSION;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback{
    private static final String TAG = "jrr";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final int ORIENTATION = 90;
    private int mScreenWidth;
    private int mScreenHeight;
    private boolean isOpen;
    // 0表示后置，1表示前置
    private int cameraPosition = 1;
    private Context context;
    private SurfaceHolder holder;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getScreenMatrix(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void getScreenMatrix(Context context) {
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        WM.getDefaultDisplay().getMetrics(outMetrics);
        mScreenHeight = outMetrics.heightPixels;
        mScreenWidth = outMetrics.widthPixels;
    }

    public void takePicture(Camera.ShutterCallback mShutterCallback, Camera.PictureCallback rawPictureCallback, Camera.PictureCallback jpegPictureCallback) {
        if (mCamera != null)
            mCamera.takePicture(mShutterCallback, rawPictureCallback, jpegPictureCallback);
    }

    public void startPreview() {
        mCamera.startPreview();
    }

    public void openCamera(){
        if(holder != null){
            if (!checkCameraHardware(getContext()))
                return;
            if (mCamera == null) {
                isOpen = safeCameraOpen(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
            if (!isOpen) {
                return;
            }
            mCamera.setDisplayOrientation(ORIENTATION);
            try {
                mCamera.setPreviewDisplay(holder);
                setCameraParams(mScreenWidth, mScreenHeight);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(EasyPermissions.hasPermissions(this.context, PERMISSION)){
            this.holder = holder;
            openCamera();
        }else{
            this.holder = holder;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            setCameraParams(mScreenWidth, mScreenHeight);
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCameraAndPreview();
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return qOpened;
    }

    public void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    private void setCameraParams(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();

        Camera.Size preSize = CameraSurfaceView.getCloselyPreSize(true, width, height, parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(preSize.width, preSize.height);

        Camera.Size picSize = getCloselyPictureSize();
        parameters.setPictureSize(picSize.width, picSize.height);

        parameters.setJpegQuality(100); // 设置照片质量
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
        }

        mCamera.setDisplayOrientation(90);// 设置PreviewDisplay的方向，效果就是将捕获的画面旋转多少度显示
        mCamera.setParameters(parameters);

    }

    private Camera.Size getCloselyPictureSize(){
        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
        double scale = mScreenHeight/(mScreenWidth*1.0);
        int maxCloseScaleIndex = 0;
        double minDiff = 100;
        for(int i = 0; i < pictureSizes.size(); i++){
            Camera.Size picSize = pictureSizes.get(i);
            double scaleI = picSize.width/(picSize.height*1.0);//获取到的高宽比是反的
            double absDiffI = Math.abs(scaleI - scale);
            if(absDiffI < minDiff || (absDiffI == minDiff
                    && picSize.width > pictureSizes.get(maxCloseScaleIndex).width
                    && picSize.width < 4000)){ //图片太大后面会加载失败
                maxCloseScaleIndex = i;
                minDiff = absDiffI;
            }
        }
        return pictureSizes.get(maxCloseScaleIndex);
    }
    /**
     * 选取合适的分辨率
     */
    private Camera.Size getProperSize(List<Camera.Size> pictureSizeList, float screenRatio) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            float currentRatio = ((float) size.width) / size.height;
            if (currentRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }

        if (null == result) {
            for (Camera.Size size : pictureSizeList) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {// 默认w:h = 4:3
                    result = size;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * 通过对比得到与宽高比最接近的预览尺寸（如果有相同尺寸，优先选择）
     *
     * @param isPortrait 是否竖屏
     * @param surfaceWidth 需要被进行对比的原宽
     * @param surfaceHeight 需要被进行对比的原高
     * @param preSizeList 需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    public static  Camera.Size getCloselyPreSize(boolean isPortrait, int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {
        int reqTmpWidth;
        int reqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        if (isPortrait) {
            reqTmpWidth = surfaceHeight;
            reqTmpHeight = surfaceWidth;
        } else {
            reqTmpWidth = surfaceWidth;
            reqTmpHeight = surfaceHeight;
        }
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for(Camera.Size size : preSizeList){
            if((size.width == reqTmpWidth) && (size.height == reqTmpHeight)){
                return size;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) reqTmpWidth) / reqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

}