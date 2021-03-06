package com.bresee.breseefaceapitest;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bresee.breseefaceapitest.camera.ComplexFrameHelper;
import com.bresee.breseefacelib.FaceCompareResult;
import com.bresee.breseefacelib.FaceConfigSetting;
import com.bresee.breseefacelib.FaceDetResult;
import com.bresee.breseefacelib.FaceExtrResult;
import com.bresee.breseefacelib.FaceSDKManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.bresee.breseefaceapitest.camera.CameraManager;
import com.bresee.breseefaceapitest.camera.CameraPreview;
import com.bresee.breseefaceapitest.camera.CameraPreviewData;
import com.bresee.breseefacelib.InputImageParam;
import com.bresee.breseefacelib.VersionInfos;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import static com.bresee.breseefaceapitest.ActivityHelper.NV21_rotate_to_180;
import static com.bresee.breseefaceapitest.ActivityHelper.NV21_rotate_to_270;
import static com.bresee.breseefaceapitest.ActivityHelper.NV21_rotate_to_90;
import static com.bresee.breseefaceapitest.ActivityHelper.saveYuvImage;
import static com.bresee.breseefaceapitest.ConfigParam.onlineLicenseButton;

public class MainActivity extends Activity {
    public final String TAG = "MainActivity";

    private FaceConfigSetting faceConfigSetting;
    private PointerByReference faceDetHandle;
    private PointerByReference faceExtrHandle;
    private InputImageParam inputFdImageParam;
    private InputImageParam inputFrImageParam;
    private FaceDetResult faceDetResult;
    private FaceDetResult faceRegInput;
    private FaceExtrResult faceExtrResult;
    private FaceCompareResult faceCompareResult;

    private CameraPreview cameraView;
    private CameraPreview cameraView_ir;
    private ArrayBlockingQueue<ActivityHelper.RecognizeData> mRecognizeDataQueue;
    private CameraManager manager;
    private CameraManager manager_ir;
    private RecognizeThread mRecognizeThread;
    private DetectThread mDetectThread;

    private String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
    List<String> mPermissionList = new ArrayList<>();
    private static final int REQUEST_ALL_PERMISSION = 11;

    private Handler mHandler;
    private FaceView faceView;
    private TextView resultOut,version;
    private FaceSDKManager faceSDKManager;
    private int FrameSeqNum=0;
    private boolean isBusyRecognise=false;

    //?????????????????????????????????????????????
    private int faceTrackIdSubTmp=0;
    //private int faceGlobalTrackIdTmp=0;
    private int initReturn=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        mRecognizeDataQueue = new ArrayBlockingQueue<>(1);
        mHandler = new Handler();

        if(ConfigParam.liveButton==1&&ConfigParam.liveLevel==1){
            initView();
        }
        else{
            initViewSimple();
        }
        checkPermission();

        sdkAllFlow();
    }
    @Override
    protected void onResume() {
        if(ConfigParam.liveButton==1&&ConfigParam.liveLevel==1) {
            manager_ir.open(getWindowManager(), true, ConfigParam.cameraWidth, ConfigParam.cameraHeight);//??????
            manager.open(getWindowManager(), false, ConfigParam.cameraWidth, ConfigParam.cameraHeight);//?????????
        }else {
            manager.open(getWindowManager(), false, ConfigParam.cameraWidth, ConfigParam.cameraHeight);//?????????
        }
        super.onResume();
    }
    @Override
    protected void onStop() {
        mRecognizeDataQueue.clear();
        if (manager != null) {
            manager.release();
        }
        if (manager_ir != null) {
            manager_ir.release();
        }
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        mRecognizeThread.isInterrupt = true;
        mDetectThread.isInterrupt = true;
        mRecognizeThread.interrupt();
        mDetectThread.interrupt();
        if (manager != null) {
            manager.release();
        }
        if (manager_ir != null) {
            manager_ir.release();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        sdkDestroy();
        super.onDestroy();
    }
    @Override
    protected void onRestart() {
        faceView.clear();
        faceView.invalidate();
        super.onRestart();
    }

    private boolean checkPermission() {
        mPermissionList.clear();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        }
        if (!mPermissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[mPermissionList.size()]), REQUEST_ALL_PERMISSION);
            return false;
        }
        return true;
    }
    public void alertEditForDetect(View view){
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle("???????????????ID")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(et)
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String deleteFaceId=et.getText().toString();
                        sdkDeleteFace(deleteFaceId);
                    }
                }).setNegativeButton("??????",null).show();
    }
    /**
     * ??????SDK???????????????.
     */
    private void sdkAllFlow() {
        if(onlineLicenseButton==1){
            if(sdkOnlineLicense())//???????????????????????????????????????
            {
                sdkVersion();
                sdkLoadLibray();
                sdkInit();

                mRecognizeThread = new RecognizeThread();
                mRecognizeThread.start();
                mDetectThread = new DetectThread();
                mDetectThread.start();
            }
            else{
                Log.e(TAG, "###SDK??????????????????,?????????????????????!!");
            }
        }
        else{//????????????
            sdkVersion();
            sdkLoadLibray();
            sdkInit();

            mRecognizeThread = new RecognizeThread();
            mRecognizeThread.start();
            mDetectThread = new DetectThread();
            mDetectThread.start();
        }
    }
    /**
     * ???????????????.
     */
    private void sdkDeleteFace(String faceInfo) {
        int deleteFaceDbState = faceSDKManager.isfDeleteFaceFeature(ConfigParam.faceLibName, faceInfo,ConfigParam.faceLibFile);
        Log.e(TAG, "###sdkClearLibray down :" + deleteFaceDbState);
        final String dbNameId=faceInfo;
        faceView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, dbNameId+"????????????", Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * ?????????????????????.
     */
    private void initView() {
        //????????????????????????
        final int mCurrentOrientation = getResources().getConfiguration().orientation;
        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, "###screenState-??????");
        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "###screenState-??????");
        }

        //??????????????????
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Log.i(TAG, "###displayMetrics height :" +displayMetrics.heightPixels);
        Log.i(TAG, "###displayMetrics width :" +displayMetrics.widthPixels);

        setContentView(R.layout.activity_main);

        faceSDKManager=new FaceSDKManager();

        /* ??????????????? */
        resultOut = findViewById(R.id.result);
        faceView = (FaceView) this.findViewById(R.id.fcview);
        cameraView = (CameraPreview) findViewById(R.id.preview);
        cameraView_ir = (CameraPreview) findViewById(R.id.preview_ir);
        version= findViewById(R.id.vesion);
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.release();
                final Intent intent = new Intent();
                intent.setClass(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertEditForDetect(v);
            }
        });
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit( 0 );
            }
        });

        manager = new CameraManager();
        manager.setPreviewDisplay(cameraView);

        manager_ir = new CameraManager();
        manager_ir.setPreviewDisplay(cameraView_ir);
        cameraView_ir.setZOrderOnTop(true);

        /* ??????????????????????????? */
        manager.setListener(new CameraManager.CameraListener() {
            @Override
            public void onPictureTaken(CameraPreviewData cameraPreviewData) {
                ComplexFrameHelper.addRgbFrameAndMakeComplex(cameraPreviewData);
            }
        });
        /* ???????????????????????? */
        manager_ir.setListener(new CameraManager.CameraListener() {
            @Override
            public void onPictureTaken(CameraPreviewData cameraPreviewData) {
                ComplexFrameHelper.addIRFrame(cameraPreviewData);
            }
        });
    }
    /**
     * ?????????????????????.
     */
    private void initViewSimple() {
        //????????????????????????
        final int mCurrentOrientation = getResources().getConfiguration().orientation;
        if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, "###screenState-??????");
        } else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "###screenState-??????");
        }

        //??????????????????
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Log.i(TAG, "###displayMetrics height :" +displayMetrics.heightPixels);
        Log.i(TAG, "###displayMetrics width :" +displayMetrics.widthPixels);

        setContentView(R.layout.activity_main);

        faceSDKManager=new FaceSDKManager();

        /* ??????????????? */
        resultOut = findViewById(R.id.result);
        faceView = (FaceView) this.findViewById(R.id.fcview);
        cameraView = (CameraPreview) findViewById(R.id.preview);
        version= findViewById(R.id.vesion);
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.release();
                final Intent intent = new Intent();
                intent.setClass(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertEditForDetect(v);
            }
        });
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit( 0 );
            }
        });

        manager = new CameraManager();
        manager.setPreviewDisplay(cameraView);

        /* ??????????????????????????? */
        manager.setListener(new CameraManager.CameraListener() {
            @Override
            public void onPictureTaken(CameraPreviewData cameraPreviewData) {
                ComplexFrameHelper.addRgbFrame(cameraPreviewData);
            }
        });
    }
    /**
     * SDK????????????.
     */
    private boolean sdkOnlineLicense() {
        //SDK????????????(????????????????????????????????????)
        String userName = "shejinlong@bresee.cn";
        String passWord = "shejinlong!123";
        String licSavePathDir=ConfigParam.workBasePath;
        int codeReturn=faceSDKManager.isfGetLicense(userName,passWord,licSavePathDir);
        Log.e(TAG, "###IsfGetLicense down :" +codeReturn);
        if(codeReturn==0) {
            return true;
        }else{
            return false;
        }
    }
    /**
     * ??????SDK??????.
     */
    private void sdkVersion() {
        VersionInfos versionInfos=new VersionInfos();
        int codeReturn=faceSDKManager.isfGetVersion(versionInfos);
        Log.e(TAG, "###sdkVersion down :" +codeReturn);
        version.setText(versionInfos.version);
    }
    /**
     * ???????????????.
     */
    private void sdkLoadLibray() {
        //???????????????
        String featureLibName=ConfigParam.faceLibName;
        int loadFaceDbState=faceSDKManager.isfLoadFeatureLibrary(featureLibName,ConfigParam.faceLibFile);
        Log.e(TAG, "###sdkLoadLibray :" +loadFaceDbState);
    }
    /**
     * SDK?????????.
     */
    private void sdkInit(){
        /*File workFile=new File(ConfigParam.workBasePath1);
        if(!workFile.exists())
        {
            workFile.mkdir();
        }*/

        faceView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "?????????????????????", Toast.LENGTH_LONG).show();
            }
        });

        faceConfigSetting=new FaceConfigSetting();
        faceConfigSetting.qualityButton=ConfigParam.qualityButton;
        faceConfigSetting.angleButton=ConfigParam.angleButton;
        faceConfigSetting.maskButton=ConfigParam.maskButton;
        faceConfigSetting.livenessButton=ConfigParam.liveButton;
        faceConfigSetting.livenessLevel=ConfigParam.liveLevel;
        faceConfigSetting.extractButton=ConfigParam.extraButton;
        faceConfigSetting.faceLibNmame=ConfigParam.faceLibName;
        faceConfigSetting.fullScreen=ConfigParam.fullScreenButton;
        faceConfigSetting.detectAreaPoints= ConfigParam.screenROI;

        faceDetHandle = new PointerByReference(Pointer.NULL);
        faceExtrHandle =new PointerByReference(Pointer.NULL);
        inputFdImageParam=new InputImageParam();
        inputFrImageParam=new InputImageParam();
        faceDetResult=new FaceDetResult();
        faceRegInput=new FaceDetResult();
        faceExtrResult=new FaceExtrResult();
        faceCompareResult=new FaceCompareResult();

        //SDK?????????
        initReturn=faceSDKManager.isfFaceInit(ConfigParam.workBasePath,faceConfigSetting,faceDetHandle,faceExtrHandle);
        Log.e(TAG, "###IsfFaceInit :" +initReturn);
    }
    /**
     * SDK??????.
     */
    private void sdkDestroy() {
        int codeReturn=faceSDKManager.isfFaceDestroy(faceDetHandle,faceExtrHandle);
        Log.i(TAG, "###IsfDestroy down :" +codeReturn);
    }

    private class DetectThread extends Thread {
        boolean isInterrupt;
        @Override
        public void run() {
            while (!isInterrupt) {
                Pair<CameraPreviewData,CameraPreviewData> cameraPreviewData = null;
                try {
                    cameraPreviewData = ComplexFrameHelper.takeComplexFrame();
                    //saveYuvImage(cameraPreviewData.first.nv21Data,cameraPreviewData.first.width, cameraPreviewData.first.height);//??????NV21??????
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                if (faceSDKManager == null||inputFdImageParam==null) {
                    continue;
                }

                /*??????????????????*/
                if (ConfigParam.cameraFrameRotation == 0) {
                    inputFdImageParam.bgrImgWidht = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrImgHeight = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrImgStride = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrByteData = cameraPreviewData.first.nv21Data;
                    if (null != cameraPreviewData.second) {
                        inputFdImageParam.riBgrByteData = cameraPreviewData.second.nv21Data;
                    } else {
                        inputFdImageParam.riBgrByteData = null;
                    }
                } else if (ConfigParam.cameraFrameRotation == 90) {
                    inputFdImageParam.bgrImgWidht = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrImgHeight = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrImgStride = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrByteData = NV21_rotate_to_90(cameraPreviewData.first.nv21Data, inputFdImageParam.bgrImgHeight, inputFdImageParam.bgrImgWidht);
                    if (null != cameraPreviewData.second) {
                        inputFdImageParam.riBgrByteData = NV21_rotate_to_90(cameraPreviewData.second.nv21Data, inputFdImageParam.bgrImgHeight, inputFdImageParam.bgrImgWidht);
                    } else {
                        inputFdImageParam.riBgrByteData = null;
                    }
                } else if (ConfigParam.cameraFrameRotation == 180) {
                    inputFdImageParam.bgrImgWidht = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrImgHeight = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrImgStride = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrByteData = NV21_rotate_to_180(cameraPreviewData.first.nv21Data, inputFdImageParam.bgrImgWidht, inputFdImageParam.bgrImgHeight);
                    if (null != cameraPreviewData.second) {
                        inputFdImageParam.riBgrByteData = NV21_rotate_to_180(cameraPreviewData.second.nv21Data, inputFdImageParam.bgrImgWidht, inputFdImageParam.bgrImgHeight);
                    } else {
                        inputFdImageParam.riBgrByteData = null;
                    }
                } else if (ConfigParam.cameraFrameRotation == 270) {
                    inputFdImageParam.bgrImgWidht = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrImgHeight = (short) cameraPreviewData.first.width;
                    inputFdImageParam.bgrImgStride = (short) cameraPreviewData.first.height;
                    inputFdImageParam.bgrByteData = NV21_rotate_to_270(cameraPreviewData.first.nv21Data, inputFdImageParam.bgrImgHeight, inputFdImageParam.bgrImgWidht);
                    if (null != cameraPreviewData.second) {
                        inputFdImageParam.riBgrByteData = NV21_rotate_to_270(cameraPreviewData.second.nv21Data, inputFdImageParam.bgrImgHeight, inputFdImageParam.bgrImgWidht);
                    } else {
                        inputFdImageParam.riBgrByteData = null;
                    }
                }
                inputFdImageParam.bgrImgStyle = 1;
                inputFdImageParam.bgrImgSeq = ++FrameSeqNum;

                //saveYuvImage(cameraPreviewData.first.nv21Data,cameraPreviewData.first.width, cameraPreviewData.first.height);//??????NV21??????
                int mDetectState = faceSDKManager.isfFaceDetector(inputFdImageParam, faceDetResult, faceDetHandle);
                //Log.i(TAG, "###isfFaceDetector :" +mDetectState);

                /*????????????*/
                boolean isDetectReport=true;
                if(faceTrackIdSubTmp!=faceDetResult.faceTrackId){
                    isDetectReport=false;
                }
                faceTrackIdSubTmp=faceDetResult.faceTrackId;

                if (mDetectState != 0 || inputFdImageParam.bgrByteData == null || faceDetResult == null ||
                        faceDetResult.detTargetNum == 0) {

                    /* ??????????????? */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faceView.clear();
                            faceView.invalidate();
                        }
                    });
                    showResult(0, "");

                    //license????????????
                    if(initReturn==7){
                        showResult(Color.RED, "??????????????????");
                    }
                } else {
                    if (!isBusyRecognise&&isDetectReport) {
                        //?????????????????????
                        inputFrImageParam.bgrByteData = inputFdImageParam.bgrByteData.clone();
                        if (inputFdImageParam.riBgrByteData != null) {
                            inputFrImageParam.riBgrByteData = inputFdImageParam.riBgrByteData.clone();
                        } else {
                            inputFrImageParam.riBgrByteData = null;
                        }
                        inputFrImageParam.bgrImgHeight = inputFdImageParam.bgrImgHeight;
                        inputFrImageParam.bgrImgWidht = inputFdImageParam.bgrImgWidht;
                        inputFrImageParam.bgrImgStride = inputFdImageParam.bgrImgStride;
                        inputFrImageParam.bgrImgSeq = inputFdImageParam.bgrImgSeq;
                        inputFrImageParam.bgrImgStyle = inputFdImageParam.bgrImgStyle;

                        faceRegInput.faceRectangle = faceDetResult.faceRectangle;
                        faceRegInput.detTargetNum = faceDetResult.detTargetNum;
                        faceRegInput.faceTrackId = faceDetResult.faceTrackId;
                        faceRegInput.faceLandmarks = faceDetResult.faceLandmarks.clone();
                        faceRegInput.landmarkNum = faceDetResult.landmarkNum;
                        faceRegInput.angleScore=faceDetResult.angleScore;
                        faceRegInput.qualityScore=faceDetResult.qualityScore;
                        faceRegInput.extractPattern = 2;

                        ActivityHelper.RecognizeData mRecData = new ActivityHelper.RecognizeData(inputFrImageParam, faceRegInput);
                        mRecognizeDataQueue.offer(mRecData);
                    }

                    /* ?????????????????????????????????????????????*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFaceTrackRect(faceDetResult);
                        }
                    });

                    //??????????????????
                    if (FrameSeqNum % 15 == 0) {
                        showResult(0, "");
                    }
                }
            }
        }
        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }

    private class RecognizeThread extends Thread {
        boolean isInterrupt;
        @Override
        public void run() {
            String comResultOut="";
            int recStateColor=0;
            while (!isInterrupt) {
                try {
                    ActivityHelper.RecognizeData recognizeData = mRecognizeDataQueue.take();
                    isBusyRecognise=true;
                    int faceTrackIdReg=recognizeData.faceRegInputData.faceTrackId;
                    /**??????????????????*/
                    int mRecognuseState = faceSDKManager.isfFaceExtractor(recognizeData.inputFrImageParamData, recognizeData.faceRegInputData, faceExtrResult, faceExtrHandle);
                    Log.i(TAG, "###isfFaceExtractor down :" + mRecognuseState);
                    Log.i(TAG, "###?????????????????? :" + faceExtrResult.maskResult);/**???????????? 0-?????? 1-??????*/

                    if(faceTrackIdSubTmp==faceTrackIdReg) {
                        if ((0 == mRecognuseState) && (1 == ConfigParam.extraButton)) {
                            /**???????????????*/
                            int mCompareState = faceSDKManager.isfGetTopOfFaceLib(ConfigParam.faceLibName, faceExtrResult, faceCompareResult);
                            Log.i(TAG, "###IsfGetTopOfFaceLib down :" + mCompareState);

                            if ((null != faceCompareResult) && (mCompareState == 0)) {
                                if (faceCompareResult.compareSimilarScore > ConfigParam.recogniseThreshold) {
                                    comResultOut = "????????????\n" + new String(faceCompareResult.compareFaceInfo);
                                    recStateColor = Color.GREEN;
                                    Log.i(TAG, "###???????????? :" + faceCompareResult.compareSimilarScore);
                                } else {
                                    recStateColor = Color.RED;
                                    comResultOut = "??????????????????";
                                    Log.e(TAG, "###??????????????? :" + faceCompareResult.compareSimilarScore);
                                }
                            } else {
                                comResultOut = "";
                                Log.e(TAG, "###??????????????? :" + faceCompareResult.compareSimilarScore);
                            }
                        } else if (ConfigParam.maskButton == 2 && mRecognuseState == 1) {
                            recStateColor = Color.RED;
                            comResultOut = "???????????????";
                        } else if (mRecognuseState == 2) {
                            recStateColor = Color.RED;
                            comResultOut = "???????????????";
                        } else if (mRecognuseState == 3) {
                            recStateColor = Color.RED;
                            comResultOut = "???????????????";
                        }else {
                            comResultOut = "";
                        }
                    }else {
                        comResultOut = "";
                    }
                    showResult(recStateColor,comResultOut);
                    isBusyRecognise=false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            isInterrupt = true;
            super.interrupt();
        }
    }

    private void showResult(final int showColor, final String showMsg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                resultOut.setText(showMsg);
                resultOut.setTextColor(showColor);
            }
        });

    }

    private void showFaceTrackRect(FaceDetResult detectResult) {
        faceView.clear();
        Matrix mat = new Matrix();
        int viewWidth = cameraView.getMeasuredWidth();
        int viewHeight = cameraView.getMeasuredHeight();
        int cameraHeight = manager.getCameraheight();
        int cameraWidth = manager.getCameraWidth();
        float left = 0;
        float top = 0;
        float right = 0;
        float bottom = 0;

        if(ConfigParam.cameraPreviewRotation==0&&ConfigParam.cameraPreviewScaleWidht==1.0f&&ConfigParam.cameraPreviewScaleHeight==1.0f){
            mat.postScale((float) viewWidth / (float)cameraWidth , (float) viewHeight / (float) cameraHeight);
            left = cameraWidth - detectResult.faceRectangle.left;
            top = detectResult.faceRectangle.top;
            right = cameraWidth -  detectResult.faceRectangle.right;
            bottom =detectResult.faceRectangle.bottom;
        }
        if(ConfigParam.cameraPreviewRotation==90&&ConfigParam.cameraPreviewScaleWidht==1.0f&&ConfigParam.cameraPreviewScaleHeight==1.0f) {
            mat.postScale((float) viewWidth / (float) cameraHeight, (float) viewHeight / (float) cameraWidth);
            left = detectResult.faceRectangle.left;
            top = detectResult.faceRectangle.top;
            right = detectResult.faceRectangle.right;
            bottom = detectResult.faceRectangle.bottom;
        }
        if(ConfigParam.cameraPreviewRotation==180&&ConfigParam.cameraPreviewScaleWidht==1.0f&&ConfigParam.cameraPreviewScaleHeight==1.0f) {
            mat.postScale((float) viewWidth / (float)cameraWidth , (float) viewHeight / (float) cameraHeight);
            left = detectResult.faceRectangle.right;
            top = viewHeight-detectResult.faceRectangle.bottom;
            right = detectResult.faceRectangle.left;
            bottom =viewHeight-detectResult.faceRectangle.top;
        }
        if(ConfigParam.cameraPreviewRotation==270&&ConfigParam.cameraPreviewScaleWidht==1.0f&&ConfigParam.cameraPreviewScaleHeight==1.0f){
            mat.postScale((float) viewWidth / (float) cameraHeight, (float) viewHeight / (float) cameraWidth);
            left =  detectResult.faceRectangle.right;
            top = detectResult.faceRectangle.bottom;
            right = detectResult.faceRectangle.left;
            bottom = detectResult.faceRectangle.top;
        }
        if(ConfigParam.cameraPreviewRotation==0&&ConfigParam.cameraPreviewScaleWidht==1.6f&&ConfigParam.cameraPreviewScaleHeight==1.0f){
            mat.postScale((float) viewWidth*0.62f / (float)cameraWidth, (float) viewHeight / (float) cameraHeight);
            left = cameraWidth - detectResult.faceRectangle.left;
            top = detectResult.faceRectangle.top;
            right = cameraWidth -  detectResult.faceRectangle.right;
            bottom =detectResult.faceRectangle.bottom;
        }

        RectF drect = new RectF();
        RectF srect = new RectF(left, top, right, bottom);

        mat.mapRect(drect, srect);
        faceView.addRect(drect);
        //faceView.addId(faceIdString.toString());
        faceView.invalidate();
    }
}

