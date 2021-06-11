package cn.org.bjca.fingerbio.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import com.IDWORLD.LAPI;

import cn.org.bjca.finger.ImageUtils;
import cn.org.bjca.finger.model.SignSubmitModel;
import cn.org.bjca.finger.value.ConstanceValue;
import cn.org.bjca.finger.value.Constant;
import cn.org.bjca.fingerbio.R;
import cn.org.bjca.fingerbio.bean.FingerprintBean;
import cn.org.bjca.fingerbio.finger.FingerManage;

/**
 * 大板子
 */
public class FingerZAZ0C0Activity extends AppCompatActivity {
    private ImageView mImgView = null;
    private TextView tvClose = null;
    private TextView tv_tip = null;
    private ImageView imageClose = null;


    private TextView tv_restart_finger = null;
    private TextView tv_submit = null;
    private TextView tv_restart = null;
    private TextView tv_ignore = null;

    private LinearLayout lin_restart_finger;
    private RelativeLayout rl_error_finger;

    //测试模板，二代证采集的1024字节有2个模板
    private byte[] mTestFeature = new byte[512];
    //测试模板Base64字符串
    private String mstrTemplate = "QwH/EgFjSAAAAAAAAAAAAAAAACwBmmq0AP///////7QjTP5ZKRb8miux/JE4Yv5LP+L8RE8q/IJTV/7BV0H+cWIG/LtqlPxpcBT8i4FD/oCQO/45mS38d586/qGhKf5fsuv8arfv/JO3cPw2uD78b78B/qTGGf6SzBT+T9E8/GvR8/wo4j78b/Pu/K/0Ef4lCzD9oxEA/70WH/+MG+79ZB7i/a0hA/8sJSz9PCsq/VQ2Lv2+Pnj9jEI3/alFH/1QUhj9qlIA/YtXJv1pXCD9AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACpAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJc=";

    private boolean mbStop = true;
    private CountDownLatch countdownLatch = new CountDownLatch(1);

    private Context mContext = null;
    private Bitmap bitmap;
    private String fingerprintResultCode = null;
    private FingerprintBean fingerprintBean;
    private String handwrittenSignFile;

    private int mHoldTime = 0;
    // 5秒内没有采集到指纹，则展示出跳过采集的按钮
    private int mMaxHoldTime = 5 * 1000;

    private int mEveryGetImageTime = 500;

    private int mDeviceId;
    private LAPI mLApi;

    Timer mTimerFinger = new Timer();// 实例化Timer类
    private TimerTask mTimerTaskGetFinger;

    private MyHandler myHandler;

    private SignSubmitModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_finger);
        mTestFeature = Base64.decode(mstrTemplate, Base64.NO_WRAP);


        mImgView = findViewById(R.id.imageView);
        imageClose = findViewById(R.id.image_close);
        tvClose = findViewById(R.id.tv_close);
        tv_tip = findViewById(R.id.tv_tip);

        lin_restart_finger = findViewById(R.id.lin_restart_finger);
        tv_restart_finger = findViewById(R.id.tv_restart_finger);
        tv_submit = findViewById(R.id.tv_submit);

        tv_restart = findViewById(R.id.tv_restart);
        tv_ignore = findViewById(R.id.tv_ignore);

        rl_error_finger = findViewById(R.id.rl_error_finger);


        mContext = this.getApplicationContext();

        fingerprintBean = new FingerprintBean();
        model = new SignSubmitModel();

        Intent intent = getIntent();
        handwrittenSignFile = intent.getStringExtra(Constant.HANDWRITTEN_SIGN_FILE);

        //点击dialog外部不消失
        this.setFinishOnTouchOutside(false);


        imageClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("取消指纹采集");
                fingerprintResultCode = ConstanceValue.FINGERPRINT_CANCEL_CODE;

                finish();
            }
        });

        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("跳过指纹采集");
                fingerprintResultCode = ConstanceValue.FINGERPRINT_NO_CODE;

                model.setHandwrittenSignFile(handwrittenSignFile);
                model.setSignFile(handwrittenSignFile);
                finish();
            }
        });

        tv_restart_finger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//重新录用指纹

                mImgView.setImageResource(cn.org.bjca.finger.zkyw.R.mipmap.finger_image);
                lin_restart_finger.setVisibility(View.GONE);
                tv_tip.setVisibility(View.VISIBLE);
                tvClose.setVisibility(View.VISIBLE);

                //重新打开设备，获取指纹
                OpenDeviceAndRequestDevice();
                startGetFingerImage();
            }
        });

        //提交签名
        tv_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageSuccess();
            }
        });

        myHandler = new MyHandler(this);

        this.mLApi = FingerManage.getLApi(this);
        //进入页面就打开指纹采集功能
        OpenDeviceAndRequestDevice();
        startGetFingerImage();
    }

    private Toast mToast;

    private void toast(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(message);
                }
                mToast.show();
            }
        });
    }


    private void OpenDeviceAndRequestDevice() {
        if (mDeviceId != 0) {
            toast("设备已连接，请先断开连接！");
            return;
        }
        mDeviceId = this.mLApi.OpenDeviceEx();
        if (mDeviceId == 0) {
            toast("设备连接失败，请重试！");
        }
    }

    private void getFingerImage() {
        if (mDeviceId == 0) {
            return;
        }
        byte[] image = new byte[LAPI.WIDTH * LAPI.HEIGHT];
        int result = mLApi.GetImage(mDeviceId, image);
        if (result == 1) {
            int score = mLApi.GetImageQuality(mDeviceId, image);
            // 得分质量大于50，可以认为指纹采集成功
            if (score > 50) {
                closeDevice();
                int width = LAPI.WIDTH;
                int height = LAPI.HEIGHT;
                int[] RGBbits = new int[width * height];
                for (int i = 0; i < width * height; i++) {
                    int v;
                    if (image != null) v = image[i] & 0xff;
                    else v = 0;

                    if (v < 200)
                        RGBbits[i] = Color.argb(255, 255, v, v);
                    else
                        RGBbits[i] = Color.argb(0, v, v, v);
                }
//                bitmap = Bitmap.createBitmap(RGBbits, width, height, Config.RGB_565);
                bitmap = Bitmap.createBitmap(RGBbits, width, height, Config.ARGB_8888);
                mImgView.setImageBitmap(bitmap);

                //Log.e("test", "width" + width + "__bbb___height" + height);//width256  height360

                tv_tip.setVisibility(View.GONE);
                tvClose.setVisibility(View.GONE);
                lin_restart_finger.setVisibility(View.VISIBLE);

            }
        }
    }

    private void getImageSuccess() {
        fingerprintResultCode = ConstanceValue.FINGERPRINT_SUCCESS_CODE;

        //手绘签名bitmap
        Bitmap signBitmap = BitmapFactory.decodeFile(handwrittenSignFile);
        //指纹签名bitmap，把指纹图片指定大小
        Bitmap fingerBitmap = ImageUtils.getNewImage(bitmap, 85, 120);//400x160

        //拼接指纹和手绘图片
        Bitmap compoundBitmap = ImageUtils.combineImage(signBitmap, fingerBitmap);

        //手绘指纹图片转成file格式
        File signFiles = ImageUtils.compressImage(compoundBitmap, mContext);

        //压缩指纹图片，并转为file格式
        File fingerprintSignFile = ImageUtils.compressImage(fingerBitmap, mContext);

        model.setHandwrittenSignFile(handwrittenSignFile);
        model.setFingerprintSignFile(fingerprintSignFile.getAbsolutePath());
        model.setSignFile(signFiles.getAbsolutePath());

//        mImgView.setImageBitmap(compoundBitmap);

        finish();
    }

    private void startGetFingerImage() {
//        toast("请采集指纹！");
        mTimerTaskGetFinger = new TimerTask() {
            @Override
            public void run() {
                myHandler.sendEmptyMessage(MyHandler.codeGetImage);
            }
        };
        mTimerFinger.schedule(mTimerTaskGetFinger, mEveryGetImageTime, mEveryGetImageTime);


//        myHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                tvClose.setVisibility(View.VISIBLE);
//            }
//        }, mMaxHoldTime);
    }


    void closeDevice() {
        if (mTimerTaskGetFinger != null) {
            mTimerTaskGetFinger.cancel();
            mTimerTaskGetFinger = null;
        }
        if (mDeviceId != 0) {
            this.mLApi.CloseDeviceEx(mDeviceId);
        }
        mDeviceId = 0;
    }

    @Override
    public void finish() {
        closeDevice();  //尝试关闭设备

        //结束后才关闭mTimerFinger
        if (mTimerFinger != null) {
            mTimerFinger.cancel();
            mTimerFinger = null;
        }

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constant.SUBMIT_DATA, model);
        intent.putExtras(bundle);

        switch (fingerprintResultCode) {
            case ConstanceValue.FINGERPRINT_SUCCESS_CODE:
                setResult(ConstanceValue.FINGERPRINT_RESULT_CODE, intent);
                //成功
                break;
            case ConstanceValue.FINGERPRINT_CANCEL_CODE:
                //取消指纹采集
                setResult(ConstanceValue.FINGERPRINT_RESULT_CANCEL_CODE, intent);
                break;
            case ConstanceValue.FINGERPRINT_NO_CODE:
                //跳过采集
                setResult(ConstanceValue.FINGERPRINT_RESULT_IGNORE_CODE, intent);
                break;
        }

        super.finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mToast != null) {
            mToast.cancel();
        }
        if (myHandler != null) {
            myHandler.removeCallbacksAndMessages(null);
//            myHandler=null;
        }
    }


    private static class MyHandler extends Handler {

        final static int codeGetImage = 1000;


        WeakReference<FingerZAZ0C0Activity> mWeakRef;

        MyHandler(FingerZAZ0C0Activity activity) {
            mWeakRef = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case codeGetImage:
                    mWeakRef.get().getFingerImage();
                    break;
            }
        }
    }
}
