package com.bresee.breseefaceapitest.camera;
import android.util.Pair;
import java.util.concurrent.ArrayBlockingQueue;

public class ComplexFrameHelper {
    //双目用到
    private static ArrayBlockingQueue<Pair<CameraPreviewData, CameraPreviewData>> complexFrameQueue = new ArrayBlockingQueue<>(2);
    private static CameraPreviewData rgbFrameBuffer = null;
    private static CameraPreviewData irFrameBuffer = null;
    private static void makeComplexFrame() {
        if ((rgbFrameBuffer != null) && (irFrameBuffer != null)) {
            if (complexFrameQueue.remainingCapacity() > 0) {
                complexFrameQueue.offer(new Pair(rgbFrameBuffer, irFrameBuffer));
            }
            rgbFrameBuffer = null;
            irFrameBuffer = null;
        }
    }
    public static void addRgbFrameAndMakeComplex(CameraPreviewData rgbFrame) {
        synchronized (ComplexFrameHelper.class) {
            if (rgbFrameBuffer == null) {
                rgbFrameBuffer = rgbFrame;
            }
            makeComplexFrame();
        }
    }
    public static void addIRFrame(CameraPreviewData infraFrame) {
        synchronized (ComplexFrameHelper.class) {
            if (irFrameBuffer == null) {
                irFrameBuffer = infraFrame;
            }
            makeComplexFrame();
        }
    }


    //单目用到
    private static CameraPreviewData rgbSimpleFrameBuffer = null;
    private static void makeSimpleFrame() {
        if (rgbSimpleFrameBuffer != null) {
            if (complexFrameQueue.remainingCapacity() > 0) {
                complexFrameQueue.offer(new Pair(rgbSimpleFrameBuffer, null));
            }
            rgbSimpleFrameBuffer = null;
        }
    }
    public static void addRgbFrame(CameraPreviewData rgbFrame) {
        synchronized (ComplexFrameHelper.class) {
            if(null != rgbFrame)
                rgbSimpleFrameBuffer=rgbFrame;
        }
        makeSimpleFrame();
    }


    public static Pair<CameraPreviewData, CameraPreviewData> takeComplexFrame() throws InterruptedException {
        return complexFrameQueue.take();
    }

}
