package com.example.code.floatingwindowdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCas;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.example.code.floatingwindowdemo.callback.ScreenShotCallBack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Class description
 *
 * @author tuozhaobing
 * @date 2017/11/3
 */
public class ScreenShotHelper {

    public static final String TAG = "ScreenShotHelper";
    private ImageReader mImageReader = null;
    private Handler handler = new Handler();

    private int mWindowWidth;
    private int mWindowHeight;
    /**
     * Application context
     */
    private Context mContext;
    private DisplayMetrics mDisplayMetrics;
    private VirtualDisplay mVirtualDisplay;

    private boolean mNavigationBarEnabled;
    private boolean mNeedSaveFile;
    private String mImagePath,mImageName;
    private ScreenShotCallBack mScreenShotCallBack;

    private static ScreenShotHelper mScreenShotHelper;

    private ScreenShotHelper(Context context) {
        mWindowWidth = Utils.getDeviceWidth(context);
        mWindowHeight = Utils.getAbsoluteDeviceHeight(context);

        mDisplayMetrics = new DisplayMetrics();
        Utils.getScreenMetrics(context, mDisplayMetrics);
        //avoid memory linked
        mContext = context.getApplicationContext();
    }

    public synchronized static ScreenShotHelper getInstance(Context context) {
        if (mScreenShotHelper == null) {
            mScreenShotHelper = new ScreenShotHelper(context);
        }
        return mScreenShotHelper;
    }

    /**
     * @param mediaProjection
     * @param navigationBarEnabled
     * @param screenShotCallBack
     * @param needSaveFile
     * @param path
     * @param name
     */
    public void takeScreenShot(final MediaProjection mediaProjection, boolean navigationBarEnabled, ScreenShotCallBack screenShotCallBack, boolean needSaveFile, String path, String name) {

        if (FloatingWindowApplication.getInstance().getMediaProjection() == null){
            throw new IllegalStateException("MediaProjection Permission have not grated");
        }

        mScreenShotCallBack = screenShotCallBack;
        mNavigationBarEnabled = navigationBarEnabled;
        mNeedSaveFile = needSaveFile;
        mImagePath = path;
        mImageName = name;

        if (mNeedSaveFile && (mImageName == null || mImagePath == null)){
            throw new IllegalStateException("image path and image name must be not null when save tag true");
        }
        //post delay when take screen shot[fix image gray error]
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mImageReader = ImageReader.newInstance(mWindowWidth, mWindowHeight, PixelFormat.RGBA_8888, 1);
                mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), handler);
                mVirtualDisplay = mediaProjection.createVirtualDisplay("capture_screen",
                        mWindowWidth, mWindowHeight, mDisplayMetrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mImageReader.getSurface
                                (), null, null);
            }
        },300);
    }

    class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.e(TAG, "Image Available");
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            //delete soft key height from page if needed
            if (Utils.hasNavBar(mContext) && !mNavigationBarEnabled) {
                Log.e(TAG, "has navigation bar");
                height -= Utils.getNavigationBarHeight(mContext);
            }

            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap
                    .Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();
            if (bitmap == null) {
                Log.e(TAG, "file save failure ");
                mScreenShotCallBack.screenShotError(new IOException("create bitmap failed!"));
            }
            Log.e(TAG, "bitmap  create success ");
            try {
                if (mNeedSaveFile){
                    Utils.saveBitmapFile(mImagePath + mImageName, bitmap, true);
                    mScreenShotCallBack.screenShotSuccess(mImagePath + mImageName);
                }else{
                    mScreenShotCallBack.screenShotSuccess(Utils.bitmap2Bytes(bitmap));
                }
            } catch (IOException e) {
                mScreenShotCallBack.screenShotError(e);
                e.printStackTrace();
            } finally {
                if (mImageReader != null) {
                    mImageReader.close();
                    mImageReader = null;
                }
                if (mVirtualDisplay != null){
                    //must release virtual display,otherwise system will reboot in lenovo tablet
                    mVirtualDisplay.release();
                    mVirtualDisplay = null;
                }
            }
        }
    }
}
