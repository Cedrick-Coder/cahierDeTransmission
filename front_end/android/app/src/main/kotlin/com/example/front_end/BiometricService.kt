package com.example.front_end

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.zkteco.biometric.FingerprintSensorErrorCode
import com.zkteco.biometric.FingerprintSensorEx
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class BiometricService(private val activity: Activity) {
    companion object {
        private const val TAG = "BiometricService"
        private const val THRESHOLD = 55
        private const val USB_PERMISSION_ACTION = "com.example.front_end.USB_PERMISSION"
        private const val USB_PERMISSION_TIMEOUT_MS = 30000L
    }

    // Store biometric records as Base64 strings (as received from backend)
    private var biometricDataInMemoryBase64: List<String> = emptyList()
    private var zktecoDeviceOpened: Boolean = false
    private var mhDevice: Long = 0
    private var mhDB: Long = 0
    private var fpWidth = 0
    private var fpHeight = 0
    private var imgbuf: ByteArray? = null
    private var lastZKError: String? = null
    private var isNativeLibrariesLoaded = false
    private var usbPermissionGranted = false
    private val apiService = BiometricApiService()

    init {
        loadNativeLibraries()
    }

    fun loadNativeLibraries(): Boolean {
        if (isNativeLibrariesLoaded) return true

        return try {
            System.loadLibrary("zksensorcore")
            System.loadLibrary("zkalg12")
            System.loadLibrary("zkfinger10")
            System.loadLibrary("slkidcap")

            if (!FingerprintSensorEx.isNativeLoaded()) {
                lastZKError = "Les bibliothèques natives ZKTeco n'ont pas pu être chargées"
                Log.e(TAG, lastZKError!!)
                false
            } else {
                isNativeLibrariesLoaded = true
                Log.d(TAG, "Native ZKTeco libraries loaded into memory")
                true
            }
        } catch (e: UnsatisfiedLinkError) {
            lastZKError = "Chargement des bibliothèques natives échoué: ${e.message}"
            Log.e(TAG, lastZKError!!)
            false
        } catch (e: Exception) {
            lastZKError = "Erreur lors du chargement des bibliothèques natives: ${e.message}"
            Log.e(TAG, lastZKError!!)
            false
        }
    }

    suspend fun initializeBiometricManager(): Boolean {
        if (!loadNativeLibraries()) return false
        if (!requestUsbPermission()) return false
        if (!prepareZKDevice()) return false
        if (biometricDataInMemoryBase64.isEmpty() && !loadBiometricData()) {
            Log.e(TAG, "Impossible de charger les données biométriques depuis le backend")
            return false
        }
        return true
    }

    suspend fun requestUsbPermission(): Boolean {
        if (usbPermissionGranted) {
            Log.d(TAG, "Permission USB déjà accordée")
            return true
        }

        val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = findUsbDevice(usbManager)

        if (usbDevice == null) {
            lastZKError = "Aucun périphérique USB trouvé"
            Log.e(TAG, lastZKError!!)
            return false
        }

        if (usbManager.hasPermission(usbDevice)) {
            usbPermissionGranted = true
            Log.d(TAG, "USB permission already granted for ${usbDevice.deviceName}")
            return true
        }

        val requestResult = requestUsbPermissionForDevice(usbManager, usbDevice)
        if (requestResult) {
            usbPermissionGranted = true
            Log.d(TAG, "USB permission granted for ${usbDevice.deviceName}")
        } else {
            lastZKError = "Permission USB refusée"
            Log.e(TAG, lastZKError!!)
        }
        return requestResult
    }

    private suspend fun requestUsbPermissionForDevice(usbManager: UsbManager, usbDevice: UsbDevice): Boolean {
        return withTimeoutOrNull(USB_PERMISSION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                val permissionIntent = Intent(USB_PERMISSION_ACTION)
                val pendingIntent = PendingIntent.getBroadcast(
                    activity,
                    0,
                    permissionIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
                )

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try {
                            if (intent?.action != USB_PERMISSION_ACTION) return
                            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                            if (device == null || device.deviceId != usbDevice.deviceId) {
                                return
                            }

                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            if (granted) {
                                continuation.resume(true)
                            } else {
                                continuation.resume(false)
                            }
                        } catch (exc: Exception) {
                            Log.e(TAG, "USB permission broadcast error: ${exc.message}")
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        } finally {
                            activity.unregisterReceiver(this)
                        }
                    }
                }

                activity.registerReceiver(receiver, IntentFilter(USB_PERMISSION_ACTION))
                continuation.invokeOnCancellation {
                    try {
                        activity.unregisterReceiver(receiver)
                    } catch (ignored: Exception) {
                    }
                }

                activity.runOnUiThread {
                    AlertDialog.Builder(activity)
                        .setTitle("Autorisation USB requise")
                        .setMessage("Pour utiliser le capteur biométrique USB, l'application a besoin de votre autorisation.")
                        .setPositiveButton("Autoriser") { _, _ ->
                            usbManager.requestPermission(usbDevice, pendingIntent)
                        }
                        .setNegativeButton("Refuser") { _, _ ->
                            if (continuation.isActive) continuation.resume(false)
                            try {
                                activity.unregisterReceiver(receiver)
                            } catch (ignored: Exception) {
                            }
                        }
                        .setOnCancelListener {
                            if (continuation.isActive) continuation.resume(false)
                            try {
                                activity.unregisterReceiver(receiver)
                            } catch (ignored: Exception) {
                            }
                        }
                        .show()
                }
            }
        } ?: false
    }

    private fun findUsbDevice(usbManager: UsbManager): UsbDevice? {
        val deviceList = usbManager.deviceList.values
        if (deviceList.isEmpty()) {
            return null
        }

        deviceList.forEach { device ->
            if (device.deviceClass == UsbConstants.USB_CLASS_PER_INTERFACE || device.deviceClass == UsbConstants.USB_CLASS_HID || device.deviceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                Log.d(TAG, "USB device found: ${device.deviceName} vid=${device.vendorId} pid=${device.productId}")
                return device
            }
        }

        return deviceList.firstOrNull()
    }

    /**
     * Load biometric data from PersonneAutorisee table via API
     * Data is kept in memory (RAM) only
     */
    suspend fun loadBiometricData(): Boolean {
        return try {
            val base64BiometricList = apiService.fetchBiometricDataFromServer()

            if (base64BiometricList.isEmpty()) {
                Log.e(TAG, "No biometric data received from server")
                return false
            }

            biometricDataInMemoryBase64 = base64BiometricList
            Log.d(TAG, "Biometric data (base64) loaded into memory (${biometricDataInMemoryBase64.size} records)")
            biometricDataInMemoryBase64.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load biometric data: ${e.message}")
            false
        }
    }

    /**
     * Identify fingerprint from ZKTeco 9500 device
     * Compares live fingerprint with stored biometric data
     * Returns true if fingerprint matches with threshold >= 55, false otherwise
     */
    suspend fun identifyFingerprint(): Boolean {
        return try {
            if (biometricDataInMemoryBase64.isEmpty()) {
                Log.e(TAG, "No biometric data in memory")
                return false
            }

            if (!prepareZKDevice()) {
                Log.e(TAG, "Failed to prepare ZKTeco device")
                return false
            }

            val liveFingerprint = captureFingerprintFromDevice()
            if (liveFingerprint == null) {
                Log.e(TAG, "Failed to capture fingerprint from device")
                return false
            }

            val matchFound = compareFingerprintWithStored(liveFingerprint)
            Log.d(TAG, "Fingerprint identification result: $matchFound")
            return matchFound
        } catch (e: Exception) {
            Log.e(TAG, "Error during fingerprint identification: ${e.message}")
            false
        }
    }

    suspend fun prepareZKDevice(): Boolean {
        if (!loadNativeLibraries()) {
            return false
        }

        if (!requestUsbPermission()) {
            return false
        }

        return try {
            if (zktecoDeviceOpened) {
                Log.d(TAG, "ZKTeco 9500 device already initialized")
                return true
            }

            Log.d(TAG, "Opening ZKTeco 9500 device...")
            if (!FingerprintSensorEx.isNativeLoaded()) {
                lastZKError = "Bibliothèques ZKTeco manquantes ou non chargées"
                Log.e(TAG, lastZKError!!)
                return false
            }
            if (FingerprintSensorEx.Init() != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                lastZKError = "Initialisation ZKTeco échouée"
                Log.e(TAG, lastZKError!!)
                return false
            }

            val deviceCount = FingerprintSensorEx.GetDeviceCount()
            if (deviceCount <= 0) {
                lastZKError = "Aucun appareil ZKTeco détecté"
                Log.e(TAG, lastZKError!!)
                return false
            }

            mhDevice = FingerprintSensorEx.OpenDevice(0)
            if (mhDevice == 0L) {
                lastZKError = "Impossible d'ouvrir le lecteur ZKTeco 9500"
                Log.e(TAG, lastZKError!!)
                return false
            }

            mhDB = FingerprintSensorEx.DBInit()
            if (mhDB == 0L) {
                lastZKError = "Impossible d'initialiser le DB ZKTeco"
                Log.e(TAG, lastZKError!!)
                closeZKDevice()
                return false
            }

            val paramValue = ByteArray(4)
            val size = intArrayOf(4)
            if (FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size) != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                Log.e(TAG, "Failed to obtain ZKTeco fingerprint width")
                closeZKDevice()
                return false
            }
            fpWidth = byteArrayToInt(paramValue)

            size[0] = 4
            if (FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size) != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                lastZKError = "Impossible de lire la hauteur d'image du ZKTeco"
                Log.e(TAG, lastZKError!!)
                closeZKDevice()
                return false
            }
            fpHeight = byteArrayToInt(paramValue)

            if (fpWidth <= 0 || fpHeight <= 0) {
                Log.e(TAG, "Invalid ZKTeco fingerprint dimensions: ${fpWidth}x${fpHeight}")
                closeZKDevice()
                return false
            }

            imgbuf = ByteArray(fpWidth * fpHeight)
            zktecoDeviceOpened = true
            Log.d(TAG, "ZKTeco 9500 device opened and ready")
            true
        } catch (e: Exception) {
            lastZKError = "Erreur interne lors de l'ouverture du lecteur ZKTeco"
            Log.e(TAG, "Failed to open ZKTeco 9500 device: ${e.message}")
            closeZKDevice()
            false
        }
    }

    fun getLastZKError(): String? = lastZKError

    private fun closeZKDevice() {
        try {
            if (mhDB != 0L) {
                FingerprintSensorEx.DBFree(mhDB)
                mhDB = 0
            }
            if (mhDevice != 0L) {
                FingerprintSensorEx.CloseDevice(mhDevice)
                mhDevice = 0
            }
            FingerprintSensorEx.Terminate()
        } catch (ignored: Exception) {
            Log.w(TAG, "Error closing ZKTeco device: ${ignored.message}")
        } finally {
            zktecoDeviceOpened = false
            imgbuf = null
        }
    }

    private fun byteArrayToInt(bytes: ByteArray): Int {
        if (bytes.size < 4) return 0
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }

    private suspend fun captureFingerprintFromDevice(): ByteArray? {
        return try {
            if (mhDevice == 0L) {
                Log.e(TAG, "ZKTeco device not opened")
                return null
            }

            Log.d(TAG, "ZKTeco 9500 prêt. Attente du doigt sur le capteur...")
            val imageBuffer = imgbuf ?: run {
                Log.e(TAG, "Image buffer is not initialized")
                return null
            }
            val template = ByteArray(2048)
            val templateLen = IntArray(1)

            while (true) {
                val result = FingerprintSensorEx.AcquireFingerprint(mhDevice, imageBuffer, template, templateLen)
                if (result < 0) {
                    Log.e(TAG, "AcquireFingerprint failed or native libs missing: result=$result")
                    return null
                }
                if (result == FingerprintSensorErrorCode.ZKFP_ERR_OK) {
                    break
                }
                delay(300)
            }

            if (templateLen[0] <= 0) {
                Log.e(TAG, "ZKTeco capture returned empty template")
                return null
            }

            Log.d(TAG, "Fingerprint captured, template length=${templateLen[0]}")
            return template.copyOf(templateLen[0])
        } catch (e: Exception) {
            Log.e(TAG, "Device communication error: ${e.message}")
            null
        }
    }

    fun clearBiometricData(): Boolean {
        return try {
            biometricDataInMemoryBase64 = emptyList()
            Log.d(TAG, "Biometric data cleared from memory")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear biometric data: ${e.message}")
            false
        }
    }

    private fun compareFingerprintWithStored(liveFingerprint: ByteArray): Boolean {
        for (storedBase64 in biometricDataInMemoryBase64) {
            val storedFingerprint = decodeBase64BiometricData(storedBase64) ?: continue
            val score = calculateMatchScore(liveFingerprint, storedFingerprint)

            Log.d(TAG, "Match score with stored fingerprint: $score")

            if (score >= THRESHOLD) {
                Log.d(TAG, "Match found with score: $score (threshold: $THRESHOLD)")
                return true
            }
        }

        Log.d(TAG, "No fingerprint match found. Required threshold: $THRESHOLD")
        return false
    }

    private fun calculateMatchScore(fingerprint1: ByteArray, fingerprint2: ByteArray): Int {
        if (fingerprint1.size != fingerprint2.size) {
            return 0
        }

        var matches = 0
        for (i in fingerprint1.indices) {
            if (fingerprint1[i] == fingerprint2[i]) {
                matches++
            }
        }

        return (matches * 100) / fingerprint1.size
    }

    fun decodeBase64BiometricData(base64String: String): ByteArray? {
        return try {
            Base64.decode(base64String, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode base64 biometric data: ${e.message}")
            null
        }
    }
}
