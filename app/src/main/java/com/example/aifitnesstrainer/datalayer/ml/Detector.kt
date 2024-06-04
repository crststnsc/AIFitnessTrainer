package com.example.aifitnesstrainer.datalayer.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import com.example.aifitnesstrainer.datalayer.models.BoundingBox
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val detectorListener: DetectorListener,
) {
    private var interpreter: Interpreter? = null

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun setup(isGpu: Boolean = true) {

        if (interpreter != null) {
            close()
        }

        val options = if (isGpu) {
            val compatList = CompatibilityList()

            Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply{
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]

        // If in case input shape is in format of [1, 3, ..., ...]
        if (inputShape[1] == 3) {
            tensorWidth = inputShape[2]
            tensorHeight = inputShape[3]
        }

        numChannel = outputShape[1]
        numElements = outputShape[2]
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        interpreter ?: return
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        val paddedBitmap = createPaddedBitmap(frame, tensorWidth, tensorHeight)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(paddedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)

        val boundingBoxes = decodeOutput(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (boundingBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        }

        detectorListener.onDetect(boundingBoxes, inferenceTime)
    }

    private fun decodeOutput(array: FloatArray): List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()
        for (c in 0 until numElements) {
            val cnf = array[c + numElements * 4]
            if (cnf > CONFIDENCE_THRESHOLD) {
                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)
                if (x1 <= 0F || x1 >= tensorWidth) continue
                if (y1 <= 0F || y1 >= tensorHeight) continue
                if (x2 <= 0F || x2 >= tensorWidth) continue
                if (y2 <= 0F || y2 >= tensorHeight) continue

                // Extract keypoints
                val keypoints = mutableListOf<KeyPoint>()
                for (k in 0 until 16) {
                    var kx = array[c + numElements * (5 + k * 2)]
                    var ky = array[c + numElements * (5 + k * 2 + 1)]

                    kx /= tensorWidth
                    ky /= tensorHeight

                    keypoints.add(KeyPoint(kx, ky))
                }

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h, cnf = cnf,
                        keyPoints = keypoints
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    private fun getScaledDimensions(originalWidth: Int, originalHeight: Int, targetWidth: Int, targetHeight: Int): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight
        var newWidth = targetWidth
        var newHeight = (targetWidth / aspectRatio).toInt()

        if (newHeight > targetHeight) {
            newHeight = targetHeight
            newWidth = (targetHeight * aspectRatio).toInt()
        }

        return Pair(newWidth, newHeight)
    }

    private fun createPaddedBitmap(originalBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val (newWidth, newHeight) = getScaledDimensions(originalBitmap.width, originalBitmap.height, targetWidth, targetHeight)

        // Create a new bitmap with the target dimensions
        val paddedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        val paint = Paint().apply { color = Color.BLACK } // Change the padding color as needed

        // Draw the padding
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), paint)

        // Calculate the top and left position to center the scaled bitmap
        val left = (targetWidth - newWidth) / 2f
        val top = (targetHeight - newHeight) / 2f

        // Draw the scaled bitmap onto the canvas
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        canvas.drawBitmap(scaledBitmap, left, top, null)

        return paddedBitmap
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.7F
        private const val IOU_THRESHOLD = 0.5F
    }
}

data class KeyPoint(
    var x: Float,
    val y: Float
)
