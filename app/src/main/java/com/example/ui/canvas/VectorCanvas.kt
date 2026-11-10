package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.*
import com.example.model.PathSegment
import com.example.viewmodel.DragMode
import com.example.viewmodel.EditorTool
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.PenSubTool
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ResizeHandle {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    RIGHT_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_CENTER,
    BOTTOM_LEFT,
    LEFT_CENTER
}

@Composable
fun VectorCanvas(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    val currentUiState by rememberUpdatedState(uiState)
    val currentViewModel by rememberUpdatedState(viewModel)

    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var dragStartPoint by remember { mutableStateOf(Point2D(0f, 0f)) }
    var currentDragPoint by remember { mutableStateOf(Point2D(0f, 0f)) }
    var initialElementTransforms by remember { mutableStateOf<Map<String, Matrix2D>>(emptyMap()) }
    var initialShapeBoundsMap by remember { mutableStateOf<Map<String, Rect2D>>(emptyMap()) }
    var activeResizeHandle by remember { mutableStateOf<ResizeHandle?>(null) }
    var initialResizeBounds by remember { mutableStateOf<Rect2D?>(null) }
    var currentDraggingPenHandle by remember { mutableStateOf<Point2D?>(null) }
    var penLiveAnchor by remember { mutableStateOf<Point2D?>(null) }
    var activeHandleType by remember { mutableStateOf<Int?>(null) } // 0: inHandle, 1: outHandle

    // Live brush/pencil stroke points
    val strokeLivePoints = remember { mutableStateListOf<Point2D>() }

    // Preallocated paint to prevent GC churn on every frame
    val artboardLabelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(190, 160, 175, 190)
            textSize = 11f
            isAntiAlias = true
        }
    }

    val activeArtboard = uiState.document.activeArtboard()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        // Auto-center artboard on initial layout
        var hasCenteredInitialArtboard by remember { mutableStateOf(false) }
        LaunchedEffect(uiState.document.id, viewportW, viewportH) {
            if (viewportW > 0 && viewportH > 0 && !hasCenteredInitialArtboard) {
                viewModel.fitAndCenterArtboard(viewportW, viewportH)
                hasCenteredInitialArtboard = true
            }
        }

        fun screenToDoc(screenX: Float, screenY: Float): Point2D {
            val state = currentUiState
            val z = if (state.zoom > 0f) state.zoom else 1f
            val docX = (screenX - state.panX) / z
            val docY = (screenY - state.panY) / z
            return Point2D(docX, docY)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        var isMultiTouch = false
                        var hasMoved = false
                        val startScreen = firstDown.position
                        var prevSingleScreen = startScreen
                        val startDoc = screenToDoc(startScreen.x, startScreen.y)
                        dragStartPoint = startDoc
                        currentDragPoint = startDoc

                        val touchSlop = viewConfiguration.touchSlop
                        val tool = currentUiState.currentTool

                        // Prepare single-touch drag target based on tool
                        when (tool) {
                            EditorTool.SELECTION -> {
                                val selectedElements = currentUiState.document.layers
                                    .flatMap { it.elements }
                                    .filter { it.id in currentUiState.selectedElementIds }

                                var hitHandle: ResizeHandle? = null
                                var currentBounds: Rect2D? = null

                                if (selectedElements.isNotEmpty()) {
                                    var b = selectedElements.first().getBoundingBox()
                                    for (i in 1 until selectedElements.size) {
                                        b = b.union(selectedElements[i].getBoundingBox())
                                    }
                                    currentBounds = b
                                    val handleTolerance = (26f / currentUiState.zoom).coerceAtLeast(18f)
                                    hitHandle = hitTestSelectionHandle(b, startDoc, handleTolerance)
                                }

                                if (hitHandle != null && currentBounds != null) {
                                    dragMode = DragMode.RESIZE_HANDLE
                                    activeResizeHandle = hitHandle
                                    initialResizeBounds = currentBounds
                                    val shapeBoundsMap = mutableMapOf<String, Rect2D>()
                                    val transformMap = mutableMapOf<String, Matrix2D>()
                                    for (el in selectedElements) {
                                        transformMap[el.id] = el.transform
                                        if (el is ShapeElement) {
                                            shapeBoundsMap[el.id] = el.bounds
                                        }
                                    }
                                    initialShapeBoundsMap = shapeBoundsMap
                                    initialElementTransforms = transformMap
                                } else {
                                    val hit = hitTestElement(currentUiState.document, startDoc)
                                    if (hit != null) {
                                        if (hit.id !in currentUiState.selectedElementIds) {
                                            currentViewModel.selectElement(hit.id, addToSelection = currentUiState.isShiftPressed)
                                        }
                                        dragMode = DragMode.MOVE_SELECTION
                                        val doc = currentUiState.document
                                        val map = mutableMapOf<String, Matrix2D>()
                                        for (l in doc.layers) {
                                            for (el in l.elements) {
                                                if (el.id in currentUiState.selectedElementIds || el.id == hit.id) {
                                                    map[el.id] = el.transform
                                                }
                                            }
                                        }
                                        initialElementTransforms = map
                                    } else {
                                        val activeAb = currentUiState.document.activeArtboard()
                                        if (!activeAb.contains(startDoc)) {
                                            // 1-finger outside artboard panning!
                                            dragMode = DragMode.PANNING
                                        } else {
                                            dragMode = DragMode.SELECT_BOX
                                        }
                                    }
                                }
                            }
                            EditorTool.DIRECT_SELECTION -> {
                                val selectedIds = currentUiState.selectedElementIds
                                val hit = hitTestElement(currentUiState.document, startDoc)
                                if (hit != null && hit.id in selectedIds) {
                                    val (anchorIdx, handleType) = hitTestAnchorOrHandle(hit, startDoc, 18f / currentUiState.zoom)
                                    if (anchorIdx != null) {
                                        currentViewModel.selectAnchor(anchorIdx)
                                        activeHandleType = handleType
                                        dragMode = if (handleType != null) DragMode.EDIT_HANDLE else DragMode.EDIT_ANCHOR
                                    } else {
                                        dragMode = DragMode.MOVE_SELECTION
                                    }
                                } else if (hit != null) {
                                    currentViewModel.selectElement(hit.id)
                                    val (anchorIdx, _) = hitTestAnchorOrHandle(hit, startDoc, 18f / currentUiState.zoom)
                                    currentViewModel.selectAnchor(anchorIdx)
                                    dragMode = DragMode.NONE
                                } else {
                                    currentViewModel.clearSelection()
                                    dragMode = DragMode.NONE
                                }
                            }
                            EditorTool.PEN -> {
                                if (currentUiState.activePenSubTool == PenSubTool.PEN) {
                                    dragMode = DragMode.PEN_DRAGGING
                                    penLiveAnchor = startDoc
                                    currentDraggingPenHandle = Point2D(0f, 0f)
                                }
                            }
                            EditorTool.BRUSH -> {
                                dragMode = DragMode.BRUSH_DRAWING
                                strokeLivePoints.clear()
                                strokeLivePoints.add(startDoc)
                            }
                            EditorTool.PENCIL -> {
                                dragMode = DragMode.PENCIL_DRAWING
                                strokeLivePoints.clear()
                                strokeLivePoints.add(startDoc)
                            }
                            EditorTool.SHAPES -> {
                                dragMode = DragMode.SHAPE_DRAGGING
                            }
                            EditorTool.LINE -> {
                                dragMode = DragMode.LINE_DRAGGING
                            }
                            EditorTool.SHAPE_BUILDER -> {
                                dragMode = DragMode.SHAPE_BUILDER_DRAGGING
                            }
                            EditorTool.GRADIENT -> {
                                dragMode = DragMode.GRADIENT_DRAGGING
                            }
                            EditorTool.HAND -> {
                                dragMode = DragMode.PANNING
                            }
                            EditorTool.CURVATURE -> {
                                dragMode = DragMode.CURVATURE_DRAWING
                                strokeLivePoints.clear()
                                strokeLivePoints.add(startDoc)
                            }
                            EditorTool.ERASER -> {
                                dragMode = DragMode.ERASER_DRAGGING
                            }
                            EditorTool.ROTATE -> {
                                dragMode = DragMode.ROTATE_SELECTION
                            }
                            EditorTool.SCALE -> {
                                dragMode = DragMode.SCALE_SELECTION
                            }
                            EditorTool.ARTBOARD -> {
                                dragMode = DragMode.ARTBOARD_RESIZING
                            }
                            EditorTool.ZOOM -> {
                                dragMode = DragMode.ZOOM_DRAGGING
                            }
                            else -> {}
                        }

                        // Event loop for smooth continuous touch tracking
                        var prevDistance = 0f
                        var prevCenter = Offset.Zero
                        var hasRecordedUndoForDrag = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes.filter { it.pressed }

                            if (pointers.isEmpty()) {
                                break
                            }

                            if (pointers.size >= 2) {
                                // Multi-touch: Smooth Pinch-Zoom & Two-Finger Pan!
                                isMultiTouch = true
                                val p1 = pointers[0].position
                                val p2 = pointers[1].position
                                val currentDistance = (p1 - p2).getDistance()
                                val currentCenter = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                if (prevDistance > 0f) {
                                    val zoomFactor = (currentDistance / prevDistance).coerceIn(0.2f, 5.0f)
                                    val panDelta = currentCenter - prevCenter
                                    currentViewModel.updateZoomAndPan(
                                        zoomDelta = zoomFactor,
                                        panDeltaX = panDelta.x,
                                        panDeltaY = panDelta.y,
                                        focalX = currentCenter.x,
                                        focalY = currentCenter.y
                                    )
                                }

                                prevDistance = currentDistance
                                prevCenter = currentCenter
                                event.changes.forEach { it.consume() }
                            } else {
                                // Transitioning from multi-touch: avoid single-touch drawing
                                if (isMultiTouch) {
                                    prevDistance = 0f
                                    prevCenter = Offset.Zero
                                    event.changes.forEach { it.consume() }
                                    continue
                                }

                                val singlePointer = pointers[0]
                                val screenPt = singlePointer.position
                                val docPt = screenToDoc(screenPt.x, screenPt.y)
                                currentDragPoint = docPt

                                val distFromStart = (screenPt - startScreen).getDistance()
                                if (distFromStart > touchSlop) {
                                    hasMoved = true
                                }

                                if (hasMoved) {
                                    singlePointer.consume()

                                    if (!hasRecordedUndoForDrag && (dragMode == DragMode.MOVE_SELECTION || dragMode == DragMode.RESIZE_HANDLE)) {
                                        currentViewModel.commitUndoSnapshot()
                                        hasRecordedUndoForDrag = true
                                    }

                                    when (dragMode) {
                                        DragMode.PANNING -> {
                                            val delta = screenPt - prevSingleScreen
                                            currentViewModel.updateZoomAndPan(1.0f, delta.x, delta.y)
                                        }
                                        DragMode.MOVE_SELECTION -> {
                                            val dx = docPt.x - dragStartPoint.x
                                            val dy = docPt.y - dragStartPoint.y
                                            initialElementTransforms.forEach { (id, baseTransform) ->
                                                val newTransform = baseTransform.translate(dx, dy)
                                                currentViewModel.updateElementLiveTransform(id, newTransform)
                                            }
                                        }
                                        DragMode.RESIZE_HANDLE -> {
                                            val initB = initialResizeBounds
                                            val handle = activeResizeHandle
                                            if (initB != null && handle != null) {
                                                val dx = docPt.x - dragStartPoint.x
                                                val dy = docPt.y - dragStartPoint.y
                                                val newBounds = computeResizedBounds(initB, handle, dx, dy, currentUiState.isShiftPressed)

                                                val selIds = currentUiState.selectedElementIds
                                                if (selIds.size == 1 && initialShapeBoundsMap.containsKey(selIds.first()) &&
                                                    initialElementTransforms[selIds.first()]?.isIdentity() == true) {
                                                    currentViewModel.updateElementLiveBounds(selIds.first(), newBounds)
                                                } else {
                                                    val scaleX = if (initB.width > 0.001f) (newBounds.width / initB.width).coerceIn(0.01f, 100f) else 1f
                                                    val scaleY = if (initB.height > 0.001f) (newBounds.height / initB.height).coerceIn(0.01f, 100f) else 1f
                                                    val t = Matrix2D.translation(newBounds.left, newBounds.top) *
                                                            Matrix2D.scaling(scaleX, scaleY) *
                                                            Matrix2D.translation(-initB.left, -initB.top)

                                                    initialElementTransforms.forEach { (id, baseTransform) ->
                                                        val newTransform = t * baseTransform
                                                        currentViewModel.updateElementLiveTransform(id, newTransform)
                                                    }
                                                }
                                            }
                                        }
                                        DragMode.EDIT_ANCHOR -> {
                                            currentViewModel.updateSelectedAnchor(docPt)
                                        }
                                        DragMode.EDIT_HANDLE -> {
                                            val anchorIdx = currentUiState.selectedAnchorIndex
                                            val selElements = currentUiState.document.layers.flatMap { it.elements }.filter { it.id in currentUiState.selectedElementIds }
                                            if (anchorIdx != null && selElements.isNotEmpty()) {
                                                val pathEl = selElements.first() as? PathElement
                                                if (pathEl != null && anchorIdx in pathEl.segments.indices) {
                                                    val anchor = pathEl.transform.mapPoint(pathEl.segments[anchorIdx].anchor)
                                                    val offset = docPt - anchor
                                                    activeHandleType?.let { hType ->
                                                        currentViewModel.updateSelectedHandle(hType, offset)
                                                    }
                                                }
                                            }
                                        }
                                        DragMode.PEN_DRAGGING -> {
                                            val anchor = penLiveAnchor ?: dragStartPoint
                                            currentDraggingPenHandle = docPt - anchor
                                        }
                                        DragMode.BRUSH_DRAWING, DragMode.PENCIL_DRAWING -> {
                                            strokeLivePoints.add(docPt)
                                        }
                                        DragMode.SHAPE_BUILDER_DRAGGING -> {
                                            val hit = hitTestElement(currentUiState.document, docPt)
                                            if (hit != null && hit.id !in currentUiState.selectedElementIds) {
                                                currentViewModel.selectElement(hit.id, addToSelection = true)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                prevSingleScreen = screenPt
                            }
                        }

                        // Pointer Release: Handle Tap or Drag End
                        if (!isMultiTouch) {
                            if (!hasMoved) {
                                // Clean Tap Action!
                                when (currentUiState.currentTool) {
                                    EditorTool.SELECTION -> {
                                        val hit = hitTestElement(currentUiState.document, startDoc)
                                        currentViewModel.selectElement(hit?.id, addToSelection = currentUiState.isShiftPressed)
                                    }
                                    EditorTool.DIRECT_SELECTION -> {
                                        val hit = hitTestElement(currentUiState.document, startDoc)
                                        if (hit != null) {
                                            currentViewModel.selectElement(hit.id)
                                            val (anchorIdx, _) = hitTestAnchorOrHandle(hit, startDoc, 18f / currentUiState.zoom)
                                            currentViewModel.selectAnchor(anchorIdx)
                                        } else {
                                            currentViewModel.clearSelection()
                                        }
                                    }
                                    EditorTool.PEN -> {
                                        when (currentUiState.activePenSubTool) {
                                            PenSubTool.PEN -> {
                                                val currentSegs = currentViewModel.activeDrawingSegments.value
                                                if (currentSegs.isNotEmpty() && startDoc.distanceTo(currentSegs[0].anchor) < 24f / currentUiState.zoom) {
                                                    currentViewModel.addPath(currentSegs, closed = true)
                                                    currentViewModel.activeDrawingSegments.value = emptyList()
                                                } else {
                                                    val newSeg = PathSegment(anchor = startDoc)
                                                    currentViewModel.activeDrawingSegments.value = currentSegs + newSeg
                                                }
                                            }
                                            PenSubTool.ADD_ANCHOR -> {
                                                currentViewModel.addAnchorToSelectedPath(startDoc)
                                            }
                                            PenSubTool.DELETE_ANCHOR -> {
                                                val sel = currentUiState.document.layers.flatMap { it.elements }.firstOrNull { it.id in currentUiState.selectedElementIds }
                                                if (sel != null) {
                                                    val (idx, _) = hitTestAnchorOrHandle(sel, startDoc, 18f / currentUiState.zoom)
                                                    if (idx != null) currentViewModel.deleteAnchorFromSelectedPath(idx)
                                                }
                                            }
                                            PenSubTool.CONVERT_ANCHOR -> {
                                                val sel = currentUiState.document.layers.flatMap { it.elements }.firstOrNull { it.id in currentUiState.selectedElementIds }
                                                if (sel != null) {
                                                    val (idx, _) = hitTestAnchorOrHandle(sel, startDoc, 18f / currentUiState.zoom)
                                                    if (idx != null) currentViewModel.convertAnchorOnSelectedPath(idx)
                                                }
                                            }
                                        }
                                    }
                                    EditorTool.TEXT -> {
                                        currentViewModel.addText("Text Element", startDoc.x, startDoc.y)
                                    }
                                    EditorTool.EYEDROPPER -> {
                                        val hit = hitTestElement(currentUiState.document, startDoc)
                                        if (hit != null && hit.fill is VpPaint.Solid) {
                                            currentViewModel.updateSelectedColor((hit.fill as VpPaint.Solid).color)
                                        }
                                    }
                                    EditorTool.ZOOM -> {
                                        if (currentUiState.isAltPressed) currentViewModel.zoomOut() else currentViewModel.zoomIn()
                                    }
                                    EditorTool.ROTATE -> {
                                        currentViewModel.rotateSelection(15f)
                                    }
                                    EditorTool.SCALE -> {
                                        currentViewModel.scaleSelection(1.1f)
                                    }
                                    EditorTool.ERASER -> {
                                        val hit = hitTestElement(currentUiState.document, startDoc)
                                        if (hit != null) {
                                            currentViewModel.selectElement(hit.id, addToSelection = false)
                                            currentViewModel.deleteSelectedElements()
                                        }
                                    }
                                    EditorTool.CURVATURE -> {
                                        val currentSegs = currentViewModel.activeDrawingSegments.value
                                        val newSeg = PathSegment(anchor = startDoc, isSmooth = true)
                                        currentViewModel.activeDrawingSegments.value = currentSegs + newSeg
                                    }
                                    else -> {}
                                }
                            } else {
                                // Finalize Drag
                                when (dragMode) {
                                    DragMode.SHAPE_DRAGGING -> {
                                        var rect = Rect2D.fromPoints(dragStartPoint, currentDragPoint)
                                        if (currentUiState.isShiftPressed) {
                                            val maxDim = max(rect.width, rect.height)
                                            rect = Rect2D(rect.left, rect.top, rect.left + maxDim, rect.top + maxDim)
                                        }
                                        if (rect.width > 4f && rect.height > 4f) {
                                            currentViewModel.addShapeAt(rect, currentUiState.activeShapeType)
                                        }
                                    }
                                    DragMode.LINE_DRAGGING -> {
                                        if (dragStartPoint.distanceTo(currentDragPoint) > 4f) {
                                            currentViewModel.addLineAt(dragStartPoint, currentDragPoint)
                                        }
                                    }
                                    DragMode.GRADIENT_DRAGGING -> {
                                        currentViewModel.applyLinearGradient(dragStartPoint, currentDragPoint)
                                    }
                                    DragMode.SHAPE_BUILDER_DRAGGING -> {
                                        currentViewModel.runShapeBuilderOnSelected()
                                    }
                                    DragMode.PEN_DRAGGING -> {
                                        val anchor = penLiveAnchor ?: dragStartPoint
                                        val handle = currentDraggingPenHandle ?: Point2D(0f, 0f)
                                        val newSeg = if (currentUiState.isAltPressed) {
                                            PathSegment(
                                                anchor = anchor,
                                                inHandle = Point2D(-handle.x, -handle.y),
                                                outHandle = Point2D(0f, 0f),
                                                isSmooth = false
                                            )
                                        } else {
                                            PathSegment(
                                                anchor = anchor,
                                                inHandle = Point2D(-handle.x, -handle.y),
                                                outHandle = handle,
                                                isSmooth = handle.x != 0f || handle.y != 0f
                                            )
                                        }
                                        currentViewModel.activeDrawingSegments.value += newSeg
                                    }
                                    DragMode.PENCIL_DRAWING, DragMode.BRUSH_DRAWING -> {
                                        if (strokeLivePoints.size >= 2) {
                                            val segments = strokeLivePoints.map { PathSegment(anchor = it) }
                                            currentViewModel.addPath(segments, closed = false)
                                        }
                                        strokeLivePoints.clear()
                                    }
                                    DragMode.CURVATURE_DRAWING -> {
                                        if (strokeLivePoints.size >= 2) {
                                            val segments = strokeLivePoints.map { PathSegment(anchor = it, isSmooth = true) }
                                            currentViewModel.addPath(segments, closed = false)
                                        }
                                        strokeLivePoints.clear()
                                    }
                                    DragMode.ROTATE_SELECTION -> {
                                        val deltaX = currentDragPoint.x - dragStartPoint.x
                                        currentViewModel.rotateSelection(deltaX * 0.3f)
                                    }
                                    DragMode.SCALE_SELECTION -> {
                                        val dist = dragStartPoint.distanceTo(currentDragPoint)
                                        val factor = if (currentDragPoint.x >= dragStartPoint.x) 1f + (dist / 200f) else (1f - (dist / 200f)).coerceAtLeast(0.2f)
                                        currentViewModel.scaleSelection(factor)
                                    }
                                    DragMode.ERASER_DRAGGING -> {
                                        val hit = hitTestElement(currentUiState.document, currentDragPoint)
                                        if (hit != null) {
                                            currentViewModel.selectElement(hit.id, addToSelection = false)
                                            currentViewModel.deleteSelectedElements()
                                        }
                                    }
                                    else -> {}
                                }
                                if (dragMode == DragMode.MOVE_SELECTION || dragMode == DragMode.RESIZE_HANDLE) {
                                    currentViewModel.saveDocument()
                                }
                            }
                        }

                        // Reset drag state
                        dragMode = DragMode.NONE
                        activeResizeHandle = null
                        initialResizeBounds = null
                        initialShapeBoundsMap = emptyMap()
                        penLiveAnchor = null
                        currentDraggingPenHandle = null
                        activeHandleType = null
                        initialElementTransforms = emptyMap()
                        strokeLivePoints.clear()
                    }
                }
        ) {
            // Authentic Adobe Illustrator studio matte background
            drawRect(Color(0xFF333742))

            translate(left = uiState.panX, top = uiState.panY) {
                scale(scaleX = uiState.zoom, scaleY = uiState.zoom, pivot = Offset.Zero) {

                    // 1. Clean Artboard Surface (NO software logos, clean white sheet)
                    drawArtboard(activeArtboard, uiState.showBleed, uiState.showGrid, artboardLabelPaint)

                    // 2. Render Artwork Elements
                    for (layer in uiState.document.layers) {
                        if (!layer.visible) continue
                        for (element in layer.elements) {
                            if (element.isHidden) continue
                            drawVectorElement(element, uiState.document, uiState.softProofCmyk)
                        }
                    }

                    // 3. Live Brush / Pencil Trail
                    if (strokeLivePoints.size >= 2) {
                        val path = Path()
                        path.moveTo(strokeLivePoints[0].x, strokeLivePoints[0].y)
                        for (i in 1 until strokeLivePoints.size) {
                            path.lineTo(strokeLivePoints[i].x, strokeLivePoints[i].y)
                        }
                        val strokeW = if (uiState.currentTool == EditorTool.BRUSH) 4.0f / uiState.zoom else 2.0f / uiState.zoom
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = strokeW)
                        )
                    }

                    // 4. Pen Segments & Live Bézier Rubber-Band Curve
                    val penSegs = viewModel.activeDrawingSegments.value
                    if (penSegs.isNotEmpty()) {
                        drawPenLiveSegments(penSegs, penLiveAnchor, currentDraggingPenHandle, uiState.zoom)
                    }

                    // 5. Shape In-Progress Preview
                    if (dragMode == DragMode.SHAPE_DRAGGING) {
                        var rect = Rect2D.fromPoints(dragStartPoint, currentDragPoint)
                        if (uiState.isShiftPressed) {
                            val maxDim = max(rect.width, rect.height)
                            rect = Rect2D(rect.left, rect.top, rect.left + maxDim, rect.top + maxDim)
                        }
                        drawRect(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height)
                        )
                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height),
                            style = Stroke(width = 1.5f / uiState.zoom)
                        )
                    }

                    // Line in progress
                    if (dragMode == DragMode.LINE_DRAGGING) {
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(dragStartPoint.x, dragStartPoint.y),
                            end = Offset(currentDragPoint.x, currentDragPoint.y),
                            strokeWidth = 2.0f / uiState.zoom
                        )
                    }

                    // 6. Selection Bounding Box & Direct Selection Handles
                    if (uiState.selectedElementIds.isNotEmpty()) {
                        val selectedElements = uiState.document.layers
                            .flatMap { it.elements }
                            .filter { it.id in uiState.selectedElementIds }

                        if (selectedElements.isNotEmpty()) {
                            if (uiState.currentTool == EditorTool.DIRECT_SELECTION) {
                                for (el in selectedElements) {
                                    drawDirectSelectionHandles(el, uiState.selectedAnchorIndex, uiState.zoom)
                                }
                            } else {
                                var bounds = selectedElements.first().getBoundingBox()
                                for (i in 1 until selectedElements.size) {
                                    bounds = bounds.union(selectedElements[i].getBoundingBox())
                                }
                                drawSelectionBounds(bounds, uiState.zoom, activeResizeHandle)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawArtboard(
    artboard: Artboard,
    showBleed: Boolean,
    showGrid: Boolean,
    labelPaint: android.graphics.Paint
) {
    val artboardTopLeft = Offset(artboard.x, artboard.y)
    val artboardSize = Size(artboard.width, artboard.height)

    // Soft drop shadow
    drawRect(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = artboardTopLeft + Offset(3f, 4f),
        size = artboardSize
    )

    // Pure White Canvas surface
    drawRect(
        color = Color(0xFFFFFFFF),
        topLeft = artboardTopLeft,
        size = artboardSize
    )

    // Subtle 50pt Grid lines if enabled (batched in single Path for high performance)
    if (showGrid) {
        val gridStep = 50f
        val gridPath = Path()
        var x = artboard.x
        while (x <= artboard.x + artboard.width) {
            gridPath.moveTo(x, artboard.y)
            gridPath.lineTo(x, artboard.y + artboard.height)
            x += gridStep
        }
        var y = artboard.y
        while (y <= artboard.y + artboard.height) {
            gridPath.moveTo(artboard.x, y)
            gridPath.lineTo(artboard.x + artboard.width, y)
            y += gridStep
        }
        drawPath(
            path = gridPath,
            color = Color(0xFFECEFF1),
            style = Stroke(width = 0.75f)
        )
    }

    // Artboard perimeter stroke
    drawRect(
        color = Color(0xFFCFD8DC),
        topLeft = artboardTopLeft,
        size = artboardSize,
        style = Stroke(width = 1f)
    )

    // Bleed guideline (magenta dash) if enabled
    if (showBleed && (artboard.bleed.top > 0f || artboard.bleed.left > 0f)) {
        val bleedTopLeft = Offset(artboard.x - artboard.bleed.left, artboard.y - artboard.bleed.top)
        val bleedSize = Size(
            artboard.width + artboard.bleed.left + artboard.bleed.right,
            artboard.height + artboard.bleed.top + artboard.bleed.bottom
        )
        drawRect(
            color = Color(0xFFFF4081).copy(alpha = 0.8f),
            topLeft = bleedTopLeft,
            size = bleedSize,
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
        )
    }

    // Clean Artboard Header Readout (Name & Dimensions)
    drawContext.canvas.nativeCanvas.drawText(
        "01 - ${artboard.name}  •  ${artboard.width.roundToInt()} × ${artboard.height.roundToInt()} pt",
        artboard.x,
        artboard.y - 8f,
        labelPaint
    )
}

private fun DrawScope.drawVectorElement(element: VectorElement, document: Document, softProof: Boolean) {
    val composePath = toComposePath(element)

    // Fill
    when (val f = element.fill) {
        is VpPaint.Solid -> {
            val c = ColorEngine.toComposeColor(f.color, document.profile, softProof)
            drawPath(
                path = composePath,
                color = c.copy(alpha = c.alpha * element.opacity)
            )
        }
        is VpPaint.Linear -> {
            if (f.stops.isNotEmpty()) {
                val c = ColorEngine.toComposeColor(f.stops.first().color, document.profile)
                drawPath(
                    path = composePath,
                    color = c.copy(alpha = c.alpha * element.opacity)
                )
            }
        }
        else -> {}
    }

    // Stroke
    when (val s = element.stroke) {
        is VpPaint.Solid -> {
            if (element.strokeStyle.width > 0f) {
                val c = ColorEngine.toComposeColor(s.color, document.profile, softProof)
                drawPath(
                    path = composePath,
                    color = c.copy(alpha = c.alpha * element.opacity),
                    style = Stroke(width = element.strokeStyle.width)
                )
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawPenLiveSegments(
    segments: List<PathSegment>,
    liveAnchor: Point2D?,
    liveHandle: Point2D?,
    zoom: Float
) {
    if (segments.isEmpty()) return
    val path = Path()
    path.moveTo(segments[0].anchor.x, segments[0].anchor.y)

    for (i in 1 until segments.size) {
        val p0 = segments[i - 1]
        val p1 = segments[i]
        val cp1 = p0.anchor + p0.outHandle
        val cp2 = p1.anchor + p1.inHandle
        path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.anchor.x, p1.anchor.y)
    }

    // Live segment rubber-band to live anchor/handle if dragging
    if (liveAnchor != null && liveHandle != null) {
        val last = segments.last()
        val cp1 = last.anchor + last.outHandle
        val cp2 = liveAnchor - liveHandle
        path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, liveAnchor.x, liveAnchor.y)

        // Draw live tangent handle lines
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.6f),
            start = Offset(liveAnchor.x, liveAnchor.y),
            end = Offset(liveAnchor.x + liveHandle.x, liveAnchor.y + liveHandle.y),
            strokeWidth = 1f / zoom
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.6f),
            start = Offset(liveAnchor.x, liveAnchor.y),
            end = Offset(liveAnchor.x - liveHandle.x, liveAnchor.y - liveHandle.y),
            strokeWidth = 1f / zoom
        )
        // Tangent handle tips
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 3.5f / zoom,
            center = Offset(liveAnchor.x + liveHandle.x, liveAnchor.y + liveHandle.y)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 3.5f / zoom,
            center = Offset(liveAnchor.x - liveHandle.x, liveAnchor.y - liveHandle.y)
        )
    }

    drawPath(
        path = path,
        color = Color(0xFF00E5FF),
        style = Stroke(width = 2.0f / zoom)
    )

    // Draw anchor squares
    for (seg in segments) {
        drawRect(
            color = Color.White,
            topLeft = Offset(seg.anchor.x - 3.5f / zoom, seg.anchor.y - 3.5f / zoom),
            size = Size(7f / zoom, 7f / zoom)
        )
        drawRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(seg.anchor.x - 3.5f / zoom, seg.anchor.y - 3.5f / zoom),
            size = Size(7f / zoom, 7f / zoom),
            style = Stroke(width = 1f / zoom)
        )
    }
}

private fun DrawScope.drawSelectionBounds(bounds: Rect2D, zoom: Float, activeHandle: ResizeHandle? = null) {
    drawRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 1.5f / zoom)
    )

    // Corner & Edge Handles
    val handles = listOf(
        ResizeHandle.TOP_LEFT to Offset(bounds.left, bounds.top),
        ResizeHandle.TOP_CENTER to Offset(bounds.centerX, bounds.top),
        ResizeHandle.TOP_RIGHT to Offset(bounds.right, bounds.top),
        ResizeHandle.RIGHT_CENTER to Offset(bounds.right, bounds.centerY),
        ResizeHandle.BOTTOM_RIGHT to Offset(bounds.right, bounds.bottom),
        ResizeHandle.BOTTOM_CENTER to Offset(bounds.centerX, bounds.bottom),
        ResizeHandle.BOTTOM_LEFT to Offset(bounds.left, bounds.bottom),
        ResizeHandle.LEFT_CENTER to Offset(bounds.left, bounds.centerY)
    )

    val handleSize = (8f / zoom).coerceIn(6f / zoom, 24f / zoom)
    val half = handleSize / 2f
    for ((handle, pt) in handles) {
        val isActive = handle == activeHandle
        drawRect(
            color = if (isActive) Color(0xFF00E5FF) else Color.White,
            topLeft = pt - Offset(half, half),
            size = Size(handleSize, handleSize)
        )
        drawRect(
            color = if (isActive) Color.White else Color(0xFF0091EA),
            topLeft = pt - Offset(half, half),
            size = Size(handleSize, handleSize),
            style = Stroke(width = 1.5f / zoom)
        )
    }
}

private fun DrawScope.drawDirectSelectionHandles(
    element: VectorElement,
    selectedAnchorIdx: Int?,
    zoom: Float
) {
    if (element !is PathElement) {
        drawSelectionBounds(element.getBoundingBox(), zoom)
        return
    }

    val transform = element.transform
    for ((idx, seg) in element.segments.withIndex()) {
        val anchorDoc = transform.mapPoint(seg.anchor)
        val isAnchorSelected = idx == selectedAnchorIdx

        // Tangent handles for selected anchor
        if (isAnchorSelected) {
            if (seg.inHandle.x != 0f || seg.inHandle.y != 0f) {
                val inDoc = transform.mapPoint(seg.anchor + seg.inHandle)
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(anchorDoc.x, anchorDoc.y),
                    end = Offset(inDoc.x, inDoc.y),
                    strokeWidth = 1f / zoom
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 3.5f / zoom,
                    center = Offset(inDoc.x, inDoc.y)
                )
            }
            if (seg.outHandle.x != 0f || seg.outHandle.y != 0f) {
                val outDoc = transform.mapPoint(seg.anchor + seg.outHandle)
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(anchorDoc.x, anchorDoc.y),
                    end = Offset(outDoc.x, outDoc.y),
                    strokeWidth = 1f / zoom
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 3.5f / zoom,
                    center = Offset(outDoc.x, outDoc.y)
                )
            }
        }

        // Anchor Point
        val hSize = 7f / zoom
        val hHalf = hSize / 2f
        drawRect(
            color = if (isAnchorSelected) Color(0xFF00E5FF) else Color.White,
            topLeft = Offset(anchorDoc.x - hHalf, anchorDoc.y - hHalf),
            size = Size(hSize, hSize)
        )
        drawRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(anchorDoc.x - hHalf, anchorDoc.y - hHalf),
            size = Size(hSize, hSize),
            style = Stroke(width = 1.2f / zoom)
        )
    }
}

private fun toComposePath(element: VectorElement): Path {
    val path = Path()
    when (element) {
        is ShapeElement -> {
            val b = element.bounds
            val t = element.transform
            when (element.shapeType) {
                ShapeType.RECTANGLE -> {
                    val p1 = t.mapPoint(Point2D(b.left, b.top))
                    val p2 = t.mapPoint(Point2D(b.right, b.top))
                    val p3 = t.mapPoint(Point2D(b.right, b.bottom))
                    val p4 = t.mapPoint(Point2D(b.left, b.bottom))
                    path.moveTo(p1.x, p1.y)
                    path.lineTo(p2.x, p2.y)
                    path.lineTo(p3.x, p3.y)
                    path.lineTo(p4.x, p4.y)
                    path.close()
                }
                ShapeType.ROUNDED_RECT -> {
                    val r = element.cornerRadius
                    val pts = listOf(
                        Point2D(b.left + r, b.top),
                        Point2D(b.right - r, b.top),
                        Point2D(b.right, b.top + r),
                        Point2D(b.right, b.bottom - r),
                        Point2D(b.right - r, b.bottom),
                        Point2D(b.left + r, b.bottom),
                        Point2D(b.left, b.bottom - r),
                        Point2D(b.left, b.top + r)
                    ).map { t.mapPoint(it) }
                    path.moveTo(pts[0].x, pts[0].y)
                    path.lineTo(pts[1].x, pts[1].y)
                    path.quadraticTo(t.mapPoint(Point2D(b.right, b.top)).x, t.mapPoint(Point2D(b.right, b.top)).y, pts[2].x, pts[2].y)
                    path.lineTo(pts[3].x, pts[3].y)
                    path.quadraticTo(t.mapPoint(Point2D(b.right, b.bottom)).x, t.mapPoint(Point2D(b.right, b.bottom)).y, pts[4].x, pts[4].y)
                    path.lineTo(pts[5].x, pts[5].y)
                    path.quadraticTo(t.mapPoint(Point2D(b.left, b.bottom)).x, t.mapPoint(Point2D(b.left, b.bottom)).y, pts[6].x, pts[6].y)
                    path.lineTo(pts[7].x, pts[7].y)
                    path.quadraticTo(t.mapPoint(Point2D(b.left, b.top)).x, t.mapPoint(Point2D(b.left, b.top)).y, pts[0].x, pts[0].y)
                    path.close()
                }
                ShapeType.ELLIPSE -> {
                    val cx = b.centerX
                    val cy = b.centerY
                    val rx = b.width / 2f
                    val ry = b.height / 2f
                    val k = 0.55228475f
                    val kx = rx * k
                    val ky = ry * k

                    val top = t.mapPoint(Point2D(cx, cy - ry))
                    val right = t.mapPoint(Point2D(cx + rx, cy))
                    val bottom = t.mapPoint(Point2D(cx, cy + ry))
                    val left = t.mapPoint(Point2D(cx - rx, cy))

                    val cp1 = t.mapPoint(Point2D(cx + kx, cy - ry))
                    val cp2 = t.mapPoint(Point2D(cx + rx, cy - ky))
                    val cp3 = t.mapPoint(Point2D(cx + rx, cy + ky))
                    val cp4 = t.mapPoint(Point2D(cx + kx, cy + ry))
                    val cp5 = t.mapPoint(Point2D(cx - kx, cy + ry))
                    val cp6 = t.mapPoint(Point2D(cx - rx, cy + ky))
                    val cp7 = t.mapPoint(Point2D(cx - rx, cy - ky))
                    val cp8 = t.mapPoint(Point2D(cx - kx, cy - ry))

                    path.moveTo(top.x, top.y)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, right.x, right.y)
                    path.cubicTo(cp3.x, cp3.y, cp4.x, cp4.y, bottom.x, bottom.y)
                    path.cubicTo(cp5.x, cp5.y, cp6.x, cp6.y, left.x, left.y)
                    path.cubicTo(cp7.x, cp7.y, cp8.x, cp8.y, top.x, top.y)
                    path.close()
                }
                ShapeType.STAR -> {
                    val cx = b.centerX
                    val cy = b.centerY
                    val rOut = min(b.width, b.height) / 2f
                    val rIn = rOut * 0.45f
                    val points = element.starPoints
                    for (i in 0 until points * 2) {
                        val angle = (i * Math.PI / points) - Math.PI / 2
                        val r = if (i % 2 == 0) rOut else rIn
                        val pt = t.mapPoint(Point2D((cx + r * kotlin.math.cos(angle)).toFloat(), (cy + r * kotlin.math.sin(angle)).toFloat()))
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    path.close()
                }
                ShapeType.POLYGON -> {
                    val cx = b.centerX
                    val cy = b.centerY
                    val r = min(b.width, b.height) / 2f
                    val sides = element.sides
                    for (i in 0 until sides) {
                        val angle = (i * 2 * Math.PI / sides) - Math.PI / 2
                        val pt = t.mapPoint(Point2D((cx + r * kotlin.math.cos(angle)).toFloat(), (cy + r * kotlin.math.sin(angle)).toFloat()))
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    path.close()
                }
                ShapeType.LINE -> {
                    val p1 = t.mapPoint(Point2D(b.left, b.top))
                    val p2 = t.mapPoint(Point2D(b.right, b.bottom))
                    path.moveTo(p1.x, p1.y)
                    path.lineTo(p2.x, p2.y)
                }
                ShapeType.SPIRAL -> {
                    val cx = b.centerX
                    val cy = b.centerY
                    val maxR = min(b.width, b.height) / 2f
                    val turns = 3.5
                    val totalSteps = 100
                    for (i in 0..totalSteps) {
                        val tFrac = i.toFloat() / totalSteps
                        val angle = tFrac * turns * 2 * Math.PI
                        val r = tFrac * maxR
                        val pt = t.mapPoint(Point2D((cx + r * kotlin.math.cos(angle)).toFloat(), (cy + r * kotlin.math.sin(angle)).toFloat()))
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                }
            }
        }
        is PathElement -> {
            val t = element.transform
            if (element.segments.isNotEmpty()) {
                val start = t.mapPoint(element.segments[0].anchor)
                path.moveTo(start.x, start.y)
                for (i in 1 until element.segments.size) {
                    val p0 = element.segments[i - 1]
                    val p1 = element.segments[i]
                    val cp1 = t.mapPoint(p0.anchor + p0.outHandle)
                    val cp2 = t.mapPoint(p1.anchor + p1.inHandle)
                    val end = t.mapPoint(p1.anchor)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
                }
                if (element.closed && element.segments.size > 2) {
                    val pLast = element.segments.last()
                    val pFirst = element.segments.first()
                    val cp1 = t.mapPoint(pLast.anchor + pLast.outHandle)
                    val cp2 = t.mapPoint(pFirst.anchor + pFirst.inHandle)
                    val end = t.mapPoint(pFirst.anchor)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
                    path.close()
                }
            }
        }
        is TextElement -> {
            val b = element.getBoundingBox()
            val t = element.transform
            val p1 = t.mapPoint(Point2D(b.left, b.top))
            val p2 = t.mapPoint(Point2D(b.right, b.top))
            val p3 = t.mapPoint(Point2D(b.right, b.bottom))
            val p4 = t.mapPoint(Point2D(b.left, b.bottom))
            path.moveTo(p1.x, p1.y)
            path.lineTo(p2.x, p2.y)
            path.lineTo(p3.x, p3.y)
            path.lineTo(p4.x, p4.y)
            path.close()
        }
        is GroupElement -> {
            for (child in element.children) {
                path.addPath(toComposePath(child))
            }
        }
    }
    return path
}

private fun hitTestElement(document: Document, pt: Point2D): VectorElement? {
    for (layer in document.layers.asReversed()) {
        if (!layer.visible || layer.locked) continue
        for (element in layer.elements.asReversed()) {
            if (element.isHidden || element.isLocked) continue
            val b = element.getBoundingBox()
            if (b.contains(pt)) {
                return element
            }
        }
    }
    return null
}

private fun hitTestAnchorOrHandle(element: VectorElement, docPt: Point2D, tolerance: Float): Pair<Int?, Int?> {
    if (element !is PathElement) return Pair(null, null)
    val t = element.transform

    for ((idx, seg) in element.segments.withIndex()) {
        val anchorPt = t.mapPoint(seg.anchor)
        if (anchorPt.distanceTo(docPt) <= tolerance) {
            return Pair(idx, null) // Hit anchor point
        }
        if (seg.inHandle.x != 0f || seg.inHandle.y != 0f) {
            val inPt = t.mapPoint(seg.anchor + seg.inHandle)
            if (inPt.distanceTo(docPt) <= tolerance) {
                return Pair(idx, 0) // Hit in-handle
            }
        }
        if (seg.outHandle.x != 0f || seg.outHandle.y != 0f) {
            val outPt = t.mapPoint(seg.anchor + seg.outHandle)
            if (outPt.distanceTo(docPt) <= tolerance) {
                return Pair(idx, 1) // Hit out-handle
            }
        }
    }
    return Pair(null, null)
}

private fun hitTestSelectionHandle(bounds: Rect2D, docPt: Point2D, tolerance: Float): ResizeHandle? {
    val handlePoints = listOf(
        ResizeHandle.TOP_LEFT to Point2D(bounds.left, bounds.top),
        ResizeHandle.TOP_CENTER to Point2D(bounds.centerX, bounds.top),
        ResizeHandle.TOP_RIGHT to Point2D(bounds.right, bounds.top),
        ResizeHandle.RIGHT_CENTER to Point2D(bounds.right, bounds.centerY),
        ResizeHandle.BOTTOM_RIGHT to Point2D(bounds.right, bounds.bottom),
        ResizeHandle.BOTTOM_CENTER to Point2D(bounds.centerX, bounds.bottom),
        ResizeHandle.BOTTOM_LEFT to Point2D(bounds.left, bounds.bottom),
        ResizeHandle.LEFT_CENTER to Point2D(bounds.left, bounds.centerY)
    )
    for ((handle, pt) in handlePoints) {
        if (docPt.distanceTo(pt) <= tolerance) {
            return handle
        }
    }
    return null
}

private fun computeResizedBounds(
    initB: Rect2D,
    handle: ResizeHandle,
    dx: Float,
    dy: Float,
    proportional: Boolean
): Rect2D {
    val minSize = 6f
    var l = initB.left
    var t = initB.top
    var r = initB.right
    var b = initB.bottom

    when (handle) {
        ResizeHandle.TOP_LEFT -> {
            l = min(initB.left + dx, initB.right - minSize)
            t = min(initB.top + dy, initB.bottom - minSize)
            if (proportional && initB.width > 0 && initB.height > 0) {
                val ratio = initB.width / initB.height
                val newW = initB.right - l
                val newH = newW / ratio
                t = initB.bottom - newH
            }
        }
        ResizeHandle.TOP_CENTER -> {
            t = min(initB.top + dy, initB.bottom - minSize)
        }
        ResizeHandle.TOP_RIGHT -> {
            r = max(initB.right + dx, initB.left + minSize)
            t = min(initB.top + dy, initB.bottom - minSize)
            if (proportional && initB.width > 0 && initB.height > 0) {
                val ratio = initB.width / initB.height
                val newW = r - initB.left
                val newH = newW / ratio
                t = initB.bottom - newH
            }
        }
        ResizeHandle.RIGHT_CENTER -> {
            r = max(initB.right + dx, initB.left + minSize)
        }
        ResizeHandle.BOTTOM_RIGHT -> {
            r = max(initB.right + dx, initB.left + minSize)
            b = max(initB.bottom + dy, initB.top + minSize)
            if (proportional && initB.width > 0 && initB.height > 0) {
                val ratio = initB.width / initB.height
                val newW = r - initB.left
                val newH = newW / ratio
                b = initB.top + newH
            }
        }
        ResizeHandle.BOTTOM_CENTER -> {
            b = max(initB.bottom + dy, initB.top + minSize)
        }
        ResizeHandle.BOTTOM_LEFT -> {
            l = min(initB.left + dx, initB.right - minSize)
            b = max(initB.bottom + dy, initB.top + minSize)
            if (proportional && initB.width > 0 && initB.height > 0) {
                val ratio = initB.width / initB.height
                val newW = initB.right - l
                val newH = newW / ratio
                b = initB.top + newH
            }
        }
        ResizeHandle.LEFT_CENTER -> {
            l = min(initB.left + dx, initB.right - minSize)
        }
    }
    return Rect2D(l, t, r, b)
}
