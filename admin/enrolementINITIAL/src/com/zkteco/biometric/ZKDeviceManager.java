package com.zkteco.biometric;

import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public class ZKDeviceManager {
    public interface Listener {
        void onStatus(String message);
        void onImageCaptured(BufferedImage image);
    }

    private final Listener listener;
    private volatile boolean mbStop = true;
    private long mhDevice = 0;
    private long mhDB = 0;

    private final byte[] lastRegTemp = new byte[2048];
    private int cbRegTemp = 0;
    private final byte[][] regtemparray = new byte[3][2048];
    private boolean bRegister = false;
    private boolean bIdentify = true;
    private int iFid = 1;
    private int nFakeFunOn = 1;
    private int enroll_idx = 0;

    private int fpWidth = 0;
    private int fpHeight = 0;
    private byte[] imgbuf = null;
    private final byte[] template = new byte[2048];
    private final int[] templateLen = new int[1];

    private WorkThread workThread = null;
    private static final String DEFAULT_IMAGE_PATH = "d:\\test\\fingerprint.bmp";

    public ZKDeviceManager(Listener listener) {
        this.listener = listener != null ? listener : new Listener() {
            @Override
            public void onStatus(String message) {}
            @Override
            public void onImageCaptured(BufferedImage image) {}
        };
    }

    public boolean openDevice() {
        if (mhDevice != 0) {
            fireStatus("Device already open");
            return true;
        }

        cbRegTemp = 0;
        bRegister = false;
        bIdentify = false;
        iFid = 1;
        enroll_idx = 0;

        if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
            fireStatus("Init failed!");
            return false;
        }

        int ret = FingerprintSensorEx.GetDeviceCount();
        if (ret < 0) {
            fireStatus("No devices connected!");
            freeSensor();
            return false;
        }

        mhDevice = FingerprintSensorEx.OpenDevice(0);
        if (mhDevice == 0) {
            fireStatus("Open device fail, ret = " + ret + "!");
            freeSensor();
            return false;
        }

        mhDB = FingerprintSensorEx.DBInit();
        if (mhDB == 0) {
            fireStatus("Init DB fail, ret = " + ret + "!");
            freeSensor();
            return false;
        }

        byte[] paramValue = new byte[4];
        int[] size = new int[1];
        size[0] = 4;
        FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);
        size[0] = 4;
        FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgbuf = new byte[fpWidth * fpHeight];
        mbStop = false;
        workThread = new WorkThread();
        workThread.start();

        fireStatus("Open succ!");
        return true;
    }

    public void closeDevice() {
        freeSensor();
        fireStatus("Close succ!");
    }

    public void startEnrollment() {
        if (!isOpen()) {
            fireStatus("Please open device first!");
            return;
        }
        enroll_idx = 0;
        bRegister = true;
        fireStatus("Please press your finger 3 times!");
    }

    public void setVerifyMode() {
        if (!isOpen()) {
            fireStatus("Please open device first!");
            return;
        }
        if (bRegister) {
            enroll_idx = 0;
            bRegister = false;
        }
        if (bIdentify) {
            bIdentify = false;
        }
        fireStatus("Verify mode selected");
    }

    public void setIdentifyMode() {
        if (!isOpen()) {
            fireStatus("Please open device first!");
            return;
        }
        if (bRegister) {
            enroll_idx = 0;
            bRegister = false;
        }
        if (!bIdentify) {
            bIdentify = true;
        }
        fireStatus("Identify mode selected");
    }

    public void registerFromImage() {
        registerFromImage(DEFAULT_IMAGE_PATH);
    }

    public void registerFromImage(String path) {
        if (mhDB == 0) {
            fireStatus("Please open device first!");
            return;
        }

        byte[] fpTemplate = new byte[2048];
        int[] sizeFPTemp = new int[1];
        sizeFPTemp[0] = 2048;
        int ret = FingerprintSensorEx.ExtractFromImage(mhDB, path, 500, fpTemplate, sizeFPTemp);
        if (ret != 0) {
            fireStatus("ExtractFromImage fail, ret=" + ret);
            return;
        }

        ret = FingerprintSensorEx.DBAdd(mhDB, iFid, fpTemplate);
        if (ret == 0) {
            iFid++;
            cbRegTemp = sizeFPTemp[0];
            System.arraycopy(fpTemplate, 0, lastRegTemp, 0, cbRegTemp);
            fireStatus("Enroll succ");
        } else {
            fireStatus("DBAdd fail, ret=" + ret);
        }
    }

    public void verifyFromImage() {
        verifyFromImage(DEFAULT_IMAGE_PATH);
    }

    public void verifyFromImage(String path) {
        if (mhDB == 0) {
            fireStatus("Please open device first!");
            return;
        }

        byte[] fpTemplate = new byte[2048];
        int[] sizeFPTemp = new int[1];
        sizeFPTemp[0] = 2048;
        int ret = FingerprintSensorEx.ExtractFromImage(mhDB, path, 500, fpTemplate, sizeFPTemp);
        if (ret != 0) {
            fireStatus("ExtractFromImage fail, ret=" + ret);
            return;
        }

        if (bIdentify) {
            int[] fid = new int[1];
            int[] score = new int[1];
            ret = FingerprintSensorEx.DBIdentify(mhDB, fpTemplate, fid, score);
            if (ret == 0) {
                fireStatus("Identify succ, fid=" + fid[0] + ", score=" + score[0]);
            } else {
                fireStatus("Identify fail, errcode=" + ret);
            }
        } else {
            if (cbRegTemp <= 0) {
                fireStatus("Please register first!");
            } else {
                ret = FingerprintSensorEx.DBMatch(mhDB, lastRegTemp, fpTemplate);
                if (ret > 0) {
                    fireStatus("Verify succ, score=" + ret);
                } else {
                    fireStatus("Verify fail, ret=" + ret);
                }
            }
        }
    }

    public boolean isOpen() {
        return mhDevice != 0 && mhDB != 0;
    }

    private void fireStatus(String message) {
        SwingUtilities.invokeLater(() -> listener.onStatus(message));
    }

    private void fireImage(final BufferedImage image) {
        SwingUtilities.invokeLater(() -> listener.onImageCaptured(image));
    }

    private void freeSensor() {
        mbStop = true;
        if (workThread != null) {
            workThread.interrupt();
            try {
                workThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            workThread = null;
        }
        if (mhDB != 0) {
            FingerprintSensorEx.DBFree(mhDB);
            mhDB = 0;
        }
        if (mhDevice != 0) {
            FingerprintSensorEx.CloseDevice(mhDevice);
            mhDevice = 0;
        }
        FingerprintSensorEx.Terminate();
    }

    private void onCaptureOK(byte[] imgBuf) {
        BufferedImage image = toBufferedImage(imgBuf, fpWidth, fpHeight);
        fireImage(image);
    }

    private void onExtractOK(byte[] template, int len) {
        if (bRegister) {
            int[] fid = new int[1];
            int[] score = new int[1];
            int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
            if (ret == 0) {
                fireStatus("The finger already enrolled by " + fid[0] + ", cancel enroll");
                bRegister = false;
                enroll_idx = 0;
                return;
            }
            if (enroll_idx > 0 && FingerprintSensorEx.DBMatch(mhDB, regtemparray[enroll_idx - 1], template) <= 0) {
                fireStatus("Please press the same finger 3 times for enrollment");
                return;
            }
            System.arraycopy(template, 0, regtemparray[enroll_idx], 0, 2048);
            enroll_idx++;
            if (enroll_idx == 3) {
                int[] _retLen = new int[1];
                _retLen[0] = 2048;
                byte[] regTemp = new byte[_retLen[0]];
                if (0 == (ret = FingerprintSensorEx.DBMerge(mhDB, regtemparray[0], regtemparray[1], regtemparray[2], regTemp, _retLen)) &&
                    0 == (ret = FingerprintSensorEx.DBAdd(mhDB, iFid, regTemp))) {
                    iFid++;
                    cbRegTemp = _retLen[0];
                    System.arraycopy(regTemp, 0, lastRegTemp, 0, cbRegTemp);
                    fireStatus("Enroll succ");
                } else {
                    fireStatus("Enroll fail, error code=" + ret);
                }
                bRegister = false;
            } else {
                fireStatus("You need to press the " + (3 - enroll_idx) + " times fingerprint");
            }
        } else {
            if (bIdentify) {
                int[] fid = new int[1];
                int[] score = new int[1];
                int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
                if (ret == 0) {
                    fireStatus("Identify succ, fid=" + fid[0] + ", score=" + score[0]);
                } else {
                    fireStatus("Identify fail, errcode=" + ret);
                }
            } else {
                if (cbRegTemp <= 0) {
                    fireStatus("Please register first!");
                } else {
                    int ret = FingerprintSensorEx.DBMatch(mhDB, lastRegTemp, template);
                    if (ret > 0) {
                        fireStatus("Verify succ, score=" + ret);
                    } else {
                        fireStatus("Verify fail, ret=" + ret);
                    }
                }
            }
        }
    }

    private static BufferedImage toBufferedImage(byte[] imageBuf, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setDataElements(0, 0, width, height, imageBuf);
        return image;
    }

    @SuppressWarnings("unused")
    private static byte[] intToByteArray(final int number) {
        byte[] abyte = new byte[4];
        abyte[0] = (byte) (0xff & number);
        abyte[1] = (byte) ((0xff00 & number) >> 8);
        abyte[2] = (byte) ((0xff0000 & number) >> 16);
        abyte[3] = (byte) ((0xff000000 & number) >> 24);
        return abyte;
    }

    private static int byteArrayToInt(byte[] bytes) {
        int number = bytes[0] & 0xFF;
        number |= ((bytes[1] << 8) & 0xFF00);
        number |= ((bytes[2] << 16) & 0xFF0000);
        number |= ((bytes[3] << 24) & 0xFF000000);
        return number;
    }

    private class WorkThread extends Thread {
        @Override
        public void run() {
            int ret;
            while (!mbStop) {
                templateLen[0] = 2048;
                ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgbuf, template, templateLen);
                if (ret == 0) {
                    if (nFakeFunOn == 1) {
                        byte[] paramValue = new byte[4];
                        int[] size = new int[1];
                        size[0] = 4;
                        ret = FingerprintSensorEx.GetParameters(mhDevice, 2004, paramValue, size);
                        int nFakeStatus = byteArrayToInt(paramValue);
                        if (ret == 0 && (byte) (nFakeStatus & 31) != 31) {
                            fireStatus("Is a fake finger?");
                            return;
                        }
                    }
                    onCaptureOK(imgbuf);
                    onExtractOK(template, templateLen[0]);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
