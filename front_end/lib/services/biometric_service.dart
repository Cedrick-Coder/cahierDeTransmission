import 'dart:io';
import 'package:flutter/services.dart';

class BiometricService {
  static const platform = MethodChannel('com.smmec.equipeINFO.cahierDeTransmisssion/biometric');

  static bool get _isAndroid => Platform.isAndroid;

  /// Initiate fingerprint identification
  /// Returns true if fingerprint matched, false otherwise
  static Future<bool> identifyFingerprint() async {
    if (!_isAndroid) {
      print('Biometric identification is only supported on Android. Current platform: ${Platform.operatingSystem}');
      return false;
    }

    try {
      final bool result = await platform.invokeMethod<bool>('identifyFingerprint') ?? false;
      return result;
    } on PlatformException catch (e) {
      print("Failed to identify fingerprint: '${e.message}'.");
      return false;
    }
  }

  /// Get biometric data from PersonneAutorisee table
  /// Returns true if data loaded successfully
  static Future<bool> loadBiometricData() async {
    if (!_isAndroid) {
      print('Biometric data loading is only supported on Android. Current platform: ${Platform.operatingSystem}');
      return false;
    }

    try {
      final bool result = await platform.invokeMethod<bool>('loadBiometricData') ?? false;
      return result;
    } on PlatformException catch (e) {
      print("Failed to load biometric data: '${e.message}'.");
      return false;
    }
  }

  /// Prepare the ZKTeco 9500 device before fingerprint capture
  static Future<bool> prepareZKDevice() async {
    if (!_isAndroid) {
      print('Device preparation is only supported on Android. Current platform: ${Platform.operatingSystem}');
      return false;
    }

    try {
      final bool result = await platform.invokeMethod<bool>('prepareZKDevice') ?? false;
      return result;
    } on PlatformException catch (e) {
      print("Failed to prepare ZKTeco device: '${e.message}'.");
      return false;
    }
  }

  /// Returns the last ZKTeco preparation error message from native code
  static Future<String?> getZKError() async {
    if (!_isAndroid) {
      return 'Unsupported platform: ${Platform.operatingSystem}';
    }
    try {
      final String? error = await platform.invokeMethod<String>('getZKError');
      return error;
    } on PlatformException catch (e) {
      print("Failed to retrieve ZKTeco error: '${e.message}'.");
      return null;
    }
  }

  /// Clear biometric data from memory
  static Future<bool> clearBiometricData() async {
    if (!_isAndroid) {
      print('Biometric data clearing is only supported on Android. Current platform: ${Platform.operatingSystem}');
      return false;
    }

    try {
      final bool result = await platform.invokeMethod<bool>('clearBiometricData') ?? false;
      return result;
    } on PlatformException catch (e) {
      print("Failed to clear biometric data: '${e.message}'.");
      return false;
    }
  }
}
