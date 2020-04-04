package com.example.objecttranslator;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.baidu.translate.ocr.OcrCallback;
import com.baidu.translate.ocr.OcrClient;
import com.baidu.translate.ocr.OcrClientFactory;
import com.baidu.translate.ocr.entity.Language;
import com.baidu.translate.ocr.entity.OcrContent;
import com.baidu.translate.ocr.entity.OcrResult;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class CameraActivity extends AppCompatActivity {
    public static Canvas canvas;
    public static byte[] imageData;
    public static Bitmap imageBitmap;
    public static String srcText = "";
    public static String desText ="";

    //engines
    private AipImageClassify aipImageClassify;
    private OcrClient ocrClient;
    private TransApi transApi;

    private ProgressBar progressBar;
    private ImageButton ocrButton;
    private ImageButton objButton;
    private FloatingActionButton pictureButton;
    private SurfaceView cameraPreView;
    private SurfaceHolder surfaceHolder;
    //Support Low API version
    private Camera camera;
    private ImageView resultImage;
    private int viewWidth, viewHeight;//mSurfaceView的宽和高
    private boolean lock = false;
    public DetectingMode detectingMode = DetectingMode.OCR;

    public enum DetectingMode{
        OCR,
        OBJ
    }
    //private Semaphore lock = new Semaphore(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        InitEngine();
        initView();

    }
    private void InitEngine(){
        ocrClient = OcrClientFactory.create(this,APIKeyChain.OcrId,APIKeyChain.OcrKey);
        aipImageClassify = new AipImageClassify(APIKeyChain.ObjId,APIKeyChain.ObjKey,APIKeyChain.ObjSecret);
        aipImageClassify.setConnectionTimeoutInMillis(2000);
        aipImageClassify.setSocketTimeoutInMillis(60000);
        transApi = new TransApi(APIKeyChain.TransId, APIKeyChain.TransKey);

    }
    private void initView() {
        progressBar = findViewById(R.id.cameraPrograssBar);
        ocrButton = findViewById(R.id.ocrButton);
        objButton = findViewById(R.id.objButton);
        pictureButton = findViewById(R.id.pictureButton);
        resultImage = findViewById(R.id.resultImage);
        cameraPreView = findViewById(R.id.cameraPreView);
        surfaceHolder = cameraPreView.getHolder();
        // 不需要自己的缓冲区
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 添加回调
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
                // 初始化Camera
                initCamera();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
                // 释放Camera资源
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (cameraPreView != null) {
            viewWidth = cameraPreView.getWidth();
            viewHeight = cameraPreView.getHeight();
        }
    }

    /**
     * SurfaceHolder 回调接口方法
     */
    private void initCamera() {
        camera = Camera.open();//默认开启后置
        camera.setDisplayOrientation(90);//摄像头进行旋转90°
        if (camera != null) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewFpsRange(viewWidth, viewHeight);
                //设置相机预览照片帧数
                parameters.setPreviewFpsRange(4, 10);
                //设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);
                //设置图片的质量
                parameters.set("jpeg-quality", 90);
                //设置照片的大小
                parameters.setPictureSize(viewWidth, viewHeight);
                //通过SurfaceView显示预览
                camera.setPreviewDisplay(surfaceHolder);
                //开始预览
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    /**
     * 自动对焦 对焦成功后 就进行拍照
     */
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

            if (success) {//对焦成功

                camera.takePicture(new Camera.ShutterCallback() {//按下快门
                    @Override
                    public void onShutter() {
                        //按下快门瞬间的操作
                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {//是否保存原始图片的信息

                    }
                }, pictureCallback);
            }
        }
    };
    /**
     * 获取图片
     */
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (resource == null) {
                Toast.makeText(CameraActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
            }
            final Matrix matrix = new Matrix();
            matrix.setRotate(90);
            imageBitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
            if (imageBitmap != null && resultImage != null && resultImage.getVisibility() == View.GONE) {
                CameraActivity.this.camera.stopPreview();
                resultImage.setVisibility(View.VISIBLE);
                ShowResult(data,imageBitmap);
                pictureButton.setImageResource(android.R.drawable.ic_menu_revert);
            }
        }
    };
    public void TakePicture(View view) {
        if(lock){
            return;
        }
        else {
            lock = true;
            progressBar.setVisibility(View.VISIBLE);
        }
        if(resultImage.getVisibility() == View.GONE){
            if (camera == null) return;

            //自动对焦后拍照
            camera.autoFocus(autoFocusCallback);
        }
        else {
            pictureButton.setImageResource(android.R.drawable.ic_menu_camera);
            resultImage.setVisibility(View.GONE);
            camera.startPreview();
            lock = false;
            progressBar.setVisibility(View.GONE);
        }

    }
    public void ShowResult(byte[] data,Bitmap bitmap){

        canvas = new Canvas(bitmap);
        if(detectingMode==DetectingMode.OCR){
            ShowOcrResult(bitmap);
        }
        else if(detectingMode==DetectingMode.OBJ){
            ShowObjResult(data,bitmap);
        }

    }
    public void ShowOcrResult(final Bitmap bitmap){

        ocrClient.getOcrResult(Options.srcLang, Options.desLang, bitmap, new OcrCallback() {
            @Override
            public void onOcrResult(OcrResult ocrResult) {
                // 该回调已切换至主线程，可直接更新UI
                if(ocrResult.getError()!=0){
                    lock=false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "Error Code:"+ocrResult.getError(), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(ocrResult==null){
                    lock = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "ocrResult is null", Toast.LENGTH_SHORT).show();
                    return;
                }
                srcText = ocrResult.getSumSrc();
                desText = ocrResult.getSumDst();
                if(ocrResult.getContents()==null||ocrResult.getContents().size()<1){
                    lock = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CameraActivity.this, "getContents is null", Toast.LENGTH_SHORT).show();
                    return;
                }
                Paint textPaint = new Paint();
                Paint rectPaint = new Paint();
                Rect rect;
                List<OcrContent> contents =  ocrResult.getContents();
                for (OcrContent ocrContent: contents){
                    rect = ocrContent.getRect();
                    textPaint.setColor(Color.BLACK);
                    float textHeight = (float)rect.height()/(0.5f+ocrContent.getLineCount());
                    textPaint.setTextSize(textHeight);
                    rectPaint.setStyle(Paint.Style.FILL);
                    rectPaint.setColor(Color.WHITE);
                    canvas.drawRect(rect,rectPaint);
                    canvas.drawText(ocrContent.getDst(),rect.left,rect.top+textHeight,textPaint);
                }
                resultImage.setImageBitmap(bitmap);
                lock = false;
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    public void ShowObjResult(byte[] data,final Bitmap bitmap){
        CameraActivity.imageData = data;
        if (data==null||data.length<1) Toast.makeText(CameraActivity.this, "nulldata", Toast.LENGTH_SHORT).show();
        new Thread(objTask).start();

    }
    @SuppressLint("HandlerLeak")
    Handler objHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            Paint textPaint = new Paint();
            Paint rectPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            float textHeight = 40f;
            textPaint.setTextSize(textHeight);
            rectPaint.setStyle(Paint.Style.STROKE);

            rectPaint.setColor(Color.WHITE);


            String val = data.getString("detect");
            try {

                JSONObject detectJson = new JSONObject(val);
                Log.i("mylog", "请求结果为-->" + val);
                JSONObject resultJson = detectJson.getJSONObject("result");
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                int jpgLeft = resultJson.getInt("left");
                int jpgTop = resultJson.getInt("top");
                int jpgWidth = resultJson.getInt("width");
                int jpgHeight = resultJson.getInt("height");
                int left = canvasWidth-jpgTop-jpgHeight;
                int right = canvasWidth-jpgTop;
                int top = jpgLeft;
                int bottom = jpgLeft+jpgWidth;
                Rect rect = new Rect(left,top,right,bottom);

                canvas.drawRect(rect,rectPaint);

                String keyword = data.getString("keyword");
                String root = data.getString("root");
                //can not be null,because of HTTPPOST
                JSONObject enKeywordResult = new JSONObject(data.getString("enkeyword"));
                JSONObject enRootResult = new JSONObject(data.getString("enroot"));
                //trans UTF8 to gbk
                String enKeyword = new String(enKeywordResult.getJSONArray("trans_result").getJSONObject(0).getString("dst").getBytes());
                String enRoot = new String(enRootResult.getJSONArray("trans_result").getJSONObject(0).getString("dst").getBytes());
                switch (Options.desLang){
                    case Language.ZH:
                        srcText = enKeyword+"\n"+enRoot;
                        desText = keyword +"\n"+root;
                        break;
                    case Language.EN:
                        desText = enKeyword+"\n"+enRoot;
                        srcText = keyword +"\n"+root;
                        keyword = enKeyword;
                        root = enRoot;
                        break;
                    default:
                        break;
                }
                canvas.drawText(keyword,rect.left,rect.top+textHeight,textPaint);
                canvas.drawText(root,rect.left,rect.top+textHeight*2,textPaint);


            } catch (Exception e) {
                e.printStackTrace();
                lock= false;
                progressBar.setVisibility(View.GONE);
                return;
            }
            resultImage.setImageBitmap(imageBitmap);
            // TODO
            lock= false;
            progressBar.setVisibility(View.GONE);
        }
    };

    Runnable objTask = new Runnable() {
        @Override
        public void run() {
            // TODO
            Message msg = new Message();
            Bundle data = new Bundle();
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("with_face", "0");
            JSONObject res = aipImageClassify.objectDetect(imageData,options);
            data.putString("detect", res.toString());
            options.clear();
            options.put("baike_num", "0");
            res = aipImageClassify.advancedGeneral(imageData,options);
            JSONObject resultJson = null;
            try {
                resultJson = res.getJSONArray("result").getJSONObject(0);
                String keyword = resultJson.getString("keyword");
                String root = resultJson.getString("root");
                String enKeywordResult = transApi.getTransResult(keyword,Language.ZH, Language.EN);
                String enRootResult = transApi.getTransResult(root,Language.ZH, Language.EN);
                data.putString("keyword",keyword);
                data.putString("enkeyword",enKeywordResult);
                data.putString("root",root);
                data.putString("enroot",enRootResult);
            } catch (Exception e) {
                e.printStackTrace();
            }

            msg.setData(data);
            objHandler.sendMessage(msg);
        }
    };

    public void ChangeToOcrMode(View view) {
        if(lock){
            return;
        }
        view.setVisibility(View.GONE);
        ocrButton.setVisibility(View.VISIBLE);
        detectingMode = DetectingMode.OCR;
        Toast.makeText(CameraActivity.this, "Changed to OCR mode", Toast.LENGTH_SHORT).show();
    }

    public void ChangeToObjMode(View view) {
        if(lock){
            return;
        }
        view.setVisibility(View.GONE);
        objButton.setVisibility(View.VISIBLE);
        detectingMode = DetectingMode.OBJ;
        Toast.makeText(CameraActivity.this, "Changed to Object mode", Toast.LENGTH_SHORT).show();
    }
    public void LookTranslation(View view) {
        Intent intent = new Intent();
        intent.setClass(this,ResultActivity.class);
        intent.putExtra("des",desText);
        intent.putExtra("src",srcText);
        startActivity(intent);
    }
}
