package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
//import org.tensorflow.lite.examples.objectdetection.EmotionDetector
//import android.content.Context
//import android.graphics.Bitmap

data class DetectionWithEmotion(
    val boundingBox: RectF,
    val categories: List<Category>,
    val emotion: String
)


class ObjectDetectorHelper(
  var threshold: Float = 0.5f,
  var numThreads: Int = 2,
  var maxResults: Int = 30,
  var currentDelegate: Int = 0,
  var currentModel: Int = 0,
  val context: Context,
  val objectDetectorListener: DetectorListener?
) {

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    private var objectDetector: ObjectDetector? = null
    private val emotionDetector = EmotionDetector(context) // Instantiate EmotionDetector

    fun detectEmotion(bitmap: Bitmap): String {
        return emotionDetector.detectEmotion(bitmap)
    }

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {mmccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccmccccc ^^^^^^^66
                    objectDetectorListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                else -> "mobilenetv1.tflite"
            }

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime



      //  objectDetectorListener?.onResults(
       //     results,
         //   inferenceTime,
         //   tensorImage.height,
          //  tensorImage.width)


        val detectedBitmaps = mutableListOf<Bitmap>()


val detectionWithEmotions = mutableListOf<DetectionWithEmotion>()
        // Count persons detected
        var personCount = 0


        results?.forEach { detection ->
        if (detection.categories.any { it.label == "person" }) {
            val boundingBox = detection.boundingBox

                val left = boundingBox.left.coerceIn(0f, image.width.toFloat()).toInt()
            val top = boundingBox.top.coerceIn(0f, image.height.toFloat()).toInt()
            val right = boundingBox.right.coerceIn(0f, image.width.toFloat()).toInt()
            val bottom = boundingBox.bottom.coerceIn(0f, image.height.toFloat()).toInt()

            val width = (right - left).coerceAtLeast(1)  // Ensure width is at least 1 pixel
            val height = (bottom - top).coerceAtLeast(1)  // Ensure height is at least 1 pixel


                var detectedEmotion = "Unknown"
                if (width > 0 && height > 0) {
                    val croppedBitmap = Bitmap.createBitmap(image, left, top, width, height)
                    detectedEmotion = emotionDetector.detectEmotion(croppedBitmap)
                    Log.d("EmotionDetection", "Detected Emotion: $detectedEmotion")
                }

                detectionWithEmotions.add(
                    DetectionWithEmotion(
                        boundingBox = detection.boundingBox,
                        categories = detection.categories,
                        emotion = detectedEmotion
                    )
                )
            personCount++
        }
                // Print each detection's label and score
                results?.forEach { detection ->
                detection.categories.forEach { category ->
                Log.d("Detection Results", "Label: ${category.label}, Score: ${category.score}")
                    }
                        }
    }
objectDetectorListener?.onResults(
            detectionWithEmotions,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
        )
    // Notify the listener about the person count
    objectDetectorListener?.onPersonCount(personCount)


    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
          results: List<DetectionWithEmotion>?,
          inferenceTime: Long,
          imageHeight: Int,
          imageWidth: Int
        )
         fun onPersonCount(count: Int) // New method for person count
    
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
    }
}