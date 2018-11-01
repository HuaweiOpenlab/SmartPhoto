package com.huawei.hiaidemo.Utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ActionBarOverlayLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.huawei.hiaidemo.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static com.huawei.hiaidemo.Utils.Constant.GALLERY_REQUEST_CODE;
import static com.huawei.hiaidemo.Utils.Constant.IMAGE_CAPTURE_REQUEST_CODE;
import static com.huawei.hiaidemo.Utils.Constant.IMAGE_PREFIX;
import static com.huawei.hiaidemo.Utils.Constant.RESIZED_HEIGHT;
import static com.huawei.hiaidemo.Utils.Constant.RESIZED_WIDTH;

public class MyUtils {
    private static String[] labelArray = null;

    public static String getTop1Result(AssetManager mgr, float[] softmaxOutput) {
        List<Map.Entry<Integer, Float>> topNResult = getMaxTopN(softmaxOutput);

        if(labelArray == null){
            labelArray = getLabelArrays(mgr);
        }

        int index = topNResult.get(0).getKey();

        return labelArray[index];
    }


    public static List<Map.Entry<Integer, Float>> getMaxTopN(float[] softmaxOutput) {
        Map<Integer, Float> map = new TreeMap<Integer, Float>();

        for(int i=0;i<softmaxOutput.length;i++){
            map.put(i, softmaxOutput[i]);
        }

        List<Map.Entry<Integer, Float>> list = new ArrayList<>(map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>() {
            public int compare(Map.Entry<Integer, Float> o1, Map.Entry<Integer, Float> o2) {
                return  o2.getValue().compareTo(o1.getValue());
            }
        }); //重新排序

        return list;
    }

    public static float[] getPixel(Bitmap bitmap, int resizedWidth, int resizedHeight) {
        int channel = 3;
        float[] buff = new float[channel * resizedWidth * resizedHeight];

        int rIndex, gIndex, bIndex;
        for (int i = 0; i < resizedHeight; i++) {
            for (int j = 0; j < resizedWidth; j++) {
                bIndex = i * resizedWidth + j;
                gIndex = bIndex + resizedWidth * resizedHeight;
                rIndex = gIndex + resizedWidth * resizedHeight;

                int color = bitmap.getPixel(j, i);
                //这里不做均值处理，直接以像素值作为输入
                buff[bIndex] = (float) (blue(color));
                buff[gIndex] = (float) (green(color));
                buff[rIndex] = (float) (red(color));

            }
        }

        return buff;
    }


    //保存文件到指定路径
    public static boolean saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SmartPhotoImages";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = IMAGE_PREFIX + System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            //通过io流的方式来压缩保存图片
            boolean isSuccess = bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);//图片质量100，原图输出
            fos.flush();
            fos.close();

            //把文件插入到系统图库
//            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);

            //保存图片后发送广播通知更新数据库
            Uri uri = Uri.fromFile(file);
            //部分手机默认只显示系统照相机拍的照片，这时需要去应用对应的相册去查找图片
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            if (isSuccess) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //从bytes到bitmap
    public static Bitmap getDecodeImageData(byte[] bytes){
        Bitmap decodeBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap rotationBitmap = adjustPhotoRotation(decodeBitmap, 90);//原始拍出的图片需要选择90度
        return rotationBitmap;
    }

    //从bitmap到resizeBitmap再到像素值
    public static float[] getFloatImagePixel(Bitmap decodeImageData) {
        Bitmap rgba =  decodeImageData.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(rgba, RESIZED_WIDTH, RESIZED_HEIGHT, false);
        return getPixel(resizeBitmap, RESIZED_WIDTH, RESIZED_HEIGHT);
    }

    public  static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;

        } catch (OutOfMemoryError ex) {
        }
        return null;

    }


    public static Bitmap addTextWatermark(Context context, Bitmap src, String content) {
        Bitmap warterMark = BitmapFactory.decodeResource(context.getResources(),R.drawable.warter_mark);

        Bitmap includeIcon = createWaterMaskBitmap(src,warterMark, 70,40);

        Paint waterTextPaint = getWaterTextPaint(content);

        Bitmap includeText = drawTextToBitmap(includeIcon,content, waterTextPaint, 220, 128);

        return includeText;
    }


    private static boolean isEmptyBitmap(Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }

    private static Paint getWaterTextPaint(String content){
        int color = Color.WHITE;
        int textSize = 80;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTypeface( Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(content, 0, content.length(), bounds);
        return paint;
    }

    //识别文件编码方式之后再对文件安装识别的编码方式进行读取
    public static String[] getLabelArrays(AssetManager mgr)  {

        BufferedReader reader;
        StringBuilder result = new StringBuilder();
        try {
            InputStream in = mgr.open("labels.txt");
            in.mark(4);
            byte[] first3bytes = new byte[3];
            in.read(first3bytes);//找到文档的前三个字节并自动判断文档类型。
            in.reset();
            if (first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB
                    && first3bytes[2] == (byte) 0xBF) {// utf-8

                reader = new BufferedReader(new InputStreamReader(in, "utf-8"));

            } else if (first3bytes[0] == (byte) 0xFF
                    && first3bytes[1] == (byte) 0xFE) {

                reader = new BufferedReader(
                        new InputStreamReader(in, "unicode"));
            } else if (first3bytes[0] == (byte) 0xFE
                    && first3bytes[1] == (byte) 0xFF) {

                reader = new BufferedReader(new InputStreamReader(in,
                        "utf-16be"));
            } else if (first3bytes[0] == (byte) 0xFF
                    && first3bytes[1] == (byte) 0xFF) {

                reader = new BufferedReader(new InputStreamReader(in,
                        "utf-16le"));
            } else {

                reader = new BufferedReader(new InputStreamReader(in, "GBK"));
            }
            String str = reader.readLine();
            result.append(System.lineSeparator() + str);
            while (str != null) {
                str = reader.readLine();

                result.append(System.lineSeparator() + str);
            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString().trim().split(System.lineSeparator());
    }

    //将图标以水印的方式绘制到图片上
    public static Bitmap createWaterMaskBitmap(Bitmap src, Bitmap watermark,int paddingLeft, int paddingTop) {
        if (src == null) {
            return null;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        //创建一个bitmap
        Bitmap newb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        //将该图片作为画布
        Canvas canvas = new Canvas(newb);
        //在画布 0，0坐标上开始绘制原始图片
        canvas.drawBitmap(src, 0, 0, null);
        //在画布上绘制水印图片
        canvas.drawBitmap(watermark, paddingLeft, paddingTop, null);
        canvas.save();
        canvas.restore();
        return newb;
    }

    //将文字以水印方式绘制到图片上
    private static Bitmap drawTextToBitmap(Bitmap bitmap, String text, Paint paint, int paddingLeft, int paddingTop) {
        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();

        paint.setDither(true); // 获取跟清晰的图像采样
        paint.setFilterBitmap(true);// 过滤一些
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);
        Canvas canvas = new Canvas(bitmap);
        drawTextWaterMark(text, paddingLeft, paddingTop, paint,canvas);
        return bitmap;
    }

    private static void drawTextWaterMark(String str, float x, float y, Paint paint, Canvas canvas) {

        float txtSize = -paint.ascent() + paint.descent();

        if (paint.getStyle() == Paint.Style.FILL_AND_STROKE
                || paint.getStyle() == Paint.Style.STROKE) {
            txtSize += paint.getStrokeWidth(); // add stroke width to the text
        }
        canvas.drawText(str, x, y, paint);
    }


    public static void setFullScreenWindowLayout(Window window) {

        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        //设置页面全屏显示
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        //设置页面延伸到刘海区显示
        window.setAttributes(lp);
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * 裁剪
     *
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
    public static Bitmap cropBitmap(Context context, Bitmap bitmap) {
        int w = bitmap.getWidth(); //得到图片的宽，高
        int h = bitmap.getHeight();
        double ratioBitmap = h/(w*1.0);
        int[] screenMatrix = getScreenMatrix(context);
        double ratioScreen = screenMatrix[1]/(screenMatrix[0]*1.0);
        int x = 0;
        int y = 0;
        int cropWidth = 0;
        int cropHeight = 0;
        if(Math.abs(ratioScreen - ratioBitmap) > 0.1){//有必要进行裁剪
            if(ratioScreen > ratioBitmap){
                cropHeight = h;
                cropWidth = (int)(cropHeight/ratioScreen);
                x = (int)((w - cropWidth)/2);
                y = 0;
            }else{
                cropWidth = w;
                cropHeight = (int)ratioScreen*cropWidth;
                x = 0;
                y = (int)((h - cropHeight)/2);
            }
            return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight, null, false);

        }else{
            return bitmap;
        }

    }

    //获取屏幕分辨率
    public static int[] getScreenMatrix(Context context) {
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        WM.getDefaultDisplay().getMetrics(outMetrics);
        int mScreenWidth = outMetrics.widthPixels;
        int mScreenHeight = outMetrics.heightPixels;
        int[] notchSize = getNotchSize(context);
        return new int[]{mScreenWidth, mScreenHeight + notchSize[1]};//要加上刘海的高度
    }

    //获取刘海尺寸
    public static int[] getNotchSize(Context context) {

        int[] ret = new int[]{0, 0};

        try {

            ClassLoader cl = context.getClassLoader();

            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");

            Method get = HwNotchSizeUtil.getMethod("getNotchSize");

            ret = (int[]) get.invoke(HwNotchSizeUtil);

        } catch (ClassNotFoundException e) {

            Log.e("test", "getNotchSize ClassNotFoundException");

        } catch (NoSuchMethodException e) {

            Log.e("test", "getNotchSize NoSuchMethodException");

        } catch (Exception e) {

            Log.e("test", "getNotchSize Exception");

        } finally {

            return ret;

        }

    }

}
