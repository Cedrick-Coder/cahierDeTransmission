package com.zkteco.biometric

import android.content.Context
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference
import android.os.ParcelFileDescriptor

/**
 * Compatibility shim: use SDK Java API (FingerprintFactory / FingerprintSensor)
 * to provide the same API surface used by existing code (Init, OpenDevice, AcquireFingerprint...).
 * This avoids UnsatisfiedLinkError when native JNI symbols are not present in the provided .so files.
 */
class FingerprintSensorEx {
    companion object {
        private const val TAG = "FingerprintSensorExShim"
        private var loaded = false

        // If the Java SDK classes (jars) are present, treat as "native loaded" for compatibility
        private val javaSdkAvailable: Boolean = try {
            Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintFactory")
            true
        } catch (_: Throwable) {
            try {
                Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor")
                true
            } catch (_: Throwable) {
                false
            }
        }

        // sensor instance created by factory
        private var sensorInstance: Any? = null
        private var appContext: Context? = null

        fun isNativeLoaded(): Boolean = loaded || javaSdkAvailable

        @JvmStatic fun Init(context: Context? = null): Int {
            try {
                // obtain application context when not passed explicitly
                val ctx = context ?: try {
                    val at = Class.forName("android.app.ActivityThread")
                    val currentApp = at.getMethod("currentApplication").invoke(null)
                    currentApp as Context
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to get application context: ${e.message}")
                    null
                }
                appContext = ctx

                // Try to initialize higher-level fingerprint service if present
                try {
                    val fsClass = Class.forName("com.zkteco.zkfinger.FingerprintService")
                    val initMethod = fsClass.methods.firstOrNull { it.name == "init" }
                    initMethod?.invoke(null, ctx)
                    Log.d(TAG, "FingerprintService initialized")
                } catch (t: Throwable) {
                    Log.d(TAG, "FingerprintService not available or init failed: ${t.message}")
                }

                // Create a FingerprintSensor via FingerprintFactory if available
                try {
                    val factory = Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintFactory")
                    Log.d(TAG, "FingerprintFactory found")
                    val transportType = Class.forName("com.zkteco.android.biometric.core.device.TransportType")
                    Log.d(TAG, "TransportType found")

                    // List all available enum values
                    val allFields = transportType.fields.map { it.name }
                    Log.d(TAG, "TransportType fields: $allFields")

                    // Try to get enum values using values() method first
                    val transportEnum = try {
                        val valuesMethod = transportType.getMethod("values")
                        val enumArray = valuesMethod.invoke(null) as Array<*>
                        Log.d(TAG, "TransportType.values() returned ${enumArray.size} enums: ${enumArray.map { it.toString() }}")
                        enumArray.firstOrNull()
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to call values() method: ${e.message}")
                        // Fallback: try specific enum names
                        val transportNames = listOf("USBHID", "USBMCU", "USBSCSI", "USB", "NONE")
                        transportNames.mapNotNull { name ->
                            try {
                                val field = transportType.getField(name)
                                Log.d(TAG, "Found TransportType.$name")
                                field.get(null)
                            } catch (e: Throwable) {
                                Log.d(TAG, "TransportType.$name not found: ${e.message}")
                                null
                            }
                        }.firstOrNull()
                    }

                    if (transportEnum == null) {
                        Log.e(TAG, "No supported TransportType enum found, will try without transport type")
                        // Try to create sensor without specifying transport type (use valueOf or default)
                        sensorInstance = try {
                            val sensorClass = Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor")
                            val ctor = sensorClass.getConstructor(Context::class.java, transportType, Map::class.java)
                            Log.d(TAG, "Will instantiate FingerprintSensor with null transport type")
                            ctor.newInstance(ctx, null, null)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to instantiate with null transport: ${t.message}")
                            null
                        }
                    } else {
                        Log.d(TAG, "Using TransportType: $transportEnum")
                        val createMethod = listOfNotNull(
                            factory.methods.firstOrNull { it.name == "createFingerprintSensor" && it.parameterTypes.size == 3 },
                            factory.methods.firstOrNull { it.name == "createFingerprintSensor2" && it.parameterTypes.size == 3 }
                        ).firstOrNull()

                        sensorInstance = if (createMethod != null) {
                            try {
                                val res = createMethod.invoke(null, ctx, transportEnum, null)
                                Log.d(TAG, "FingerprintSensor created via factory: ${res != null}")
                                res
                            } catch (t: Throwable) {
                                Log.e(TAG, "Failed to invoke factory create method: ${t.message}, trying direct instantiation")
                                try {
                                    val sensorClass = Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor")
                                    val ctor = sensorClass.getConstructor(Context::class.java, transportType, Map::class.java)
                                    val res = ctor.newInstance(ctx, transportEnum, null)
                                    Log.d(TAG, "FingerprintSensor instantiated directly: ${res != null}")
                                    res
                                } catch (t2: Throwable) {
                                    Log.e(TAG, "Direct instantiation also failed: ${t2.message}")
                                    null
                                }
                            }
                        } else {
                            try {
                                val sensorClass = Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor")
                                val ctor = sensorClass.getConstructor(Context::class.java, transportType, Map::class.java)
                                val res = ctor.newInstance(ctx, transportEnum, null)
                                Log.d(TAG, "FingerprintSensor instantiated directly: ${res != null}")
                                res
                            } catch (t: Throwable) {
                                Log.e(TAG, "Failed to instantiate FingerprintSensor directly: ${t.message}")
                                null
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "FingerprintFactory setup failed: ${t.message}")
                    sensorInstance = null
                }

                loaded = true
                Log.d(TAG, "Init completed, sensorInstance=${sensorInstance != null}")
                return 0
            } catch (e: Throwable) {
                Log.e(TAG, "Init error: ${e.message}")
                loaded = false
                return -1
            }
        }

        @JvmStatic fun GetDeviceCount(): Int {
            return try {
                // If sensor was successfully created, return 1
                if (sensorInstance != null) {
                    Log.d(TAG, "GetDeviceCount: sensorInstance is available")
                    return 1
                }
                // Fallback: if SDK jar is available, assume 1 device (even if not yet opened)
                if (javaSdkAvailable) {
                    Log.d(TAG, "GetDeviceCount: javaSdkAvailable, returning 1")
                    return 1
                }
                0
            } catch (t: Throwable) {
                Log.e(TAG, "GetDeviceCount error: ${t.message}")
                0
            }
        }

        @JvmStatic fun OpenDevice(fd: Int): Long {
            try {
                Log.d(TAG, "OpenDevice called with fd=$fd, sensorInstance=${sensorInstance != null}")
                
                if (sensorInstance != null) {
                    val cls = sensorInstance!!.javaClass
                    Log.d(TAG, "Sensor class: ${cls.simpleName}")

                    // Collect candidate methods that might accept an FD or descriptor
                    val openCandidates = cls.methods.filter { method ->
                        method.parameterTypes.size == 1 && (
                            method.parameterTypes[0] == Integer.TYPE ||
                            method.parameterTypes[0] == java.lang.Integer::class.java ||
                            method.parameterTypes[0] == java.io.FileDescriptor::class.java ||
                            method.parameterTypes[0].name == "android.os.ParcelFileDescriptor" ||
                            method.parameterTypes[0].name.contains("Usb", true) ||
                            method.parameterTypes[0] == String::class.java
                        )
                    }
                    Log.d(TAG, "Found ${openCandidates.size} candidate open methods: ${openCandidates.map { it.name + it.parameterTypes.joinToString(",") { p-> p.simpleName } }}")

                    // Try invoking candidates with appropriate argument types
                    for (method in openCandidates) {
                        try {
                            val paramType = method.parameterTypes[0]
                            Log.d(TAG, "Trying ${method.name} with param type ${paramType.name}")
                            val arg: Any? = when {
                                paramType == Integer.TYPE || paramType == java.lang.Integer::class.java -> fd
                                paramType == java.io.FileDescriptor::class.java -> {
                                    try {
                                        ParcelFileDescriptor.adoptFd(fd).fileDescriptor
                                    } catch (adoptEx: Throwable) {
                                        Log.w(TAG, "adoptFd failed: ${adoptEx.message}")
                                        null
                                    }
                                }
                                paramType.name == "android.os.ParcelFileDescriptor" -> {
                                    try {
                                        ParcelFileDescriptor.adoptFd(fd)
                                    } catch (adoptEx: Throwable) {
                                        Log.w(TAG, "adoptFd failed: ${adoptEx.message}")
                                        null
                                    }
                                }
                                paramType == String::class.java -> {
                                    // unknown path; try empty string as a diagnostic
                                    ""
                                }
                                else -> null
                            }

                            val res = if (arg != null) method.invoke(sensorInstance!!, arg) else method.invoke(sensorInstance!!)
                            Log.d(TAG, "Method ${method.name} returned: $res (type: ${res?.javaClass?.simpleName})")
                            return if (res is Number) res.toLong() else if (res != null) 1L else 0L
                        } catch (e: Exception) {
                            Log.w(TAG, "Method ${method.name} failed: ${e.message}\n${e.stackTraceToString()}")
                        }
                    }

                    // Priority 2: Try openDeviceByAndroidHost (may work without fd)
                    val openByHost = cls.methods.firstOrNull {
                        it.name == "openDeviceByAndroidHost" || it.name == "openDeviceByHost" || it.name == "tryOpenDevice"
                    }
                    if (openByHost != null) {
                        try {
                            Log.d(TAG, "Trying ${openByHost.name}")
                            val res = if (openByHost.parameterTypes.isEmpty()) {
                                openByHost.invoke(sensorInstance!!)
                            } else {
                                openByHost.invoke(sensorInstance!!, null as Any?)
                            }
                            Log.d(TAG, "${openByHost.name} returned: $res")
                            return if (res is Number) res.toLong() else if (res != null) 1L else 0L
                        } catch (e: Exception) {
                            Log.w(TAG, "${openByHost.name} failed: ${e.message}\n${e.stackTraceToString()}")
                        }
                    }

                    // Priority 3: Try no-arg open()
                    val openNoArg = cls.methods.firstOrNull {
                        it.name == "open" && it.parameterTypes.isEmpty()
                    }
                    if (openNoArg != null) {
                        try {
                            Log.d(TAG, "Trying open() no-arg")
                            val res = openNoArg.invoke(sensorInstance!!)
                            Log.d(TAG, "open() returned: $res")
                            return if (res is Number) res.toLong() else if (res != null) 1L else 0L
                        } catch (e: Exception) {
                            Log.w(TAG, "open() no-arg failed: ${e.message}\n${e.stackTraceToString()}")
                        }
                    }

                    Log.e(TAG, "No suitable open method found on ${cls.simpleName}")
                    Log.d(TAG, "Available methods: ${cls.methods.map { m -> m.name + m.parameterTypes.joinToString(",") { p-> p.simpleName } }.distinct().joinToString("; ")}")
                } else {
                    Log.w(TAG, "OpenDevice: sensorInstance is null, cannot proceed")
                    return 0L
                }
            } catch (t: Throwable) {
                Log.e(TAG, "OpenDevice error: ${t.message}\n${t.stackTraceToString()}")
            }
            return 0L
        }

        @JvmStatic fun DBInit(): Long {
            // SDK Java layer manages templates differently; return a dummy non-zero handle
            return 1L
        }

        @JvmStatic fun GetParameters(device: Long, paramType: Int, paramValue: ByteArray, size: IntArray): Int {
            try {
                val sensor = sensorInstance ?: return -1
                val cls = sensor.javaClass
                // try fields imageWidth / imageHeight
                if (paramType == 1) {
                    val w = try { cls.getDeclaredField("imageWidth").let { it.isAccessible = true; it.getInt(sensor) } } catch (_: Throwable) { 256 }
                    val v = intToByteArray(w)
                    System.arraycopy(v, 0, paramValue, 0, Math.min(v.size, paramValue.size))
                    return 0
                }
                if (paramType == 2) {
                    val h = try { cls.getDeclaredField("imageHeight").let { it.isAccessible = true; it.getInt(sensor) } } catch (_: Throwable) { 360 }
                    val v = intToByteArray(h)
                    System.arraycopy(v, 0, paramValue, 0, Math.min(v.size, paramValue.size))
                    return 0
                }
            } catch (t: Throwable) {
                Log.w(TAG, "GetParameters fallback: ${t.message}")
            }
            return -1
        }

        @JvmStatic fun ExtractFromImage(mhDB: Long, path: String, timeout: Int, fpTemplate: ByteArray, sizeFPTemp: IntArray): Int {
            // Not implemented in Java shim — delegate to higher-level APIs if needed
            return -1
        }

        @JvmStatic fun DBAdd(mhDB: Long, fid: Int, fpTemplate: ByteArray): Int { return -1 }
        @JvmStatic fun DBIdentify(mhDB: Long, fpTemplate: ByteArray, fid: IntArray, score: IntArray): Int { return -1 }
        @JvmStatic fun DBMatch(mhDB: Long, template1: ByteArray, template2: ByteArray): Int { return -1 }
        @JvmStatic fun DBFree(mhDB: Long): Int { return 0 }

        @JvmStatic fun CloseDevice(device: Long): Int {
            try {
                val sensor = sensorInstance ?: return -1
                val cls = sensor.javaClass
                val closeMethod = cls.methods.firstOrNull { it.name == "close" || it.name == "stop" }
                closeMethod?.invoke(sensor)
                sensorInstance = null
                return 0
            } catch (t: Throwable) {
                Log.w(TAG, "CloseDevice error: ${t.message}")
                return -1
            }
        }

        @JvmStatic fun Terminate(): Int {
            try {
                sensorInstance = null
                return 0
            } catch (t: Throwable) {
                return -1
            }
        }

        @JvmStatic fun AcquireFingerprint(device: Long, imgbuf: ByteArray, template: ByteArray, templateLen: IntArray): Int {
            try {
                val sensor = sensorInstance ?: return -1
                val cls = sensor.javaClass

                // find listener interface
                val listenerClass = try { Class.forName("com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener") } catch (_: Throwable) { null }

                val resultRef = AtomicReference<ByteArray?>(null)

                if (listenerClass != null) {
                    val handler = InvocationHandler { _: Any, method: Method, args: Array<Any>? ->
                        if (method.name == "extractOK") {
                            // try to pick first byte[] arg
                            args?.forEach { a ->
                                if (a is ByteArray) {
                                    resultRef.set(a)
                                }
                            }
                        }
                        null
                    }
                    val proxy = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass), handler)

                    // set listener if available
                    val setListener = cls.methods.firstOrNull { it.parameterTypes.size == 1 && it.parameterTypes[0].name.contains("FingerprintCaptureListener") }
                    setListener?.invoke(sensor, proxy)
                }

                // start capture
                val start = cls.methods.firstOrNull { it.name.contains("startCapture") || it.name == "start" }
                start?.invoke(sensor)

                // wait for result (simple polling)
                var waited = 0
                while (waited < 30000) {
                    val res = resultRef.get()
                    if (res != null) {
                        // copy to template
                        val len = Math.min(template.size, res.size)
                        System.arraycopy(res, 0, template, 0, len)
                        templateLen[0] = len
                        return 0
                    }
                    Thread.sleep(200)
                    waited += 200
                }
                return -1
            } catch (t: Throwable) {
                Log.e(TAG, "AcquireFingerprint error: ${t.message}")
                return -1
            }
        }

        private fun intToByteArray(value: Int): ByteArray {
            return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte(), ((value shr 16) and 0xFF).toByte(), ((value shr 24) and 0xFF).toByte())
        }
    }
}
