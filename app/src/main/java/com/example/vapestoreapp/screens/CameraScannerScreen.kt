package com.example.vapestoreapp.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

// ВАЖНО: Добавьте эту аннотацию ДО @Composable
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraScanner(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(true) }

    // Запрашиваем разрешение на камеру
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // При первом запуске проверяем разрешение
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            hasCameraPermission = true
        }
    }

    // Обработка найденного штрих-кода
    LaunchedEffect(lastScannedBarcode) {
        if (lastScannedBarcode.isNotEmpty() && isScanning) {
            isScanning = false
            // Ждем немного перед вызовом коллбека
            delay(500)
            onBarcodeScanned(lastScannedBarcode)
            // Сбрасываем через 2 секунды
            delay(2000)
            lastScannedBarcode = ""
            isScanning = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!hasCameraPermission) {
            // Если нет разрешения
            Text(
                "Для сканирования нужен доступ к камере",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            // Камера для сканирования
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // Настройка превью
                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            // Настройка анализа изображения
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            // Сканер штрих-кодов
                            val scanner: BarcodeScanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(
                                Executors.newSingleThreadExecutor()
                            ) { imageProxy ->
                                if (!isScanning) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                val mediaImage = imageProxy.image

                                if (mediaImage != null) {
                                    // Используем полное имя InputImage
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        rotationDegrees
                                    )

                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { value ->
                                                    // Фильтруем только цифры и проверяем длину
                                                    val digitsOnly = value.filter { it.isDigit() }
                                                    if (digitsOnly.length >= 8 && lastScannedBarcode != digitsOnly) {
                                                        lastScannedBarcode = digitsOnly
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            // Выбираем заднюю камеру
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Связываем с жизненным циклом
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Направляющая рамка для сканирования
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                val width = size.width
                val height = size.height
                val frameSize = minOf(width, height) * 0.7f
                val left = (width - frameSize) / 2
                val top = (height - frameSize) / 2

                // Рамка сканирования
                drawRect(
                    color = Color.Green.copy(alpha = 0.3f),
                    topLeft = Offset(left, top),
                    size = Size(frameSize, frameSize),
                    style = Stroke(width = 4f)
                )

                // Уголки рамки
                val cornerSize = 40f

                // Левый верхний угол
                drawLine(
                    color = Color.Green,
                    start = Offset(left, top),
                    end = Offset(left + cornerSize, top),
                    strokeWidth = 8f
                )
                drawLine(
                    color = Color.Green,
                    start = Offset(left, top),
                    end = Offset(left, top + cornerSize),
                    strokeWidth = 8f
                )

                // Правый верхний угол
                drawLine(
                    color = Color.Green,
                    start = Offset(left + frameSize, top),
                    end = Offset(left + frameSize - cornerSize, top),
                    strokeWidth = 8f
                )
                drawLine(
                    color = Color.Green,
                    start = Offset(left + frameSize, top),
                    end = Offset(left + frameSize, top + cornerSize),
                    strokeWidth = 8f
                )

                // Левый нижний угол
                drawLine(
                    color = Color.Green,
                    start = Offset(left, top + frameSize),
                    end = Offset(left + cornerSize, top + frameSize),
                    strokeWidth = 8f
                )
                drawLine(
                    color = Color.Green,
                    start = Offset(left, top + frameSize),
                    end = Offset(left, top + frameSize - cornerSize),
                    strokeWidth = 8f
                )

                // Правый нижний угол
                drawLine(
                    color = Color.Green,
                    start = Offset(left + frameSize, top + frameSize),
                    end = Offset(left + frameSize - cornerSize, top + frameSize),
                    strokeWidth = 8f
                )
                drawLine(
                    color = Color.Green,
                    start = Offset(left + frameSize, top + frameSize),
                    end = Offset(left + frameSize, top + frameSize - cornerSize),
                    strokeWidth = 8f
                )
            }

            // Индикатор сканирования
            if (lastScannedBarcode.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        "📷 Найден: $lastScannedBarcode",
                        color = Color.Green,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}