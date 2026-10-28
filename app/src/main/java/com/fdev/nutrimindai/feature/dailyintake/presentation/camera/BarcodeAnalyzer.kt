package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

/**
 * CameraX ImageAnalysis Analyzer powered by Google ML Kit Barcode Scanning.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @Volatile
    var isScanningEnabled: Boolean = true

    private var lastScannedCode: String = ""
    private var lastScannedTimestamp: Long = 0L
    private val throttleMs = 2000L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isScanningEnabled) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!isScanningEnabled) return@addOnSuccessListener

                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue?.trim() ?: continue
                    if (rawValue.isNotBlank()) {
                        if (rawValue != lastScannedCode || (currentTime - lastScannedTimestamp > throttleMs)) {
                            lastScannedCode = rawValue
                            lastScannedTimestamp = currentTime
                            Timber.d("Barcode detected: $rawValue (Format: ${barcode.format})")
                            onBarcodeDetected(rawValue)
                            break
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Timber.e(error, "Barcode scanning failed")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
