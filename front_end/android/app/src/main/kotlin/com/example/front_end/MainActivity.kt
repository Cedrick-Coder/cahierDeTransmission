package com.example.front_end

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.smmec.equipeINFO.cahierDeTransmisssion/biometric"
    private lateinit var biometricService: BiometricService

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        biometricService = BiometricService(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "loadBiometricData" -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val success = biometricService.loadBiometricData()
                                result.success(success)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "loadBiometricData error", e)
                                result.success(false)
                            }
                        }
                    }
                    "prepareZKDevice" -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val success = biometricService.prepareZKDevice()
                                result.success(success)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "prepareZKDevice error", e)
                                result.success(false)
                            }
                        }
                    }
                    "identifyFingerprint" -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val success = biometricService.identifyFingerprint()
                                result.success(success)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "identifyFingerprint error", e)
                                result.success(false)
                            }
                        }
                    }
                    "getZKError" -> {
                        try {
                            val error = biometricService.getLastZKError()
                            result.success(error)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "getZKError error", e)
                            result.success(null)
                        }
                    }
                    "requestUsbPermission" -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val success = biometricService.requestUsbPermission()
                                result.success(success)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "requestUsbPermission error", e)
                                result.success(false)
                            }
                        }
                    }
                    "initializeBiometricManager" -> {
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val success = biometricService.initializeBiometricManager()
                                result.success(success)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "initializeBiometricManager error", e)
                                result.success(false)
                            }
                        }
                    }
                    "clearBiometricData" -> {
                        val success = biometricService.clearBiometricData()
                        result.success(success)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
    }
}

