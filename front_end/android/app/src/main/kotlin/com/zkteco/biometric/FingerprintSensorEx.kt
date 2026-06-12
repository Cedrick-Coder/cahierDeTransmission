package com.zkteco.biometric

class FingerprintSensorEx {
    companion object {
        private var nativeLoaded: Boolean = false

        init {
            try {
                System.loadLibrary("zksensorcore")
                System.loadLibrary("slkidcap")
                System.loadLibrary("zkalg12")
                System.loadLibrary("zkfinger10")
                nativeLoaded = true
            } catch (t: Throwable) {
                // If native libs are missing or fail to load, avoid crashing the app
                nativeLoaded = false
            }
        }

        fun isNativeLoaded(): Boolean = nativeLoaded

        @JvmStatic external fun Init(): Int

        @JvmStatic external fun GetDeviceCount(): Int

        @JvmStatic external fun OpenDevice(fd: Int): Long

        @JvmStatic external fun DBInit(): Long

        @JvmStatic external fun GetParameters(device: Long, paramType: Int, paramValue: ByteArray, size: IntArray): Int

        @JvmStatic external fun ExtractFromImage(mhDB: Long, path: String, timeout: Int, fpTemplate: ByteArray, sizeFPTemp: IntArray): Int

        @JvmStatic external fun DBAdd(mhDB: Long, fid: Int, fpTemplate: ByteArray): Int

        @JvmStatic external fun DBIdentify(mhDB: Long, fpTemplate: ByteArray, fid: IntArray, score: IntArray): Int

        @JvmStatic external fun DBMatch(mhDB: Long, template1: ByteArray, template2: ByteArray): Int

        @JvmStatic external fun DBFree(mhDB: Long): Int

        @JvmStatic external fun CloseDevice(device: Long): Int

        @JvmStatic external fun Terminate(): Int

        @JvmStatic external fun AcquireFingerprint(device: Long, imgbuf: ByteArray, template: ByteArray, templateLen: IntArray): Int
    }
}
