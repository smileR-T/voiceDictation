package com.example.xunfeitest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.speech.util.FucUtil;
import com.iflytek.speech.util.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private EditText mResultText;
    private Button languageText, dialogButton;
    // 语言类型【默认中文】
    private String language = "zh_cn";
    // 格式类型【默认json】
    private String resultType = "json";
    private boolean cyclic = false;//音频流识别是否循环调用
    //拼接字符串
    private StringBuffer buffer = new StringBuffer();
    //Handler码
    private int handlerCode = 0x123;
    // 函数调用返回值
    private int resultCode = 0;

    public static String appid = "b008d696";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpeechUtility.createUtility(this, SpeechConstant.APPID +"="+appid);
        //初始化控件
        findViewById(R.id.iat_recognize).setOnClickListener(this);
        findViewById(R.id.iat_recognize_stream).setOnClickListener(this);
        findViewById(R.id.iat_stop).setOnClickListener(this);
        findViewById(R.id.iat_cancel).setOnClickListener(this);
        mResultText = this.findViewById(R.id.iat_text);
//        languageText = this.findViewById(R.id.languageText);
//        dialogButton = this.findViewById(R.id.dialogButton);
//        languageText.setOnClickListener(this);
//        dialogButton.setOnClickListener(this);

        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
    }

    @Override
    public void onClick(View view) {
        if (null == mIat) {
            // 创建单例失败，与 21001 错误为同样原因，
            // 参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            showToast("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        switch (view.getResources().getResourceEntryName(view.getId())) {
            // 1111111111111开始听写
            // 如何判断一次听写结束：OnResult isLast=true 或者 onError
            case "iat_recognize":
                buffer.setLength(0);//长度清空
                mResultText.setText(null);// 清空显示内容
                mIatResults.clear();//清除存贮结果
                // 设置参数
                setParam();
                resultCode = mIat.startListening(mRecognizerListener);
                if (resultCode != ErrorCode.SUCCESS) {
                    showToast("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                } else {
                    showToast("开始听写");
                }
                break;
            // 音频流识别
            case "iat_recognize_stream":
                executeStream();
                break;
//            case R.id.languageText:
//                if (languageType) {
//                    languageType = false;
//                    language = "zh_cn";
//                    languageText.setText("点击切换语种：中文");
//                } else {
//                    languageType = true;
//                    language = "en_us";
//                    languageText.setText("点击切换语种：英文");
//                }
//                mIat.setParameter(SpeechConstant.LANGUAGE, language);
//                break;
            // 停止听写
            case "iat_stop":
                mIat.stopListening();
                showToast("停止听写");
                break;
            // 取消听写
            case "iat_cancel":
                mIat.cancel();
                showToast("取消听写");
                break;
//            //默认显示弹框
//            case "dialogButton":
//                if (dialogType == 0) {
//                    dialogType = 1;
//                    dialogButton.setText("不显示讯飞弹框");
//                } else if (dialogType == 1) {
//                    dialogType = 2;
//                    dialogButton.setText("显示自定义弹框");
//                } else if (dialogType == 2) {
//                    dialogButton.setText("显示讯飞弹框");
//                    dialogType = 0;
//                }
//                break;
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.e(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showToast("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showToast("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            showToast(error.getPlainDescription(true));
            if (null != dialog) {
                dialog.dismiss();
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showToast("结束说话");
            if (null != dialog) {
                dialog.dismiss();
            }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.e(TAG, "onResult: " + results.getResultString());
            if (resultType.equals(resultType)) {
                printResult(results);
            } else if (resultType.equals("plain")) {
                buffer.append(results.getResultString());
                mResultText.setText(buffer.toString());
                mResultText.setSelection(mResultText.length());
            }
            if (isLast & cyclic) {
                // TODO 最后的结果
                Message message = Message.obtain();
                message.what = handlerCode;
                handler.sendMessageDelayed(message, 100);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            //showToast("当前正在说话，音量大小：" + volume);
            Log.e(TAG, "onVolumeChanged: " + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //    String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //    Log.d(TAG, "session id =" + sid);
            // }
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == handlerCode) {
                executeStream();
            }
        }
    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        /**
         * 识别回调成功
         */
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showToast(error.getPlainDescription(true));
        }
    };

    /**
     * 打印听写结果
     */
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        mResultText.setText(resultBuffer.toString());
        mResultText.setSelection(mResultText.length());
    }

    /**
     * 听写参数设置
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎类型
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式【目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容】
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        //目前Android SDK支持zh_cn：中文、en_us：英文、ja_jp：日语、ko_kr：韩语、ru-ru：俄语、fr_fr：法语、es_es：西班牙语、
        // 注：小语种若未授权无法使用会报错11200，可到控制台-语音听写（流式版）-方言/语种处添加试用或购买。
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        // 设置语言区域、当前仅在LANGUAGE为简体中文时，支持方言选择，其他语言区域时，可把此参数值设为mandarin。
        // 默认值：mandarin，其他方言参数可在控制台方言一栏查看。
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        //获取当前语言（同理set对应get方法）
        Log.e(TAG, "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");
        //开始录入音频后，音频后面部分最长静音时长，取值范围[0,10000ms]，默认值5000ms
        mIat.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音取值范围[0,10000ms]，默认值1800ms。
        mIat.setParameter(SpeechConstant.VAD_EOS, "1800");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/helloword.wav");
    }

    //执行音频流识别操作
    private void executeStream() {
        buffer.setLength(0);
        mResultText.setText(null);// 清空显示内容
        mIatResults.clear();
        //设置参数
        setParam();
        //设置音频来源为外部文件
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        //也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
        //mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
        //mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
        resultCode = mIat.startListening(mRecognizerListener);
        if (resultCode != ErrorCode.SUCCESS) {
            showToast("识别失败,错误码：" + resultCode + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        } else {
            byte[] audioData = FucUtil.readAudioFile(MainActivity.this, "iattest.wav");
            if (null != audioData) {
                showToast("开始音频流识别");
                // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），位长16bit，单声道的wav或者pcm
                // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                ArrayList<byte[]> bytes = FucUtil.splitBuffer(audioData, audioData.length, audioData.length / 3);
                for (int i = 0; i < bytes.size(); i++) {
                    mIat.writeAudio(bytes.get(i), 0, bytes.get(i).length);
                    try {
                        Thread.sleep(1000);//休眠1秒
                    } catch (Exception e) {
                    }
                }
                //mIat.writeAudio(audioData, 0, audioData.length );
                mIat.stopListening();
            } else {
                mIat.cancel();
                showToast("读取音频流失败");
            }
        }
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
      /*FlowerCollector.onResume(MainActivity.this);
      FlowerCollector.onPageStart(TAG);*/
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        super.onPause();
    }

    /**
     * 展示吐司
     */
    private void showToast(final String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private AlertDialog dialog;

    private void showAlertDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("自定弹框")//标题
                .setMessage("正在识别，请稍后...")//内容
                .setIcon(R.mipmap.ic_launcher)//图标
                .create();
        dialog.show();
    }
}
