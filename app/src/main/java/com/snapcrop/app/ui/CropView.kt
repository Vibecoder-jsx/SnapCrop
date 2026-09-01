package com.snapcrop.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

private enum class DragHandle {
    NONE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT,
    CENTER
}

@Composable
fun CropView(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onCropRectCalculated: (getBitmap: () -> Bitmap) -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var imageDisplayRect by remember { mutableStateOf(Rect.Zero) }
    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var isDragging by remember { mutableStateOf(false) }

    // Generous thumb-friendly touch zones
    val cornerRadiusPx = with(density) { 68.dp.toPx() }
    val edgeThresholdPx = with(density) { 80.dp.toPx() }
    val minCropSizePx = with(density) { 60.dp.toPx() }

    // Initialize layout bounds and crop rect to 100% full frame
    LaunchedEffect(canvasSize, bitmap) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val scale = min(
                canvasSize.width / bitmap.width.toFloat(),
                canvasSize.height / bitmap.height.toFloat()
            )
            val dispWidth = bitmap.width * scale
            val dispHeight = bitmap.height * scale
            val left = (canvasSize.width - dispWidth) / 2f
            val top = (canvasSize.height - dispHeight) / 2f

            val fullRect = Rect(left, top, left + dispWidth, top + dispHeight)
            imageDisplayRect = fullRect
            cropRect = fullRect
        }
    }

    // Pass crop calculation lambda
    val currentImageDisplayRect by rememberUpdatedState(imageDisplayRect)
    val currentCropRect by rememberUpdatedState(cropRect)

    LaunchedEffect(currentCropRect, currentImageDisplayRect, bitmap) {
        onCropRectCalculated {
            val disp = currentImageDisplayRect
            val crop = currentCropRect
            if (disp.width <= 0 || disp.height <= 0) {
                bitmap
            } else {
                val scaleX = bitmap.width.toFloat() / disp.width
                val scaleY = bitmap.height.toFloat() / disp.height

                val cropLeft = ((crop.left - disp.left) * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                val cropTop = ((crop.top - disp.top) * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                val cropWidth = (crop.width * scaleX).toInt().coerceIn(1, bitmap.width - cropLeft)
                val cropHeight = (crop.height * scaleY).toInt().coerceIn(1, bitmap.height - cropTop)

                if (cropLeft == 0 && cropTop == 0 && cropWidth == bitmap.width && cropHeight == bitmap.height) {
                    bitmap
                } else {
                    Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val hit = getHitHandle(offset, currentCropRect, cornerRadiusPx, edgeThresholdPx)
                        if (hit != DragHandle.NONE) {
                            isDragging = true
                            activeHandle = hit
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        activeHandle = DragHandle.NONE
                    },
                    onDragCancel = {
                        isDragging = false
                        activeHandle = DragHandle.NONE
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val bound = currentImageDisplayRect
                        var l = currentCropRect.left
                        var t = currentCropRect.top
                        var r = currentCropRect.right
                        var b = currentCropRect.bottom

                        val dx = dragAmount.x
                        val dy = dragAmount.y

                        when (activeHandle) {
                            DragHandle.TOP_LEFT -> {
                                l = (l + dx).coerceIn(bound.left, r - minCropSizePx)
                                t = (t + dy).coerceIn(bound.top, b - minCropSizePx)
                            }
                            DragHandle.TOP_RIGHT -> {
                                r = (r + dx).coerceIn(l + minCropSizePx, bound.right)
                                t = (t + dy).coerceIn(bound.top, b - minCropSizePx)
                            }
                            DragHandle.BOTTOM_LEFT -> {
                                l = (l + dx).coerceIn(bound.left, r - minCropSizePx)
                                b = (b + dy).coerceIn(t + minCropSizePx, bound.bottom)
                            }
                            DragHandle.BOTTOM_RIGHT -> {
                                r = (r + dx).coerceIn(l + minCropSizePx, bound.right)
                                b = (b + dy).coerceIn(t + minCropSizePx, bound.bottom)
                            }
                            DragHandle.TOP -> {
                                t = (t + dy).coerceIn(bound.top, b - minCropSizePx)
                            }
                            DragHandle.BOTTOM -> {
                                b = (b + dy).coerceIn(t + minCropSizePx, bound.bottom)
                            }
                            DragHandle.LEFT -> {
                                l = (l + dx).coerceIn(bound.left, r - minCropSizePx)
                            }
                            DragHandle.RIGHT -> {
                                r = (r + dx).coerceIn(l + minCropSizePx, bound.right)
                            }
                            DragHandle.CENTER -> {
                                val width = r - l
                                val height = b - t
                                var newL = l + dx
                                var newT = t + dy
                                var newR = newL + width
                                var newB = newT + height

                                if (newL < bound.left) {
                                    newL = bound.left
                                    newR = newL + width
                                }
                                if (newR > bound.right) {
                                    newR = bound.right
                                    newL = newR - width
                                }
                                if (newT < bound.top) {
                                    newT = bound.top
                                    newB = newT + height
                                }
                                if (newB > bound.bottom) {
                                    newB = bound.bottom
                                    newT = newB - height
                                }
                                l = newL
                                t = newT
                                r = newR
                                b = newB
                            }
                            DragHandle.NONE -> {}
                        }

                        cropRect = Rect(l, t, r, b)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (canvasSize != size) {
                canvasSize = size
            }

            if (imageDisplayRect.width > 0 && imageDisplayRect.height > 0) {
                // 1. Draw the screenshot bitmap
                drawImage(
                    image = imageBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        imageDisplayRect.left.toInt(),
                        imageDisplayRect.top.toInt()
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        imageDisplayRect.width.toInt(),
                        imageDisplayRect.height.toInt()
                    )
                )

                // 2. Dimmed Mask outside the crop rectangle
                val maskColor = Color.Black.copy(alpha = 0.60f)
                if (cropRect.top > imageDisplayRect.top) {
                    drawRect(
                        color = maskColor,
                        topLeft = Offset(imageDisplayRect.left, imageDisplayRect.top),
                        size = Size(imageDisplayRect.width, cropRect.top - imageDisplayRect.top)
                    )
                }
                if (cropRect.bottom < imageDisplayRect.bottom) {
                    drawRect(
                        color = maskColor,
                        topLeft = Offset(imageDisplayRect.left, cropRect.bottom),
                        size = Size(imageDisplayRect.width, imageDisplayRect.bottom - cropRect.bottom)
                    )
                }
                if (cropRect.left > imageDisplayRect.left) {
                    drawRect(
                        color = maskColor,
                        topLeft = Offset(imageDisplayRect.left, cropRect.top),
                        size = Size(cropRect.left - imageDisplayRect.left, cropRect.height)
                    )
                }
                if (cropRect.right < imageDisplayRect.right) {
                    drawRect(
                        color = maskColor,
                        topLeft = Offset(cropRect.right, cropRect.top),
                        size = Size(imageDisplayRect.right - cropRect.right, cropRect.height)
                    )
                }

                // 3. Crisp Crop Border
                drawRect(
                    color = Color.White,
                    topLeft = cropRect.topLeft,
                    size = cropRect.size,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 4. Rule of Thirds Grid Lines (Visible when dragging)
                if (isDragging) {
                    val gridColor = Color.White.copy(alpha = 0.50f)
                    val oneThirdW = cropRect.width / 3f
                    val oneThirdH = cropRect.height / 3f

                    drawLine(gridColor, Offset(cropRect.left + oneThirdW, cropRect.top), Offset(cropRect.left + oneThirdW, cropRect.bottom), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(cropRect.left + 2 * oneThirdW, cropRect.top), Offset(cropRect.left + 2 * oneThirdW, cropRect.bottom), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(cropRect.left, cropRect.top + oneThirdH), Offset(cropRect.right, cropRect.top + oneThirdH), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(cropRect.left, cropRect.top + 2 * oneThirdH), Offset(cropRect.right, cropRect.top + 2 * oneThirdH), strokeWidth = 1.dp.toPx())
                }

                // 5. Heavy Corner Handles
                val cornerLength = 30.dp.toPx()
                val cornerStroke = 5.dp.toPx()
                val handleColor = Color.White

                // Top-Left
                drawLine(handleColor, Offset(cropRect.left, cropRect.top), Offset(cropRect.left + cornerLength, cropRect.top), strokeWidth = cornerStroke)
                drawLine(handleColor, Offset(cropRect.left, cropRect.top), Offset(cropRect.left + cornerLength, cropRect.top), strokeWidth = cornerStroke)

                // Top-Right
                drawLine(handleColor, Offset(cropRect.right, cropRect.top), Offset(cropRect.right - cornerLength, cropRect.top), strokeWidth = cornerStroke)
                drawLine(handleColor, Offset(cropRect.right, cropRect.top), Offset(cropRect.right, cropRect.top + cornerLength), strokeWidth = cornerStroke)

                // Bottom-Left
                drawLine(handleColor, Offset(cropRect.left, cropRect.bottom), Offset(cropRect.left + cornerLength, cropRect.bottom), strokeWidth = cornerStroke)
                drawLine(handleColor, Offset(cropRect.left, cropRect.bottom), Offset(cropRect.left, cropRect.bottom - cornerLength), strokeWidth = cornerStroke)

                // Bottom-Right
                drawLine(handleColor, Offset(cropRect.right, cropRect.bottom), Offset(cropRect.right - cornerLength, cropRect.bottom), strokeWidth = cornerStroke)
                drawLine(handleColor, Offset(cropRect.right, cropRect.bottom), Offset(cropRect.right, cropRect.bottom - cornerLength), strokeWidth = cornerStroke)

                // 6. Center Edge Grip Bars
                val edgeBarLength = 26.dp.toPx()
                val edgeStroke = 4.5f.dp.toPx()
                // Top Edge
                drawLine(handleColor, Offset(cropRect.center.x - edgeBarLength / 2, cropRect.top), Offset(cropRect.center.x + edgeBarLength / 2, cropRect.top), strokeWidth = edgeStroke)
                // Bottom Edge
                drawLine(handleColor, Offset(cropRect.center.x - edgeBarLength / 2, cropRect.bottom), Offset(cropRect.center.x + edgeBarLength / 2, cropRect.bottom), strokeWidth = edgeStroke)
                // Left Edge
                drawLine(handleColor, Offset(cropRect.left, cropRect.center.y - edgeBarLength / 2), Offset(cropRect.left, cropRect.center.y + edgeBarLength / 2), strokeWidth = edgeStroke)
                // Right Edge
                drawLine(handleColor, Offset(cropRect.right, cropRect.center.y - edgeBarLength / 2), Offset(cropRect.right, cropRect.center.y + edgeBarLength / 2), strokeWidth = edgeStroke)
            }
        }
    }
}

private fun getHitHandle(touch: Offset, rect: Rect, cornerRadius: Float, threshold: Float): DragHandle {
    // 1. Corners (highest priority)
    if ((touch - rect.topLeft).getDistance() <= cornerRadius) return DragHandle.TOP_LEFT
    if ((touch - rect.topRight).getDistance() <= cornerRadius) return DragHandle.TOP_RIGHT
    if ((touch - rect.bottomLeft).getDistance() <= cornerRadius) return DragHandle.BOTTOM_LEFT
    if ((touch - rect.bottomRight).getDistance() <= cornerRadius) return DragHandle.BOTTOM_RIGHT

    // 2. Bottom Edge (Super generous: catches anything in bottom threshold zone)
    if (touch.x in (rect.left - threshold)..(rect.right + threshold)) {
        if (touch.y >= rect.bottom - threshold && touch.y <= rect.bottom + threshold) {
            return DragHandle.BOTTOM
        }
    }

    // 3. Top Edge
    if (touch.x in (rect.left - threshold)..(rect.right + threshold)) {
        if (touch.y >= rect.top - threshold && touch.y <= rect.top + threshold) {
            return DragHandle.TOP
        }
    }

    // 4. Left Edge
    if (touch.y in (rect.top - threshold)..(rect.bottom + threshold)) {
        if (touch.x >= rect.left - threshold && touch.x <= rect.left + threshold) {
            return DragHandle.LEFT
        }
    }

    // 5. Right Edge
    if (touch.y in (rect.top - threshold)..(rect.bottom + threshold)) {
        if (touch.x >= rect.right - threshold && touch.x <= rect.right + threshold) {
            return DragHandle.RIGHT
        }
    }

    // 6. Center inside (only if clearly inside the central region)
    if (rect.contains(touch)) {
        return DragHandle.CENTER
    }

    return DragHandle.NONE
}
