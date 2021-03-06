package com.molian.finger;


import android.app.Activity;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;


public class FingerPrintUtils {

    private CancellationSignal mCancellationSignal;
    private FingerprintManagerCompat mFingerprintManager;

    public FingerPrintUtils(Activity activity) {
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager = FingerprintManagerCompat.from(activity);
    }

    public void setFingerPrintListener(FingerprintManagerCompat.AuthenticationCallback callback) {
        mFingerprintManager.authenticate(null, 0, mCancellationSignal, callback, null);
    }

    public void reSetFingerPrintListener(FingerprintManagerCompat.AuthenticationCallback callback) {
        mFingerprintManager.authenticate(null, 0, null, callback, null);
    }

    public void stopsFingerPrintListener() {
        mCancellationSignal.cancel();
        mFingerprintManager = null;
        mCancellationSignal = null;
    }

    public boolean canFinger() {
//        if(mFingerprintManager.isHardwareDetected()){
//            return false;
//        }
//        if(mFingerprintManager.hasEnrolledFingerprints()){
//            return false;
//        }
        return true;
    }
}
