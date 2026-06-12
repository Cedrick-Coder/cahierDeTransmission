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

        @JvmStatic fun Init(): Int {
            if (!nativeLoaded) return -1
            return nativeInit()
        }
        @JvmStatic private external fun nativeInit(): Int

        @JvmStatic fun GetDeviceCount(): Int { if (!nativeLoaded) return -1; return nativeGetDeviceCount() }
        @JvmStatic private external fun nativeGetDeviceCount(): Int

        @JvmStatic fun OpenDevice(deviceIndex: Int): Long { if (!nativeLoaded) return 0L; return nativeOpenDevice(deviceIndex) }
        @JvmStatic private external fun nativeOpenDevice(deviceIndex: Int): Long

        @JvmStatic fun DBInit(): Long { if (!nativeLoaded) return 0L; return nativeDBInit() }
        @JvmStatic private external fun nativeDBInit(): Long

        @JvmStatic fun GetParameters(device: Long, paramType: Int, paramValue: ByteArray, size: IntArray): Int { if (!nativeLoaded) return -1; return nativeGetParameters(device, paramType, paramValue, size) }
        @JvmStatic private external fun nativeGetParameters(device: Long, paramType: Int, paramValue: ByteArray, size: IntArray): Int

        @JvmStatic fun ExtractFromImage(mhDB: Long, path: String, timeout: Int, fpTemplate: ByteArray, sizeFPTemp: IntArray): Int { if (!nativeLoaded) return -1; return nativeExtractFromImage(mhDB, path, timeout, fpTemplate, sizeFPTemp) }
        @JvmStatic private external fun nativeExtractFromImage(mhDB: Long, path: String, timeout: Int, fpTemplate: ByteArray, sizeFPTemp: IntArray): Int

        @JvmStatic fun DBAdd(mhDB: Long, fid: Int, fpTemplate: ByteArray): Int { if (!nativeLoaded) return -1; return nativeDBAdd(mhDB, fid, fpTemplate) }
        @JvmStatic private external fun nativeDBAdd(mhDB: Long, fid: Int, fpTemplate: ByteArray): Int

        @JvmStatic fun DBIdentify(mhDB: Long, fpTemplate: ByteArray, fid: IntArray, score: IntArray): Int { if (!nativeLoaded) return -1; return nativeDBIdentify(mhDB, fpTemplate, fid, score) }
        @JvmStatic private external fun nativeDBIdentify(mhDB: Long, fpTemplate: ByteArray, fid: IntArray, score: IntArray): Int

        @JvmStatic fun DBMatch(mhDB: Long, template1: ByteArray, template2: ByteArray): Int { if (!nativeLoaded) return -1; return nativeDBMatch(mhDB, template1, template2) }
        @JvmStatic private external fun nativeDBMatch(mhDB: Long, template1: ByteArray, template2: ByteArray): Int

        @JvmStatic fun DBFree(mhDB: Long): Int { if (!nativeLoaded) return -1; return nativeDBFree(mhDB) }
        @JvmStatic private external fun nativeDBFree(mhDB: Long): Int

        @JvmStatic fun CloseDevice(device: Long): Int { if (!nativeLoaded) return -1; return nativeCloseDevice(device) }
        @JvmStatic private external fun nativeCloseDevice(device: Long): Int

        @JvmStatic fun Terminate(): Int { if (!nativeLoaded) return -1; return nativeTerminate() }
        @JvmStatic private external fun nativeTerminate(): Int

        @JvmStatic fun AcquireFingerprint(device: Long, imgbuf: ByteArray, template: ByteArray, templateLen: IntArray): Int { if (!nativeLoaded) return -1; return nativeAcquireFingerprint(device, imgbuf, template, templateLen) }
        @JvmStatic private external fun nativeAcquireFingerprint(device: Long, imgbuf: ByteArray, template: ByteArray, templateLen: IntArray): Int
    }
}
