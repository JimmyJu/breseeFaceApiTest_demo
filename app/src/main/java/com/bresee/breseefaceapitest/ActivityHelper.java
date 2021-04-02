package com.bresee.breseefaceapitest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Environment;
import android.widget.Toast;

import com.bresee.breseefacelib.FaceDetResult;
import com.bresee.breseefacelib.InputImageParam;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityHelper {
    /**
     * 识别输入数据类.
     */
    public static class RecognizeData {
        public FaceDetResult faceRegInputData;
        public InputImageParam inputFrImageParamData;
        public RecognizeData(InputImageParam inputFrImageParamData,FaceDetResult faceRegInputData) {
            this.inputFrImageParamData=inputFrImageParamData;
            this.faceRegInputData=faceRegInputData;
        }
    }
    /**
     * byte转float.
     */
    public static float[] asFloatArray(byte[] input){
        if(null == input ){
            return null;
        }
        FloatBuffer buffer = ByteBuffer.wrap(input).asFloatBuffer();
        float[] res = new float[buffer.remaining()];
        buffer.get(res);
        return res;
    }
    /**
     * 保存Bitmap为JPG图片.
     */
    public static void saveYuvImage(byte[] bytes,int width,int height) {
        @SuppressLint("SdCardPath") File appDir = new File("/sdcard/bresee/", "BreseePicture");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        YuvImage image = new YuvImage(bytes, ImageFormat.NV21, width, height, null);//ImageFormat.NV21  640 480
        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()),
                70, outputSteam); // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
        byte[] jpegData = outputSteam.toByteArray();          //从outputSteam得到byte数据

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bmp = BitmapFactory.decodeByteArray(jpegData, 0,jpegData.length, options);

        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 保存Bitmap为JPG图片.
     */
    public static void saveBitmap(Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "BreseePicture");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * NV21旋转270.
     */
    public static byte[] NV21_rotate_to_270(byte[] nv21_data, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        byte[] nv21_rotated = new byte[buffser_size];
        int i = 0;

        // Rotate the Y luma
        for (int x = width - 1; x >= 0; x--)
        {
            int offset = 0;
            for (int y = 0; y < height; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }

        // Rotate the U and V color components
        i = y_size;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i++;
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset += width;
            }
        }
        return nv21_rotated;
    }
    /**
     * NV21旋转180.
     */
    public static byte[] NV21_rotate_to_180(byte[] nv21_data, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        byte[] nv21_rotated = new byte[buffser_size];
        int i = 0;
        int count = 0;


        for (i = y_size - 1; i >= 0; i--)
        {
            nv21_rotated[count] = nv21_data[i];
            count++;
        }


        for (i = buffser_size - 1; i >= y_size; i -= 2)
        {
            nv21_rotated[count++] = nv21_data[i - 1];
            nv21_rotated[count++] = nv21_data[i];
        }
        return nv21_rotated;
    }
    /**
     * NV21旋转90.
     */
    public static byte[] NV21_rotate_to_90(byte[] nv21_data, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        byte[] nv21_rotated = new byte[buffser_size];
        // Rotate the Y luma

        int i = 0;
        int startPos = (height - 1)*width;
        for (int x = 0; x < width; x++)
        {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }

        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    /**
     * 加载文件路径获取 Bitmap.
     */
    public static Bitmap getBmpFromFilePath(String filePath) {
        String filePathSuffix=filePath.substring(filePath.lastIndexOf("."));

        boolean getBmpTrue=filePathSuffix.equals(".jpg")||filePathSuffix.equals(".png")||filePathSuffix.equals(".bmp");
        if(getBmpTrue) {
            try {
                Bitmap bmpOut = BitmapFactory.decodeFile(filePath);
                int bmpLenth = bmpOut.getByteCount();
                int bmpWidth = bmpOut.getWidth();
                int bmpHeight = bmpOut.getHeight();
                int malloc = 1920 * 1080 * 3;//如果大于1920*1080，就缩放

                if (bmpLenth > malloc) {
                    float rate1 = 1920.0f / (bmpWidth > bmpHeight ? bmpWidth : bmpHeight);
                    float rate2 = 1080.0f / (bmpWidth < bmpHeight ? bmpWidth : bmpHeight);
                    float rate = rate1 < rate2 ? rate1 : rate2;
                    int vWidth = Math.round(rate * bmpWidth);
                    int vHeight = Math.round(rate * bmpHeight);
                    bmpOut = Bitmap.createScaledBitmap(bmpOut, vWidth, vHeight, true);
                }
                return bmpOut;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     * 转换 Bitmap 到 BGR.
     */
    public static byte[] getBGRByteFromBitmap(Bitmap image) {

        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);

        byte[] temp = buffer.array();

        byte[] pixels = new byte[(temp.length / 4) * 3]; // Allocate for BGR

        // Copy pixels into place
        for (int i = 0; i < temp.length / 4; i++) {

            pixels[i * 3] = temp[i * 4 + 2];        //B
            pixels[i * 3 + 1] = temp[i * 4 + 1];    //G
            pixels[i * 3 + 2] = temp[i * 4];       //R
        }

        return pixels;
    }
    /**
     * 转换 Bitmap 到 RGB.
     */
    public static byte[] getRGBByteFromBitmap(Bitmap image) {

        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);

        byte[] temp = buffer.array();

        byte[] pixels = new byte[(temp.length / 4) * 3]; // Allocate for BGR

        // Copy pixels into place
        for (int i = 0; i < temp.length / 4; i++) {

            pixels[i * 3] =  temp[i * 4];       //R
            pixels[i * 3 + 1] = temp[i * 4 + 1];    //G
            pixels[i * 3 + 2] =temp[i * 4 + 2];        //B
        }

        return pixels;
    }
    /**
     * 加载文件路径获取 BGR.
     */
    public static byte[] getBGRByteFromPath(String filePath) {
        try {
            Bitmap image= BitmapFactory.decodeFile(filePath);
            int imageLenght = image.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(imageLenght);
            image.copyPixelsToBuffer(buffer);
            byte[] temp = buffer.array();
            byte[] pixels = new byte[(temp.length / 4) * 3];

            for (int i = 0; i < temp.length / 4; i++) {

                pixels[i * 3] = temp[i * 4 + 2];        //B
                pixels[i * 3 + 1] = temp[i * 4 + 1];    //G
                pixels[i * 3 + 2] = temp[i * 4];        //R
            }

            return pixels;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 获取文件夹下所有文件的名称.
     */
    public static int getAllFileName(String path, ArrayList<String> fileNameList) {
        File file = new File(path);

        if (file.isDirectory()) {
            if (file.exists()) {
                File[] tempList = file.listFiles();
                for (File file1 : tempList) {
                    if (file1.isFile()) {
                        fileNameList.add(file1.getName());
                    }
                    if (file1.isDirectory()) {
                        getAllFileName(file1.getAbsolutePath(), fileNameList);
                    }
                }
                return tempList.length;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            return 0;
        }
    }
    /**
     * 获取系统时间.
     */
    public static String currentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    /**使用FileOutputStream来写入txt文件
     * txtPath txt文件路径
     * content 需要写入的文本
     */
    public static void writeTxt(String txtPath,String content){
        File file = new File(txtPath);
        try {
            if(file.exists()){
                //判断文件是否存在，如果不存在就新建一个txt
                file.createNewFile();
            }
            BufferedWriter output = new BufferedWriter(new FileWriter(file,true));
            output.write(content);
            output.write("\r\n");//换行
            output.flush();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
