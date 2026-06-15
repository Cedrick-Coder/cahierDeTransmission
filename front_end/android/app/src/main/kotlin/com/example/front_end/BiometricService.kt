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
import com.example.front_end.ZKTecoBiometricManager
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
    private val zktecoManager = ZKTecoBiometricManager(activity.applicationContext)
    private var zktecoDeviceOpened: Boolean = false
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
            val loaded = ZKTecoBiometricManager.loadNativeLibraries()
            if (!loaded) {
                lastZKError = "Les bibliothèques natives ZKTeco n'ont pas pu être chargées"
                Log.e(TAG, lastZKError!!)
                false
            } else {
                isNativeLibrariesLoaded = true
                Log.d(TAG, "Native ZKTeco libraries loaded successfully")
                true
            }
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
        val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = findUsbDevice(usbManager)

        if (usbDevice == null) {
            lastZKError = "Aucun périphérique USB trouvé"
            Log.e(TAG, lastZKError!!)
            usbPermissionGranted = false
            return false
        }

        // Always recheck permission validity, even if flag was previously true
        // (device may have been disconnected/reconnected, permission may have expired)
        if (usbManager.hasPermission(usbDevice)) {
            usbPermissionGranted = true
            Log.d(TAG, "USB permission already granted for ${usbDevice.deviceName}")
            return true
        }

        // Permission not granted, request it
        usbPermissionGranted = false
        Log.d(TAG, "Permission USB not found, requesting for ${usbDevice.deviceName}")
        val requestResult = requestUsbPermissionForDevice(usbManager, usbDevice)
        if (requestResult) {
            usbPermissionGranted = true
            Log.d(TAG, "USB permission granted for ${usbDevice.deviceName}")
        } else {
            lastZKError = "Permission USB refusée"
            Log.e(TAG, lastZKError!!)
            usbPermissionGranted = false
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
                Log.d(TAG, "ZKTeco device already initialized")
                return true
            }

            val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbDevice = findUsbDevice(usbManager)
            if (usbDevice == null) {
                lastZKError = "Aucun périphérique USB ZKTeco trouvé"
                Log.e(TAG, lastZKError!!)
                return false
            }

            Log.d(TAG, "Opening ZKTeco device via Java manager...")
            if (!zktecoManager.openDevice(usbDevice)) {
                lastZKError = zktecoManager.getLastError() ?: "Impossible d'ouvrir le périphérique ZKTeco"
                Log.e(TAG, lastZKError!!)
                return false
            }

            zktecoDeviceOpened = true
            Log.d(TAG, "ZKTeco device opened and ready")
            true
        } catch (e: Exception) {
            lastZKError = "Erreur interne lors de l'ouverture du lecteur ZKTeco: ${e.message}"
            Log.e(TAG, lastZKError!!)
            return false
        }
    }

    fun getLastZKError(): String? = lastZKError

    private fun closeZKDevice() {
        try {
            zktecoManager.closeDevice()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ZKTeco device: ${e.message}")
        } finally {
            zktecoDeviceOpened = false
            usbPermissionGranted = false
        }
    }

    private suspend fun captureFingerprintFromDevice(): ByteArray? {
        return try {
            if (!zktecoDeviceOpened) {
                Log.e(TAG, "ZKTeco device is not prepared")
                return null
            }

            Log.d(TAG, "ZKTeco device ready. Waiting for finger placement...")
            val template = zktecoManager.captureFingerprint()
            if (template == null || template.isEmpty()) {
                Log.e(TAG, "Failed to capture fingerprint from ZKTeco device")
                return null
            }

            Log.d(TAG, "Fingerprint template captured, length=${template.size}")
            return template
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
        if (liveFingerprint.isEmpty()) {
            Log.e(TAG, "Live fingerprint template is empty")
            return false
        }
        if (biometricDataInMemoryBase64.isEmpty()) {
            Log.e(TAG, "No biometric data in memory to compare")
            return false
        }

        val matchFound = zktecoManager.compareAgainstStoredTemplates(liveFingerprint, biometricDataInMemoryBase64, THRESHOLD)
        Log.d(TAG, "Fingerprint identification result: $matchFound")
        return matchFound
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
