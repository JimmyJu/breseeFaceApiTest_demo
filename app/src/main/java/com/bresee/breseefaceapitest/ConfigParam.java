package com.bresee.breseefaceapitest;

import android.annotation.SuppressLint;
import android.graphics.Rect;

public class ConfigParam {
    public static final int extraButton = 1;                             /**特征提取开关*/
    public static final int qualityButton = 0;                           /**质量开关*/
    public static final int angleButton = 1;                             /**角度开关*/
    public static final int maskButton = 0;                              /**口罩属性开关*/
    public static final int liveButton = 0;                              /**活体开关*/
    public static final int liveLevel = 1;                               /**活体等级*/
    public static final int onlineLicenseButton = 1;                     /**在线授权开关*/
    public static final int fullScreenButton = 0;                        /**全屏开关，当全屏关闭，与检测区域screenROI配合使用*/
    public static final Rect screenROI= new Rect(80,
                    130,380,500);                    					 /**检测区域，根据实际显示自定义设置*/
    public static final float recogniseThreshold = 0.80f;                /**识别阈值*/
    public static final int qualityThreshold = 30;                       /**质量阈值*/
    public static final int angleThreshold = 40;                         /**角度阈值*/
    public static final int cameraPreviewRotation = 90;                  /**相机预览角度设置，根据实际显示设置，0、90、180、270*/
    public static final int cameraFrameRotation = 90;                    /**捕获帧角度设置，根据实际显示设置，0、90、180、270*/
    public static final float cameraPreviewScaleWidht = 1.0f;            /**相机预览宽缩放设置*/
    public static final float cameraPreviewScaleHeight = 1.0f;           /**相机预览高缩放设置*/
    public static final int cameraWidth =  640;                          /**捕获帧宽设置*/
    public static final int cameraHeight = 480;                          /**捕获帧高设置*/
    public static final String faceLibName = "firstFaceLib";
    @SuppressLint("SdCardPath")
    public static final String workBasePath = "/sdcard/bresee/model/";   /**模型及license存放路径*/
    @SuppressLint("SdCardPath")
    public static final String faceLibFile="/sdcard/bresee/facedb.db";   /**人脸特征库文件路径*/
    @SuppressLint("SdCardPath")
    public static final String registerImgPath="/sdcard/bresee/jpg/";    /**图片注册路径，演示库容：5人*/
}
