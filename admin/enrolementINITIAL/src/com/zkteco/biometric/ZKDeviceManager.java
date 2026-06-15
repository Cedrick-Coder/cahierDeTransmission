package com.zkteco.biometric;

import java.awt.image.BufferedImage;
import java.util.Base64;
import javax.swing.SwingUtilities;

public class ZKDeviceManager {
    public interface Listener {
        void onStatus(String message);
        void onImageCaptured(BufferedImage image);
        void onEnrollmentReady(String base64Template);
    }

    private final Listener listener;
    private volatile boolean mbStop = true;
    private long mhDevice = 0;
    private long mhDB = 0;

    private final byte[][] regtemparray = new byte[3][2048];
    private boolean bRegister = false;
    private int enroll_idx = 0;
    private volatile boolean waitingForFinger = false;

    private int fpWidth = 0;
    private int fpHeight = 0;
    private byte[] imgbuf = null;
    private final byte[] template = new byte[2048];
    private final int[] templateLen = new int[1];

    private WorkThread workThread = null;

    public ZKDeviceManager(Listener listener) {
        this.listener = listener != null ? listener : new Listener() {
            @Override public void onStatus(String message) {}
            @Override public void onImageCaptured(BufferedImage image) {}
            @Override public void onEnrollmentReady(String base64Template) {}
        };
    }

    public boolean openDevice() {
        if (mhDevice != 0) {
            fireStatus("Le lecteur est déjà ouvert.");
            return true;
        }

        bRegister = false;
        enroll_idx = 0;

        if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
            fireStatus("Échec de l'initialisation !");
            return false;
        }

        if (FingerprintSensorEx.GetDeviceCount() <= 0) {
            fireStatus("Aucun lecteur ZKTeco détecté !");
            freeSensor();
            return false;
        }

        mhDevice = FingerprintSensorEx.OpenDevice(0);
        if (mhDevice == 0) {
            fireStatus("Échec de l'ouverture du lecteur !");
            freeSensor();
            return false;
        }

        mhDB = FingerprintSensorEx.DBInit();
        if (mhDB == 0) {
            fireStatus("Échec de l'initialisation de la mémoire cache !");
            freeSensor();
            return false;
        }

        byte[] paramValue = new byte[4];
        int[] size = new int[]{4};

        FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
        fpWidth = byteArrayToInt(paramValue);
        FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
        fpHeight = byteArrayToInt(paramValue);

        imgbuf = new byte[fpWidth * fpHeight];

        /*
         * Do NOT start the acquisition thread here. Opening the device should only
         * initialize the hardware and buffer; continuous capture is started when
         * the app requests enrollment/verification/identification. This avoids
         * immediate fingerprint capture after clicking "Open" in the UI.
         */
        mbStop = true;
        workThread = null;

        fireStatus("Lecteur prêt. Cliquez sur 'Démarrer enrôlement' pour capturer.");
        return true;
    }

    public void closeDevice() {
        freeSensor();
        fireStatus("Lecteur fermé.");
    }

    public void startEnrollment() {
        if (!isOpen()) {
            fireStatus("Veuillez d'abord ouvrir le lecteur !");
            return;
        }
        enroll_idx = 0;
        bRegister = true;
        waitingForFinger = true;
        startCaptureIfNeeded();
        fireStatus("Veuillez placer votre doigt 3 fois sur le lecteur. En attente du doigt...");
    }

    public boolean isOpen() {
        return mhDevice != 0 && mhDB != 0;
    }

    public void setVerifyMode() {
        if (!isOpen()) {
            fireStatus("Veuillez d'abord ouvrir le lecteur !");
            return;
        }
        bRegister = false;
        startCaptureIfNeeded();
        fireStatus("Mode vérification activé.");
    }

    public void setIdentifyMode() {
        if (!isOpen()) {
            fireStatus("Veuillez d'abord ouvrir le lecteur !");
            return;
        }
        bRegister = false;
        startCaptureIfNeeded();
        fireStatus("Mode identification activé.");
    }

    public void registerFromImage() {
        fireStatus("Fonction d'enregistrement par image non implémentée.");
    }

    public void verifyFromImage() {
        fireStatus("Fonction de vérification par image non implémentée.");
    }

    private void fireStatus(String message) {
        SwingUtilities.invokeLater(() -> listener.onStatus(message));
    }

    private void fireImage(final BufferedImage image) {
        SwingUtilities.invokeLater(() -> listener.onImageCaptured(image));
    }

    private void fireEnrollmentReady(final String base64Template) {
        SwingUtilities.invokeLater(() -> listener.onEnrollmentReady(base64Template));
    }

    private void freeSensor() {
        mbStop = true;
        if (workThread != null) {
            workThread.interrupt();
            try { workThread.join(1000); } catch (InterruptedException ignored) {}
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

    /**
     * Start the acquisition thread if it's not already running.
     */
    private synchronized void startCaptureIfNeeded() {
        if (!isOpen()) return;
        if (workThread != null && !mbStop) return;

        mbStop = false;
        workThread = new WorkThread();
        workThread.start();
    }

    private void onCaptureOK(byte[] imgBuf) {
        fireImage(toBufferedImage(imgBuf, fpWidth, fpHeight));
    }

    private void onExtractOK(byte[] extractedTemplate, int len) {
        if (!bRegister) return;

        if (enroll_idx > 0 && FingerprintSensorEx.DBMatch(mhDB, regtemparray[enroll_idx - 1], extractedTemplate) <= 0) {
            fireStatus("Veuillez utiliser le MÊME doigt pour les 3 passages.");
            return;
        }
        System.arraycopy(extractedTemplate, 0, regtemparray[enroll_idx], 0, 2048);
        enroll_idx++;

        // Show progress like "1/3", "2/3", "3/3"
        String progress = enroll_idx + "/3";

        if (enroll_idx == 3) {
            int[] retLen = new int[]{2048};
            byte[] finalMergedTemplate = new byte[retLen[0]];

            int ret = FingerprintSensorEx.DBMerge(mhDB, regtemparray[0], regtemparray[1], regtemparray[2], finalMergedTemplate, retLen);

            if (ret == 0) {
                byte[] actualTemplateData = new byte[retLen[0]];
                System.arraycopy(finalMergedTemplate, 0, actualTemplateData, 0, retLen[0]);

                String base64String = Base64.getEncoder().encodeToString(actualTemplateData);

                fireStatus("Enrôlement " + progress + " terminé. Prêt pour l'envoi au backend.");
                fireEnrollmentReady(base64String);
            } else {
                fireStatus("Échec de la fusion de l'empreinte, code erreur=" + ret);
            }
            // stop waiting/capture after finishing (success or fail)
            waitingForFinger = false;
            stopCapture();
            bRegister = false;
        } else {
            // Prepare to wait for the next finger placement
            waitingForFinger = true;
            fireStatus("Enrôlement " + progress + ". Encore " + (3 - enroll_idx) + " passage(s) requis. Placez le doigt.");
        }
    }

    /**
     * Stop the acquisition thread but keep the device open.
     */
    private synchronized void stopCapture() {
        mbStop = true;
        if (workThread != null) {
            workThread.interrupt();
            try { workThread.join(500); } catch (InterruptedException ignored) {}
            workThread = null;
        }
    }

    /**
     * Public wrapper to stop acquisition from UI code without closing the device.
     */
    public void stopAcquisition() {
        stopCapture();
    }

    private static BufferedImage toBufferedImage(byte[] imageBuf, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setDataElements(0, 0, width, height, imageBuf);
        return image;
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
            // small warm-up delay to allow sensor parameters to settle
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            int consecutiveCaptureErrors = 0;

            while (!mbStop) {
                templateLen[0] = 2048;
                int ret;
                try {
                    ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgbuf, template, templateLen);
                } catch (Throwable t) {
                    fireStatus("Exception during AcquireFingerprint: " + t.getMessage());
                    break;
                }

                if (ret == FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                    // got a good capture — if we were waiting for the finger, clear the flag
                    if (waitingForFinger) waitingForFinger = false;
                    onCaptureOK(imgbuf);
                    onExtractOK(template, templateLen[0]);
                    consecutiveCaptureErrors = 0;
                } else if (ret == FingerprintSensorErrorCode.ZKFP_ERR_TIMEOUT) {
                    // timeout is normal when no finger is placed; don't spam
                    if (waitingForFinger) {
                        fireStatus("En attente du doigt... (placez le doigt sur le capteur)");
                    }
                } else {
                    // If we're waiting for the finger to be placed, suppress noisy errors
                    if (waitingForFinger) {
                        fireStatus("En attente du doigt... (placez le doigt sur le capteur)");
                    } else {
                        // Provide more diagnostic information to help debugging
                        String msg = "Échec de l'acquisition d'empreinte, code erreur=" + ret
                                + " (fpW=" + fpWidth + ", fpH=" + fpHeight + ", bufLen=" + (imgbuf==null?0:imgbuf.length)
                                + ", tplLenRequest=" + templateLen[0] + ")";
                        fireStatus(msg);
                        consecutiveCaptureErrors++;
                        if (consecutiveCaptureErrors >= 10) {
                            fireStatus("Erreur persistante de capture. Arrêt de l'acquisition. Ré-ouvrez le lecteur.");
                            mbStop = true;
                            break;
                        }
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }
}
