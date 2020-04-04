package com.example.objecttranslator;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.translate.ocr.entity.Language;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {
    private TextView srcTextView = null;
    private TextView desTextView = null;
    private Spinner srcLangSpinner = null;
    private Spinner desLangSpinner = null;
    private ProgressBar progressBar = null;
    private TextToSpeech tts = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        InitializeUI();
    }
    private void InitializeUI(){
        srcLangSpinner = findViewById(R.id.srcLangSpinner);
        desLangSpinner = findViewById(R.id.desLangSpinner);
        srcTextView = findViewById(R.id.srcTextView);
        desTextView = findViewById(R.id.desTextView);
        Intent intent = getIntent();
        desTextView.setText(intent.getStringExtra("des"));
        srcTextView.setText(intent.getStringExtra("src"));
        progressBar = findViewById(R.id.progressBar);
        String[] langs = {"Chinese","English"};
        ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,langs);
        ArrayAdapter<String> desAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,langs);
        srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        desAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        srcLangSpinner.setAdapter(srcAdapter);
        desLangSpinner.setAdapter(desAdapter);
        switch (Options.srcLang){
            case Language.ZH:
                srcLangSpinner.setSelection(0);
                break;
            case Language.EN:
                srcLangSpinner.setSelection(1);
                break;
            default:
                break;
        }
        switch (Options.desLang){
            case Language.ZH:
                desLangSpinner.setSelection(0);
                break;
            case Language.EN:
                desLangSpinner.setSelection(1);
                break;
            default:
                break;
        }
        srcLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i==0&&Options.srcLang==Language.ZH||i==1&&Options.srcLang==Language.EN){

                }
                else {
                    ExchangeText();
                }
                switch (i){
                    case 0:
                        Options.srcLang = Language.ZH;
                        Options.desLang = Language.EN;
                        desLangSpinner.setSelection(1);
                        break;
                    case 1:
                        Options.srcLang = Language.EN;
                        Options.desLang = Language.ZH;
                        desLangSpinner.setSelection(0);
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        desLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i==0&&Options.desLang==Language.ZH||i==1&&Options.desLang==Language.EN){

                }
                else {
                    ExchangeText();
                }
                switch (i){
                    case 0:
                        Options.srcLang = Language.EN;
                        Options.desLang = Language.ZH;
                        srcLangSpinner.setSelection(1);
                        break;
                    case 1:
                        Options.srcLang = Language.ZH;
                        Options.desLang = Language.EN;
                        srcLangSpinner.setSelection(0);
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    public void ExchangeText(){
        String temp = srcTextView.getText().toString();
        srcTextView.setText(desTextView.getText());
        desTextView.setText(temp);
    }
    public void BackToCamera(View view) {
        if(tts!=null){
            tts.shutdown();
            tts.stop();
        }
        finish();
    }

    public void PronunceDes(View view) {
        progressBar.setVisibility(View.VISIBLE);
        if(tts!=null){
            tts.shutdown();
        }

        tts = new TextToSpeech(this,new SpeechListener());
    }

    private class SpeechListener implements TextToSpeech.OnInitListener {

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result;
                //设置播放语言
                switch (Options.desLang) {
                    case Language.ZH:
                        result=tts.setLanguage(Locale.CHINESE);
                        break;
                    case Language.EN:
                        result=tts.setLanguage(Locale.ENGLISH);
                        break;
                    default:
                        return;
                }

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(ResultActivity.this, "Not supported", Toast.LENGTH_SHORT).show();
                } else if (result == TextToSpeech.LANG_AVAILABLE) {
                    //初始化成功之后才可以播放文字
                    //否则会提示“speak failed: not bound to tts engine
                    //TextToSpeech.QUEUE_ADD会将加入队列的待播报文字按顺序播放
                    //TextToSpeech.QUEUE_FLUSH会替换原有文字
                    tts.speak(desTextView.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                    progressBar.setVisibility(View.GONE);
                }

            } else {
                Log.e("TAG", "初始化失败");
            }

        }

        public void stopTTS() {
            if (tts != null) {
                tts.shutdown();
                tts.stop();
                tts = null;
            }
        }
    }
}
