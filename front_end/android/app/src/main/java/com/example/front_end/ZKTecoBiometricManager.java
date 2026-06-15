package com.example.front_end;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Base64;
import android.util.Log;

import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintConstant;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ZKTecoBiometricManager {
    private static final String TAG = "ZKTecoBiometricManager";
    private static final int DEFAULT_CAPTURE_TIMEOUT_MS = 30000;
    private static final int DEFAULT_MATCH_THRESHOLD = 55;

    private final Context context;
    private FingerprintSensor sensor;
    private ZKFingerService zkFingerService;
    private volatile byte[] lastExtractedTemplate;
    private volatile String lastError;
    private final Object captureLock = new Object();
    private volatile boolean captureReady;

    public ZKTecoBiometricManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean loadNativeLibraries() {
        boolean algLoaded = false;
        boolean sensorCoreLoaded = false;
        boolean fingerLoaded = false;

        try {
            System.loadLibrary("zkalg12");
            Log.d(TAG, "Loaded zkalg12");
            algLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load zkalg12: " + e.getMessage());
            try {
                System.loadLibrary("zkalg");
                Log.d(TAG, "Loaded zkalg fallback");
                algLoaded = true;
            } catch (UnsatisfiedLinkError e2) {
                Log.w(TAG, "Failed to load zkalg fallback: " + e2.getMessage());
            }
        }

        try {
            System.loadLibrary("zksensorcore");
            Log.d(TAG, "Loaded zksensorcore");
            sensorCoreLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load zksensorcore: " + e.getMessage());
        }

        try {
            System.loadLibrary("zkfinger10");
            Log.d(TAG, "Loaded zkfinger10");
            fingerLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load zkfinger10: " + e.getMessage());
        }

        try {
            System.loadLibrary("slkidcap");
            Log.d(TAG, "Loaded slkidcap");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Optional slkidcap not available: " + e.getMessage());
        }

        return sensorCoreLoaded && fingerLoaded;
    }

    public boolean init() {
        try {
            if (sensor != null && zkFingerService != null) {
                return true;
            }

            sensor = new FingerprintSensor(context, null, null);
            zkFingerService = new ZKFingerService();
            int initResult = zkFingerService.init();
            Log.d(TAG, "ZKFingerService initialized with code=" + initResult);
            if (initResult != 0) {
                setLastError("ZKFingerService.init failed: " + initResult);
                return false;
            }

            return true;
        } catch (Throwable t) {
            setLastError("ZKTeco init failed: " + t.getMessage());
            sensor = null;
            zkFingerService = null;
            return false;
        }
    }

    public boolean openDevice(UsbDevice usbDevice) {
        if (usbDevice == null) {
            setLastError("UsbDevice is null");
            return false;
        }
        if (!init()) {
            return false;
        }

        try {
            sensor.open(usbDevice);
            Log.d(TAG, "Opened fingerprint sensor using UsbDevice");
        } catch (Throwable t) {
            setLastError("Unable to open ZKTeco device: " + t.getMessage());
            return false;
        }

        try {
            sensor.setCaptureMode(FingerprintConstant.SENSOR_CAPTURE_MODE_AUTO);
        } catch (Throwable t) {
            Log.w(TAG, "Unable to set auto capture mode: " + t.getMessage());
        }

        try {
            sensor.setFingerprintCaptureListener(0, new Listener());
        } catch (Throwable t) {
            Log.w(TAG, "Unable to set capture listener: " + t.getMessage());
        }

        return true;
    }

    public boolean closeDevice() {
        try {
            if (sensor != null) {
                try {
                    sensor.stopCapture(0);
                } catch (Throwable ignored) {
                }
                try {
                    sensor.close(0);
                } catch (Throwable ignored) {
                }
                try {
                    sensor.destroy();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error closing device: " + t.getMessage());
        } finally {
            sensor = null;
            zkFingerService = null;
        }
        return true;
    }

    public byte[] captureFingerprint() {
        return captureFingerprint(DEFAULT_CAPTURE_TIMEOUT_MS);
    }

    public byte[] captureFingerprint(int timeoutMs) {
        if (sensor == null) {
            setLastError("Fingerprint sensor is not opened");
            return null;
        }

        synchronized (captureLock) {
            captureReady = false;
            lastExtractedTemplate = null;
        }

        try {
            sensor.startCapture(0);
            Log.d(TAG, "Fingerprint capture started");
        } catch (Throwable t) {
            setLastError("Failed to start fingerprint capture: " + t.getMessage());
            return null;
        }

        synchronized (captureLock) {
            try {
                captureLock.wait(timeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            sensor.stopCapture(0);
        } catch (Throwable ignored) {
        }

        if (!captureReady || lastExtractedTemplate == null || lastExtractedTemplate.length == 0) {
            setLastError("No fingerprint template was captured");
            return null;
        }

        return lastExtractedTemplate.clone();
    }

    public boolean compareAgainstStoredTemplates(byte[] liveTemplate, List<String> storedBase64Templates, int threshold) {
        if (liveTemplate == null || liveTemplate.length == 0) {
            setLastError("Live template is empty");
            return false;
        }
        if (storedBase64Templates == null || storedBase64Templates.isEmpty()) {
            setLastError("No stored biometric templates available");
            return false;
        }
        if (zkFingerService == null) {
            setLastError("ZKFingerService is not initialized");
            return false;
        }

        for (String storedBase64 : storedBase64Templates) {
            if (storedBase64 == null || storedBase64.isEmpty()) {
                continue;
            }
            byte[] storedTemplate;
            try {
                storedTemplate = Base64.decode(storedBase64, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid base64 template skipped: " + e.getMessage());
                continue;
            }
            int score = verifyTemplate(liveTemplate, storedTemplate);
            Log.d(TAG, "Template comparison score=" + score);
            if (score >= threshold) {
                return true;
            }
        }

        return false;
    }

    private int verifyTemplate(byte[] liveTemplate, byte[] storedTemplate) {
        if (liveTemplate == null || storedTemplate == null) {
            return -1;
        }
        try {
            int score = zkFingerService.verify(liveTemplate, storedTemplate);
            return score;
        } catch (Throwable t) {
            Log.w(TAG, "ZKFingerService.verify failed: " + t.getMessage());
            return -1;
        }
    }

    public String getLastError() {
        return lastError;
    }

    private void setLastError(String message) {
        lastError = message;
        Log.e(TAG, message);
    }

    private class Listener implements FingerprintCaptureListener {
        @Override
        public void captureError(FingerprintException throwable) {
            setLastError("Fingerprint capture error: " + throwable.getMessage());
            notifyCaptureReady();
        }

        @Override
        public void captureOK(byte[] imageBuf) {
            Log.d(TAG, "Fingerprint image captured, waiting for template extraction");
        }

        @Override
        public void extractError(int errorCode) {
            setLastError("Fingerprint extraction error code: " + errorCode);
            notifyCaptureReady();
        }

        @Override
        public void extractOK(byte[] template) {
            if (template != null) {
                lastExtractedTemplate = template.clone();
            }
            captureReady = true;
            notifyCaptureReady();
        }
    }

    private void notifyCaptureReady() {
        synchronized (captureLock) {
            captureReady = true;
            captureLock.notifyAll();
        }
    }
}
