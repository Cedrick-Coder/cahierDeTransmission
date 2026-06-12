package com.example.front_end

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class BiometricApiService {
    companion object {
        private const val TAG = "BiometricApiService"
        private const val BASE_URL = "http://192.168.0.150:8000/api"
    }

    suspend fun fetchBiometricDataFromServer(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/personne-autorisee/biometrics")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }
                    reader.close()
                    inputStream.close()

                    // Parse JSON response to extract base64 biometric data
                    val jsonArray = JSONArray(response)
                    val biometricDataList = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val biomData = obj.optString("biomData", null)
                        if (biomData != null) {
                            biometricDataList.add(biomData)
                        }
                    }

                    Log.d(TAG, "Successfully fetched ${biometricDataList.size} biometric records")
                    biometricDataList
                } else {
                    Log.e(TAG, "Failed to fetch biometric data. Response code: $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching biometric data from server: ${e.message}")
                emptyList()
            }
        }
    }
}
