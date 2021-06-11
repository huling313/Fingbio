package cn.org.bjca.fingerbio.bean;

import android.app.Activity;
import android.graphics.Bitmap;



public class FingerprintBean {

    private String status;
    private String message;

    private String fingerprintSignFile;//指纹签名文件
    private String signFile;//指纹和手写签名文件
    private String handwrittenSignFile;//手写签名文件


    public FingerprintBean() {
    }


    public FingerprintBean(String status) {
        this();
        this.status = status;
    }

    public FingerprintBean(String status, String message) {
        this(status);
        this.message = message;
    }



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFingerprintSignFile() {
        return fingerprintSignFile;
    }

    public void setFingerprintSignFile(String fingerprintSignFile) {
        this.fingerprintSignFile = fingerprintSignFile;
    }

    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    public String getHandwrittenSignFile() {
        return handwrittenSignFile;
    }

    public void setHandwrittenSignFile(String handwrittenSignFile) {
        this.handwrittenSignFile = handwrittenSignFile;
    }
}
