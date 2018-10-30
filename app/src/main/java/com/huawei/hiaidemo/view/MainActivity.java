package com.huawei.hiaidemo.view;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huawei.hiaidemo.Camera.CameraSurfaceView;
import com.huawei.hiaidemo.ModelSDK.MobileNetModel;
import com.huawei.hiaidemo.R;
import com.huawei.hiaidemo.Utils.MyUtils;

import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

import static com.huawei.hiaidemo.Utils.Constant.CAMERA_AND_STORAGE;
import static com.huawei.hiaidemo.Utils.Constant.CODE_SELECT_IMAGE;
import static com.huawei.hiaidemo.Utils.Constant.IMAGE_PREFIX;
import static com.huawei.hiaidemo.Utils.Constant.PERMISSION;
import static com.huawei.hiaidemo.Utils.MyUtils.getTop1Result;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private float[] softmax;
    private float[] floatImagePixel;
    private AssetManager mgr;
    private CameraSurfaceView mCameraSurfaceView;
    ImageView img_take_photo;
    ImageView img_photo_album;
    RelativeLayout image_obtained_view;
    RelativeLayout camera_view;
    RelativeLayout layout_show_image;
    TextView quit_btn;
    TextView ok_btn;
    Bitmap addWaterTextBitmap;
    private boolean isClick = true;
    private View show_image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        mgr = getResources().getAssets();
        //Todo: step1==> load model
        int result = MobileNetModel.load(mgr);

    }

    private void initView() {
        //全屏
        MyUtils.setFullScreenWindowLayout(getWindow());

        setContentView(R.layout.activity_main);

        img_take_photo = findViewById(R.id.img_take_photo);

        camera_view = findViewById(R.id.camera_view);

        layout_show_image = findViewById(R.id.layout_show_image);
        layout_show_image.setVisibility(View.INVISIBLE);

        image_obtained_view = findViewById(R.id.image_obtained_view);
        image_obtained_view.setVisibility(View.INVISIBLE);

        mCameraSurfaceView = findViewById(R.id.sv_camera);

        show_image_view = findViewById(R.id.show_image_view);

        quit_btn = findViewById(R.id.quit_btn);
        ok_btn = findViewById(R.id.ok_btn);
        img_photo_album = findViewById(R.id.img_photo_album);

        img_take_photo.setOnClickListener(this);
        quit_btn.setOnClickListener(this);
        ok_btn.setOnClickListener(this);
        img_photo_album.setOnClickListener(this);
        show_image_view.setOnClickListener(this);

        EasyPermissions.requestPermissions(this, "需要获取拍照和储存权限",CAMERA_AND_STORAGE, PERMISSION);


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.img_take_photo:
                if(EasyPermissions.hasPermissions(this, PERMISSION)){
                    if (isClick) {
                        isClick = false;
                        mCameraSurfaceView.takePicture(mShutterCallback, rawPictureCallback, jpegPictureCallback);
                    }
                }else{
                    EasyPermissions.requestPermissions(this, "需要获取拍照和储存权限",CAMERA_AND_STORAGE, PERMISSION);
                }
                break;
            case R.id.quit_btn:
                image_obtained_view.setVisibility(View.INVISIBLE);
                mCameraSurfaceView.startPreview();
                break;
            case R.id.ok_btn:
                image_obtained_view.setVisibility(View.INVISIBLE);
                MyUtils.saveImageToGallery(this, addWaterTextBitmap);
                mCameraSurfaceView.startPreview();
                break;
            case R.id.img_photo_album:
                openSystemAlbum();
                break;
            case R.id.show_image_view:
                layout_show_image.setVisibility(View.INVISIBLE);
                mCameraSurfaceView.startPreview();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCameraSurfaceView.openCamera();
    }

    private void openSystemAlbum() {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        //根据版本号不同使用不同的Action
        if (Build.VERSION.SDK_INT <19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        }else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        startActivityForResult(intent, CODE_SELECT_IMAGE);
    }

    //拍照完成之后返回压缩数据的回调
    private Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            isClick = true;
            Bitmap decodeImageBitmap = MyUtils.getDecodeImageData(data);

            String result = getPredictResult(decodeImageBitmap);

            showImageObtainedView(decodeImageBitmap, result);

        }
    };

    //调用AI模型对图片进行预测
    private String getPredictResult(Bitmap decodeImageBitmap){
        //将byte[]图像数据转Bitmap，然后转为浮点型的输入数据进行预测
        floatImagePixel = MyUtils.getFloatImagePixel(decodeImageBitmap);

        //Todo: step2 ==> pass image data and run model
        softmax = MobileNetModel.predict(floatImagePixel);

        //根据softmax结果，获取概率最大的值对应的label标签名称
        return getTop1Result(mgr, softmax);
    }

    private void showImageObtainedView(Bitmap decodeImageBitmap, String result) {
        //获取水印图片
        addWaterTextBitmap = MyUtils.addTextWatermark(this, decodeImageBitmap, result);

        image_obtained_view.setBackground(new BitmapDrawable(getResources(), addWaterTextBitmap));
        image_obtained_view.bringToFront();
        image_obtained_view.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CODE_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    ContentResolver resolver = this.getContentResolver();
                    Uri originalUri = data.getData();
                    String filePath = MyUtils.getFilePathByUri(this, originalUri);
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(resolver, originalUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(bitmap != null){
                        if(filePath.contains(IMAGE_PREFIX)){
                            show_image_view.setBackground(new BitmapDrawable(getResources(), bitmap));
                            layout_show_image.bringToFront();
                            layout_show_image.setVisibility(View.VISIBLE);
                        }else{
                            //没有被打过标签的情况下
                            Bitmap cropBitmp = MyUtils.cropBitmap(this, bitmap);

                            String result = getPredictResult(cropBitmp);

                            showImageObtainedView(cropBitmp, result);
                        }

                    }

                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Todo: step3==> unload model
        int result = MobileNetModel.unload();

    }


    //拍照快门的回调
    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };

    //拍照完成之后返回原始数据的回调
    private Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };

}
