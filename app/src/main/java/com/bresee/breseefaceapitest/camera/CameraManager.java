package com.bresee.breseefaceapitest.camera;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.bresee.breseefaceapitest.ConfigParam;

import java.util.List;

public class CameraManager implements CameraPreview.CameraPreviewListener {
    protected boolean front = false;

    protected Camera camera = null;

    protected int cameraId = -1;

    protected SurfaceHolder surfaceHolder = null;

    private CameraListener listener = null;

    private CameraPreview cameraPreview;

    private CameraState state = CameraState.IDEL;

    private int previewDegreen = 0;

    private int manualWidth, manualHeight;

    private Camera.Size previewSize = null;

    private byte[] mPicBuffer;

    public CameraManager() {
        super();
    }


    private boolean isSupportedPreviewSize(int width, int height, Camera mCamera) {
        Camera.Parameters camPara = mCamera.getParameters();
        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
        for (Camera.Size tmpSize : allSupportedSize) {
            if (tmpSize.height == height && tmpSize.width == width)
                return true;
        }
        return false;
    }

    public int getCameraWidth() {
        return manualWidth;
    }

    public int getCameraheight() {
        return manualHeight;
    }

    public boolean open(final WindowManager windowManager) {
        if (state != CameraState.OPENING) {
            state = CameraState.OPENING;
            release();
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... params) {
                    cameraId = front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
                    try {
                        camera = Camera.open(cameraId);
                    } catch (Exception e) {
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        int count = Camera.getNumberOfCameras();
                        if (count > 0) {
                            cameraId = 0;
                            camera = Camera.open(cameraId);
                        } else {
                            cameraId = -1;
                            camera = null;
                        }
                    }
                    if (camera != null) {
                        /**预览角度设置*/
                        int previewRotation =ConfigParam.cameraPreviewRotation;
                        camera.setDisplayOrientation(previewRotation);
                        /**预览尺寸设置*/
                        Camera.Parameters param = camera.getParameters();
                        if (manualHeight > 0 && manualWidth > 0 && isSupportedPreviewSize(manualWidth, manualHeight, camera)) {
                            param.setPreviewSize(manualWidth, manualHeight);

                            /**预览尺寸格式*/
                            param.setPreviewFormat(ImageFormat.NV21);

                            /**配置设置*/
                            camera.setParameters(param);

                            /**配置获取*/
                            Camera.Parameters parameters = camera.getParameters();

                            /**预览尺寸获取*/
                            PixelFormat pixelinfo = new PixelFormat();
                            int pixelformat = camera.getParameters().getPreviewFormat();
                            PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
                            Camera.Size sz = parameters.getPreviewSize();
                            int bufSize = sz.width * sz.height * pixelinfo.bitsPerPixel / 8;
                            if (mPicBuffer == null || mPicBuffer.length != bufSize) {
                                mPicBuffer = new byte[bufSize];
                            }
                            camera.addCallbackBuffer(mPicBuffer);
                            previewSize = sz;
                        }
                        else
                        {
                            Log.e("CameraManager", "###camera Need Config！！！！");
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    cameraPreview.setCamera(camera);
                    state = CameraState.OPENED;
                }
            }.execute();
            return true;
        } else {
            return false;
        }
    }

    public boolean open(WindowManager windowManager, boolean front, int width, int height) {
        if (state == CameraState.OPENING) {
            return false;
        }
        this.manualHeight = height;
        this.manualWidth = width;
        this.front = front;
        return open(windowManager);
    }

    public void release() {
        if (camera != null) {
            this.cameraPreview.setCamera(null);
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    public void finalRelease() {
        this.listener = null;
        this.cameraPreview = null;
        this.surfaceHolder = null;
    }

    public void setPreviewDisplay(CameraPreview preview) {
        this.cameraPreview = preview;
        this.surfaceHolder = preview.getHolder();
        preview.setListener(this);
    }

    public void setListener(CameraListener listener) {
        this.listener = listener;
    }

    @Override
    public void onStartPreview() {
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (listener != null) {
                    listener.onPictureTaken(
                            new CameraPreviewData(data, previewSize.width, previewSize.height,
                                    previewDegreen, front));
                }
                camera.addCallbackBuffer(data);
            }
        });
    }

    public enum CameraState {
        IDEL,
        OPENING,
        OPENED
    }

    public interface CameraListener {
        void onPictureTaken(CameraPreviewData cameraPreviewData);
    }
}
