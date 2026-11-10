package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.export.VectorExporter
import com.example.model.*
import com.example.pathfinder.PathfinderOps
import com.example.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

enum class EditorTool(val displayName: String) {
    SELECTION("Selection (V)"),
    DIRECT_SELECTION("Direct Selection (A)"),
    PEN("Pen Tool (P)"),
    CURVATURE("Curvature Tool (Shift+~)"),
    SHAPES("Shapes (M)"),
    BRUSH("Paintbrush (B)"),
    PENCIL("Pencil Tool (N)"),
    ERASER("Eraser Tool (Shift+E)"),
    ROTATE("Rotate Tool (R)"),
    SCALE("Scale Tool (S)"),
    WIDTH("Width Tool (Shift+W)"),
    FREE_TRANSFORM("Free Transform (E)"),
    SHAPE_BUILDER("Shape Builder (Shift+M)"),
    LINE("Line Segment (\\)"),
    GRADIENT("Gradient Tool (G)"),
    EYEDROPPER("Eyedropper (I)"),
    BLEND("Blend Tool (W)"),
    TEXT("Type Tool (T)"),
    ARTBOARD("Artboard Tool (Shift+O)"),
    HAND("Hand / Pan (H)"),
    ZOOM("Zoom Tool (Z)")
}

enum class PaintTarget {
    FILL,
    STROKE
}

enum class EditorPanel {
    NONE,
    PROPERTIES,
    COLOR,
    SWATCHES,
    LAYERS,
    PATHFINDER,
    ALIGN,
    ARTBOARDS,
    EXPORT
}

enum class DragMode {
    NONE,
    PANNING,
    SELECT_BOX,
    MOVE_SELECTION,
    RESIZE_HANDLE,
    SCALE_SELECTION,
    ROTATE_SELECTION,
    EDIT_ANCHOR,
    EDIT_HANDLE,
    PEN_DRAGGING,
    CURVATURE_DRAWING,
    PENCIL_DRAWING,
    BRUSH_DRAWING,
    ERASER_DRAGGING,
    SHAPE_DRAGGING,
    LINE_DRAGGING,
    SHAPE_BUILDER_DRAGGING,
    GRADIENT_DRAGGING,
    ARTBOARD_RESIZING,
    ZOOM_DRAGGING
}

enum class PenSubTool(val displayName: String) {
    PEN("Pen Tool (P)"),
    ADD_ANCHOR("Add Anchor Point (+)"),
    DELETE_ANCHOR("Delete Anchor Point (-)"),
    CONVERT_ANCHOR("Anchor Point Convert (Shift+C)")
}

enum class EraserSubTool(val displayName: String) {
    ERASER("Eraser Tool (Shift+E)"),
    SCISSORS("Scissors Tool (C)"),
    KNIFE("Knife Tool")
}

enum class RotateSubTool(val displayName: String) {
    ROTATE("Rotate Tool (R)"),
    REFLECT("Reflect Tool (O)")
}

enum class ScaleSubTool(val displayName: String) {
    SCALE("Scale Tool (S)"),
    SHEAR("Shear Tool")
}

enum class BrushSubTool(val displayName: String) {
    PAINTBRUSH("Paintbrush (B)"),
    BLOB_BRUSH("Blob Brush (Shift+B)")
}

enum class PencilSubTool(val displayName: String) {
    PENCIL("Pencil Tool (N)"),
    SMOOTH("Smooth Tool"),
    PATH_ERASER("Path Eraser Tool")
}

enum class EyedropperSubTool(val displayName: String) {
    EYEDROPPER("Eyedropper (I)"),
    MEASURE("Measure Tool")
}

data class EditorUiState(
    val document: Document,
    val currentTool: EditorTool = EditorTool.SELECTION,
    val activePenSubTool: PenSubTool = PenSubTool.PEN,
    val activeEraserSubTool: EraserSubTool = EraserSubTool.ERASER,
    val activeRotateSubTool: RotateSubTool = RotateSubTool.ROTATE,
    val activeScaleSubTool: ScaleSubTool = ScaleSubTool.SCALE,
    val activeBrushSubTool: BrushSubTool = BrushSubTool.PAINTBRUSH,
    val activePencilSubTool: PencilSubTool = PencilSubTool.PENCIL,
    val activeEyedropperSubTool: EyedropperSubTool = EyedropperSubTool.EYEDROPPER,
    val selectedElementIds: Set<String> = emptySet(),
    val selectedAnchorIndex: Int? = null,
    val isEditingHandle: Boolean = false,
    val activeFill: VpPaint = VpPaint.Solid(VpColor.rgb(0, 229, 255)),
    val activeStroke: VpPaint = VpPaint.Solid(VpColor.rgb(20, 23, 31)),
    val activeStrokeStyle: StrokeStyle = StrokeStyle(width = 2.0f),
    val activePaintTarget: PaintTarget = PaintTarget.FILL,
    val activeShapeType: ShapeType = ShapeType.RECTANGLE,
    val zoom: Float = 1.0f,
    val panX: Float = 100f,
    val panY: Float = 100f,
    val softProofCmyk: Boolean = false,
    val showGrid: Boolean = true,
    val showRulers: Boolean = false,
    val showTransparencyGrid: Boolean = false,
    val showBleed: Boolean = true,
    val snapToGrid: Boolean = false,
    val snapToPoint: Boolean = true,
    val keyboardIncrementPt: Float = 1.0f,
    val scaleStrokesAndEffects: Boolean = true,
    val isShiftPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val activePanel: EditorPanel = EditorPanel.NONE,
    val isExporting: Boolean = false,
    val exportResult: VectorExporter.ExportResult? = null,
    val statusMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val undoStack = mutableListOf<Document>()
    private val redoStack = mutableListOf<Document>()

    private val _uiState = MutableStateFlow(
        EditorUiState(document = repository.getTemplates().first())
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Temporary path segments for Pen and Pencil tools during live drawing
    val activeDrawingSegments = MutableStateFlow<List<PathSegment>>(emptyList())
    val activeDrawingPoints = MutableStateFlow<List<Point2D>>(emptyList())
    val tempShapeBounds = MutableStateFlow<Rect2D?>(null)

    init {
        val doc = _uiState.value.document
        val ab = doc.activeArtboard()
        centerArtboard(ab)
    }

    fun centerArtboard(artboard: Artboard) {
        fitAndCenterArtboard(400f, 800f)
    }

    fun fitAndCenterArtboard(viewportWidth: Float, viewportHeight: Float) {
        if (viewportWidth <= 0 || viewportHeight <= 0) return
        val ab = _uiState.value.document.activeArtboard()
        val availW = (viewportWidth - 90f).coerceAtLeast(100f)
        val availH = (viewportHeight - 130f).coerceAtLeast(100f)

        val scaleX = availW / ab.width
        val scaleY = availH / ab.height
        val targetZoom = min(scaleX, scaleY).coerceIn(0.12f, 4.0f) * 0.90f

        val targetW = ab.width * targetZoom
        val targetH = ab.height * targetZoom

        // Perfectly center horizontally with room for tool rail and top menu bar
        val targetPanX = 64f + (availW - targetW) / 2f
        val targetPanY = 60f + (availH - targetH) / 2f

        _uiState.update {
            it.copy(
                panX = targetPanX,
                panY = targetPanY,
                zoom = targetZoom
            )
        }
    }

    // --- DOCUMENT MANAGEMENT ---
    fun openDocument(doc: Document) {
        saveStateForUndo()
        _uiState.update {
            it.copy(
                document = doc,
                selectedElementIds = emptySet(),
                activePanel = EditorPanel.NONE
            )
        }
        centerArtboard(doc.activeArtboard())
    }

    fun updateDocument(doc: Document, saveUndo: Boolean = true) {
        if (saveUndo) saveStateForUndo()
        _uiState.update {
            it.copy(document = doc)
        }
        repository.saveDocument(doc)
    }

    fun updateElementLiveTransform(id: String, transform: Matrix2D) {
        val currentDoc = _uiState.value.document
        val updatedLayers = currentDoc.layers.map { layer ->
            layer.copy(
                elements = layer.elements.map { el ->
                    if (el.id == id) el.withTransform(transform) else el
                }
            )
        }
        _uiState.update {
            it.copy(document = currentDoc.copy(layers = updatedLayers))
        }
    }

    fun updateElementLiveBounds(id: String, bounds: Rect2D) {
        val currentDoc = _uiState.value.document
        val updatedLayers = currentDoc.layers.map { layer ->
            layer.copy(
                elements = layer.elements.map { el ->
                    if (el.id == id && el is ShapeElement) {
                        el.copy(bounds = bounds)
                    } else el
                }
            )
        }
        _uiState.update {
            it.copy(document = currentDoc.copy(layers = updatedLayers))
        }
    }

    fun commitUndoSnapshot() {
        saveStateForUndo()
    }

    fun saveDocument() {
        repository.saveDocument(_uiState.value.document)
    }

    fun createNewDocument(
        name: String,
        width: Float,
        height: Float,
        units: DocumentUnit,
        colorMode: ColorMode,
        profile: ColorProfile,
        rasterPpi: Int,
        bleed: Bleed
    ) {
        saveStateForUndo()
        val newArtboard = Artboard(
            name = "Artboard 1",
            width = width,
            height = height,
            bleed = bleed
        )
        val newDoc = Document(
            name = name.ifBlank { "Untitled Document" },
            colorMode = colorMode,
            profile = profile,
            rasterPpi = rasterPpi,
            units = units,
            artboards = listOf(newArtboard),
            layers = listOf(Layer(name = "Layer 1")),
            swatches = Document.defaultSwatches(colorMode),
            selectedArtboardId = newArtboard.id
        )
        _uiState.update {
            it.copy(
                document = newDoc,
                selectedElementIds = emptySet(),
                activePanel = EditorPanel.NONE,
                statusMessage = "Created '${newDoc.name}' ($colorMode, $rasterPpi ppi)"
            )
        }
        centerArtboard(newArtboard)
        repository.saveDocument(newDoc)
    }

    fun convertDocumentColorMode(newMode: ColorMode, newProfile: ColorProfile) {
        saveStateForUndo()
        val currentDoc = _uiState.value.document
        val updatedSwatches = Document.defaultSwatches(newMode)
        val updatedDoc = currentDoc.copy(
            colorMode = newMode,
            profile = newProfile,
            swatches = updatedSwatches,
            lastModified = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                document = updatedDoc,
                statusMessage = "Converted document to ${newMode.name} mode"
            )
        }
        repository.saveDocument(updatedDoc)
    }

    // --- UNDO / REDO ---
    private fun saveStateForUndo() {
        val current = _uiState.value.document
        undoStack.add(current)
        if (undoStack.size > 200) undoStack.removeAt(0)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.document
        redoStack.add(current)
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                document = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true,
                selectedElementIds = emptySet()
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.document
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update {
            it.copy(
                document = next,
                canUndo = true,
                canRedo = redoStack.isNotEmpty(),
                selectedElementIds = emptySet()
            )
        }
    }

    // --- TOOL & MODIFIER SELECTION ---
    fun selectTool(tool: EditorTool) {
        _uiState.update {
            it.copy(
                currentTool = tool,
                selectedAnchorIndex = null,
                isEditingHandle = false
            )
        }
    }

    fun setShiftPressed(pressed: Boolean) {
        _uiState.update { it.copy(isShiftPressed = pressed) }
    }

    fun setAltPressed(pressed: Boolean) {
        _uiState.update { it.copy(isAltPressed = pressed) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleSnapToGrid() {
        _uiState.update { it.copy(snapToGrid = !it.snapToGrid) }
    }

    fun toggleSoftProof() {
        _uiState.update { it.copy(softProofCmyk = !it.softProofCmyk) }
    }

    fun toggleBleed() {
        _uiState.update { it.copy(showBleed = !it.showBleed) }
    }

    fun toggleRulers() {
        _uiState.update { it.copy(showRulers = !it.showRulers) }
    }

    fun toggleTransparencyGrid() {
        _uiState.update { it.copy(showTransparencyGrid = !it.showTransparencyGrid) }
    }

    fun setDocumentUnits(unit: DocumentUnit) {
        val currentDoc = _uiState.value.document
        val updatedDoc = currentDoc.copy(units = unit, lastModified = System.currentTimeMillis())
        _uiState.update { it.copy(document = updatedDoc, statusMessage = "Units: ${unit.displayName}") }
        repository.saveDocument(updatedDoc)
    }

    fun setEraserSubTool(subTool: EraserSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.ERASER, activeEraserSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun setRotateSubTool(subTool: RotateSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.ROTATE, activeRotateSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun setScaleSubTool(subTool: ScaleSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.SCALE, activeScaleSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun setBrushSubTool(subTool: BrushSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.BRUSH, activeBrushSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun setPencilSubTool(subTool: PencilSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.PENCIL, activePencilSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun setEyedropperSubTool(subTool: EyedropperSubTool) {
        _uiState.update { it.copy(currentTool = EditorTool.EYEDROPPER, activeEyedropperSubTool = subTool, statusMessage = "Tool: ${subTool.displayName}") }
    }

    fun rotateSelection(degrees: Float) {
        val selectedIds = _uiState.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        saveStateForUndo()
        val doc = _uiState.value.document
        val selElements = doc.layers.flatMap { it.elements }.filter { it.id in selectedIds }
        if (selElements.isEmpty()) return
        var collectiveBounds = selElements.first().getBoundingBox()
        for (i in 1 until selElements.size) collectiveBounds = collectiveBounds.union(selElements[i].getBoundingBox())
        val cx = collectiveBounds.centerX
        val cy = collectiveBounds.centerY
        mutateSelectedElements { el ->
            el.withTransform(el.transform.rotate(degrees, cx, cy))
        }
    }

    fun scaleSelection(factor: Float) {
        val selectedIds = _uiState.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        saveStateForUndo()
        val doc = _uiState.value.document
        val selElements = doc.layers.flatMap { it.elements }.filter { it.id in selectedIds }
        if (selElements.isEmpty()) return
        var collectiveBounds = selElements.first().getBoundingBox()
        for (i in 1 until selElements.size) collectiveBounds = collectiveBounds.union(selElements[i].getBoundingBox())
        val cx = collectiveBounds.centerX
        val cy = collectiveBounds.centerY
        mutateSelectedElements { el ->
            el.withTransform(el.transform.scale(factor, factor, cx, cy))
        }
    }

    fun setActivePanel(panel: EditorPanel) {
        _uiState.update {
            it.copy(activePanel = if (it.activePanel == panel) EditorPanel.NONE else panel)
        }
    }

    fun setPaintTarget(target: PaintTarget) {
        _uiState.update { it.copy(activePaintTarget = target) }
    }

    fun setActiveShapeType(type: ShapeType) {
        _uiState.update { it.copy(activeShapeType = type) }
    }

    // --- CANVAS ZOOM & PAN ---
    fun updateZoomAndPan(
        zoomDelta: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        focalX: Float? = null,
        focalY: Float? = null
    ) {
        _uiState.update { current ->
            val oldZoom = current.zoom
            val newZoom = (oldZoom * zoomDelta).coerceIn(0.08f, 32.0f)

            val newPanX: Float
            val newPanY: Float
            if (focalX != null && focalY != null && oldZoom > 0f) {
                val docX = (focalX - current.panX) / oldZoom
                val docY = (focalY - current.panY) / oldZoom
                newPanX = focalX - docX * newZoom + panDeltaX
                newPanY = focalY - docY * newZoom + panDeltaY
            } else {
                newPanX = current.panX + panDeltaX
                newPanY = current.panY + panDeltaY
            }

            current.copy(
                zoom = newZoom,
                panX = newPanX,
                panY = newPanY
            )
        }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoom = (it.zoom * 1.25f).coerceAtMost(32.0f)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(zoom = (it.zoom / 1.25f).coerceAtLeast(0.1f)) }
    }

    fun zoomFit() {
        val doc = _uiState.value.document
        val ab = doc.activeArtboard()
        centerArtboard(ab)
    }

    // --- SELECTION & ELEMENT MUTATIONS ---
    fun selectElement(id: String?, addToSelection: Boolean = false) {
        _uiState.update {
            val newSelection = if (id == null) {
                if (addToSelection) it.selectedElementIds else emptySet()
            } else {
                if (addToSelection) it.selectedElementIds + id else setOf(id)
            }
            it.copy(
                selectedElementIds = newSelection,
                selectedAnchorIndex = null
            )
        }
    }

    fun selectAll() {
        val allIds = _uiState.value.document.layers.flatMap { it.elements.map { el -> el.id } }.toSet()
        _uiState.update { it.copy(selectedElementIds = allIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedElementIds = emptySet(), selectedAnchorIndex = null) }
    }

    fun updateSelectedColor(color: VpColor) {
        saveStateForUndo()
        val target = _uiState.value.activePaintTarget
        val selectedIds = _uiState.value.selectedElementIds

        if (target == PaintTarget.FILL) {
            _uiState.update { it.copy(activeFill = VpPaint.Solid(color)) }
        } else {
            _uiState.update { it.copy(activeStroke = VpPaint.Solid(color)) }
        }

        if (selectedIds.isEmpty()) return

        mutateSelectedElements { el ->
            if (target == PaintTarget.FILL) {
                el.withFill(VpPaint.Solid(color))
            } else {
                el.withStroke(VpPaint.Solid(color))
            }
        }
    }

    fun updateSelectedStrokeWidth(width: Float) {
        saveStateForUndo()
        _uiState.update {
            it.copy(activeStrokeStyle = it.activeStrokeStyle.copy(width = width))
        }
        mutateSelectedElements { el ->
            el.withStroke(el.stroke, el.strokeStyle.copy(width = width))
        }
    }

    fun updateSelectedOpacity(opacity: Float) {
        saveStateForUndo()
        mutateSelectedElements { el ->
            el.withOpacity(opacity)
        }
    }

    fun deleteSelectedElements() {
        if (_uiState.value.selectedElementIds.isEmpty()) return
        saveStateForUndo()
        val selected = _uiState.value.selectedElementIds
        val currentDoc = _uiState.value.document
        val updatedLayers = currentDoc.layers.map { layer ->
            layer.copy(elements = layer.elements.filterNot { it.id in selected })
        }
        _uiState.update {
            it.copy(
                document = currentDoc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()),
                selectedElementIds = emptySet()
            )
        }
    }

    fun duplicateSelectedElements() {
        val selected = _uiState.value.selectedElementIds
        if (selected.isEmpty()) return
        saveStateForUndo()
        val currentDoc = _uiState.value.document
        val newSelection = mutableSetOf<String>()
        val updatedLayers = currentDoc.layers.map { layer ->
            val newElements = layer.elements.toMutableList()
            for (el in layer.elements) {
                if (el.id in selected) {
                    val dup = el.withTransform(el.transform.translate(20f, 20f))
                    // Re-instantiate with new ID
                    val elementWithNewId = when (dup) {
                        is ShapeElement -> dup.copy(id = UUID.randomUUID().toString(), name = "${dup.name} Copy")
                        is PathElement -> dup.copy(id = UUID.randomUUID().toString(), name = "${dup.name} Copy")
                        is TextElement -> dup.copy(id = UUID.randomUUID().toString(), name = "${dup.name} Copy")
                        is GroupElement -> dup.copy(id = UUID.randomUUID().toString(), name = "${dup.name} Copy")
                    }
                    newElements.add(elementWithNewId)
                    newSelection.add(elementWithNewId.id)
                }
            }
            layer.copy(elements = newElements)
        }
        _uiState.update {
            it.copy(
                document = currentDoc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()),
                selectedElementIds = newSelection
            )
        }
    }

    fun bringForward() {
        reorderSelected(1)
    }

    fun sendBackward() {
        reorderSelected(-1)
    }

    private fun reorderSelected(step: Int) {
        saveStateForUndo()
        val selected = _uiState.value.selectedElementIds
        val doc = _uiState.value.document
        val updatedLayers = doc.layers.map { layer ->
            val list = layer.elements.toMutableList()
            val indices = list.mapIndexedNotNull { index, el -> if (el.id in selected) index else null }
            if (step > 0) {
                for (i in indices.reversed()) {
                    if (i < list.size - 1) {
                        val item = list.removeAt(i)
                        list.add(i + 1, item)
                    }
                }
            } else {
                for (i in indices) {
                    if (i > 0) {
                        val item = list.removeAt(i)
                        list.add(i - 1, item)
                    }
                }
            }
            layer.copy(elements = list)
        }
        _uiState.update {
            it.copy(document = doc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()))
        }
    }

    // --- ALIGNMENT ---
    fun alignSelected(alignment: String) {
        val selected = _uiState.value.selectedElementIds
        if (selected.isEmpty()) return
        saveStateForUndo()

        val doc = _uiState.value.document
        val artboard = doc.activeArtboard()
        val elements = doc.layers.flatMap { it.elements }.filter { it.id in selected }
        if (elements.isEmpty()) return

        val refBounds = if (elements.size == 1) {
            // Align to artboard
            Rect2D(0f, 0f, artboard.width, artboard.height)
        } else {
            // Align to collective selection bounding box
            var b = elements.first().getBoundingBox()
            for (i in 1 until elements.size) b = b.union(elements[i].getBoundingBox())
            b
        }

        mutateSelectedElements { el ->
            val elB = el.getBoundingBox()
            val (dx, dy) = when (alignment) {
                "LEFT" -> Pair(refBounds.left - elB.left, 0f)
                "CENTER_H" -> Pair(refBounds.centerX - elB.centerX, 0f)
                "RIGHT" -> Pair(refBounds.right - elB.right, 0f)
                "TOP" -> Pair(0f, refBounds.top - elB.top)
                "CENTER_V" -> Pair(0f, refBounds.centerY - elB.centerY)
                "BOTTOM" -> Pair(0f, refBounds.bottom - elB.bottom)
                else -> Pair(0f, 0f)
            }
            el.withTransform(el.transform.translate(dx, dy))
        }
    }

    // --- PATHFINDER ---
    fun runPathfinder(operation: PathfinderOps.Operation) {
        val selected = _uiState.value.selectedElementIds
        if (selected.size < 2) {
            _uiState.update { it.copy(statusMessage = "Select at least 2 shapes for Pathfinder") }
            return
        }
        saveStateForUndo()

        val doc = _uiState.value.document
        val elements = doc.layers.flatMap { it.elements }.filter { it.id in selected }
        if (elements.size < 2) return

        val bottom = elements[0]
        val top = elements[1]

        val result = PathfinderOps.combineElements(bottom, top, operation)
        if (result == null) {
            _uiState.update { it.copy(statusMessage = "Pathfinder produced no overlapping area") }
            return
        }

        // Replace bottom and top with result in active layer
        val activeLayer = doc.layers.first()
        val updatedLayers = doc.layers.map { layer ->
            val filtered = layer.elements.filterNot { it.id in selected }
            if (layer.id == activeLayer.id) {
                layer.copy(elements = filtered + result)
            } else {
                layer.copy(elements = filtered)
            }
        }

        _uiState.update {
            it.copy(
                document = doc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()),
                selectedElementIds = setOf(result.id),
                statusMessage = "Pathfinder ${operation.name.lowercase()} applied"
            )
        }
    }

    // --- ADDING NEW SHAPES & DRAWING ---
    fun addShapeAt(bounds: Rect2D, shapeType: ShapeType = _uiState.value.activeShapeType) {
        saveStateForUndo()
        val newShape = ShapeElement(
            id = UUID.randomUUID().toString(),
            name = "${shapeType.displayName} ${_uiState.value.document.layers.sumOf { it.elements.size } + 1}",
            shapeType = shapeType,
            bounds = bounds,
            fill = _uiState.value.activeFill,
            stroke = _uiState.value.activeStroke,
            strokeStyle = _uiState.value.activeStrokeStyle
        )
        insertElementIntoActiveLayer(newShape)
    }

    fun addPath(segments: List<PathSegment>, closed: Boolean = true) {
        if (segments.size < 2) return
        saveStateForUndo()
        val newPath = PathElement(
            id = UUID.randomUUID().toString(),
            name = "Path ${_uiState.value.document.layers.sumOf { it.elements.size } + 1}",
            segments = segments,
            closed = closed,
            fill = if (closed) _uiState.value.activeFill else VpPaint.None,
            stroke = _uiState.value.activeStroke,
            strokeStyle = _uiState.value.activeStrokeStyle
        )
        insertElementIntoActiveLayer(newPath)
    }

    fun addLineAt(start: Point2D, end: Point2D) {
        saveStateForUndo()
        val line = ShapeElement(
            id = UUID.randomUUID().toString(),
            name = "Line ${_uiState.value.document.layers.sumOf { it.elements.size } + 1}",
            shapeType = ShapeType.LINE,
            bounds = Rect2D(start.x, start.y, end.x, end.y),
            fill = VpPaint.None,
            stroke = _uiState.value.activeStroke,
            strokeStyle = _uiState.value.activeStrokeStyle
        )
        insertElementIntoActiveLayer(line)
    }

    fun breakOrRetractPenHandle() {
        val currentSegs = activeDrawingSegments.value
        if (currentSegs.isNotEmpty()) {
            val last = currentSegs.last()
            val updatedLast = last.copy(
                outHandle = Point2D(0f, 0f),
                isSmooth = false
            )
            activeDrawingSegments.value = currentSegs.dropLast(1) + updatedLast
            _uiState.update { it.copy(statusMessage = "Bézier Handle Split (Corner Anchor [Alt])") }
        }
    }

    fun applyLinearGradient(start: Point2D, end: Point2D) {
        val selected = _uiState.value.selectedElementIds
        if (selected.isEmpty()) return
        saveStateForUndo()
        val angle = start.angleTo(end)
        val grad = VpPaint.Linear(
            stops = listOf(
                GradientStop(0f, VpColor.rgb(0, 229, 255)),
                GradientStop(1f, VpColor.rgb(179, 136, 255))
            ),
            angleDegrees = angle
        )
        mutateSelectedElements { el -> el.withFill(grad) }
        _uiState.update { it.copy(activeFill = grad, statusMessage = "Gradient Applied") }
    }

    fun runShapeBuilderOnSelected() {
        val selected = _uiState.value.selectedElementIds
        if (selected.size >= 2) {
            runPathfinder(PathfinderOps.Operation.UNITE)
        } else {
            _uiState.update { it.copy(statusMessage = "Shape Builder: Select 2 or more overlapping shapes to merge") }
        }
    }

    fun addText(text: String, x: Float, y: Float) {
        saveStateForUndo()
        val newText = TextElement(
            id = UUID.randomUUID().toString(),
            name = "Text: ${text.take(12)}",
            text = text,
            x = x,
            y = y,
            fontSize = 24f,
            fill = _uiState.value.activeFill
        )
        insertElementIntoActiveLayer(newText)
    }

    private fun insertElementIntoActiveLayer(element: VectorElement) {
        val currentDoc = _uiState.value.document
        val firstLayer = currentDoc.layers.first()
        val updatedLayers = currentDoc.layers.map { layer ->
            if (layer.id == firstLayer.id) {
                layer.copy(elements = layer.elements + element)
            } else {
                layer
            }
        }
        _uiState.update {
            it.copy(
                document = currentDoc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()),
                selectedElementIds = setOf(element.id)
            )
        }
        repository.saveDocument(_uiState.value.document)
    }

    private fun mutateSelectedElements(transform: (VectorElement) -> VectorElement) {
        val selected = _uiState.value.selectedElementIds
        val currentDoc = _uiState.value.document
        val updatedLayers = currentDoc.layers.map { layer ->
            layer.copy(
                elements = layer.elements.map { el ->
                    if (el.id in selected) transform(el) else el
                }
            )
        }
        _uiState.update {
            it.copy(document = currentDoc.copy(layers = updatedLayers, lastModified = System.currentTimeMillis()))
        }
    }

    // --- DIRECT SELECTION EDITING ---
    fun selectAnchor(index: Int?) {
        _uiState.update { it.copy(selectedAnchorIndex = index) }
    }

    fun updateSelectedAnchor(newAnchor: Point2D) {
        val selectedIds = _uiState.value.selectedElementIds
        val anchorIdx = _uiState.value.selectedAnchorIndex ?: return
        if (selectedIds.isEmpty()) return

        mutateSelectedElements { el ->
            if (el is PathElement && anchorIdx in el.segments.indices) {
                val newSegs = el.segments.toMutableList()
                newSegs[anchorIdx] = newSegs[anchorIdx].copy(anchor = newAnchor)
                el.copy(segments = newSegs)
            } else el
        }
    }

    fun setPenSubTool(subTool: PenSubTool) {
        _uiState.update {
            it.copy(
                currentTool = EditorTool.PEN,
                activePenSubTool = subTool,
                statusMessage = "Tool: ${subTool.displayName}"
            )
        }
    }

    fun addAnchorToSelectedPath(docPt: Point2D) {
        val selectedIds = _uiState.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        saveStateForUndo()
        mutateSelectedElements { el ->
            if (el is PathElement && el.segments.size >= 2) {
                val segs = el.segments.toMutableList()
                var bestIdx = 1
                var bestDist = Float.MAX_VALUE
                for (i in 0 until segs.size) {
                    val nextIdx = (i + 1) % segs.size
                    val p1 = el.transform.mapPoint(segs[i].anchor)
                    val p2 = el.transform.mapPoint(segs[nextIdx].anchor)
                    val mid = Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                    val dist = docPt.distanceTo(mid)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIdx = i + 1
                    }
                }
                segs.add(bestIdx, PathSegment(anchor = docPt, isSmooth = true))
                el.copy(segments = segs)
            } else el
        }
        _uiState.update { it.copy(statusMessage = "Anchor Point Added (+)") }
    }

    fun deleteAnchorFromSelectedPath(anchorIdx: Int) {
        val selectedIds = _uiState.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        saveStateForUndo()
        mutateSelectedElements { el ->
            if (el is PathElement && el.segments.size > 2 && anchorIdx in el.segments.indices) {
                val segs = el.segments.toMutableList()
                segs.removeAt(anchorIdx)
                el.copy(segments = segs)
            } else el
        }
        _uiState.update { it.copy(selectedAnchorIndex = null, statusMessage = "Anchor Point Deleted (-)") }
    }

    fun convertAnchorOnSelectedPath(anchorIdx: Int) {
        val selectedIds = _uiState.value.selectedElementIds
        if (selectedIds.isEmpty()) return
        saveStateForUndo()
        mutateSelectedElements { el ->
            if (el is PathElement && anchorIdx in el.segments.indices) {
                val segs = el.segments.toMutableList()
                val target = segs[anchorIdx]
                val converted = if (target.hasInHandle() || target.hasOutHandle()) {
                    target.copy(inHandle = Point2D(0f, 0f), outHandle = Point2D(0f, 0f), isSmooth = false)
                } else {
                    target.copy(inHandle = Point2D(-24f, 0f), outHandle = Point2D(24f, 0f), isSmooth = true)
                }
                segs[anchorIdx] = converted
                el.copy(segments = segs)
            } else el
        }
        _uiState.update { it.copy(statusMessage = "Anchor Point Converted (Corner/Smooth)") }
    }

    fun updateSelectedHandle(handleType: Int, newHandleOffset: Point2D) {
        val selectedIds = _uiState.value.selectedElementIds
        val anchorIdx = _uiState.value.selectedAnchorIndex ?: return
        if (selectedIds.isEmpty()) return

        mutateSelectedElements { el ->
            if (el is PathElement && anchorIdx in el.segments.indices) {
                val segs = el.segments.toMutableList()
                val current = segs[anchorIdx]
                val updated = if (handleType == 1) { // Out handle
                    if (_uiState.value.isAltPressed) {
                        current.copy(outHandle = newHandleOffset, isSmooth = false)
                    } else {
                        current.copy(
                            outHandle = newHandleOffset,
                            inHandle = Point2D(-newHandleOffset.x, -newHandleOffset.y),
                            isSmooth = true
                        )
                    }
                } else { // In handle
                    if (_uiState.value.isAltPressed) {
                        current.copy(inHandle = newHandleOffset, isSmooth = false)
                    } else {
                        current.copy(
                            inHandle = newHandleOffset,
                            outHandle = Point2D(-newHandleOffset.x, -newHandleOffset.y),
                            isSmooth = true
                        )
                    }
                }
                segs[anchorIdx] = updated
                el.copy(segments = segs)
            } else el
        }
    }

    // --- SWATCH MANAGEMENT ---
    fun addSwatch(swatch: Swatch) {
        val doc = _uiState.value.document
        val updatedSwatches = doc.swatches + swatch
        _uiState.update {
            it.copy(document = doc.copy(swatches = updatedSwatches, lastModified = System.currentTimeMillis()))
        }
    }

    // --- EXPORT TRIGGER ---
    fun exportCurrentArtwork(config: VectorExporter.ExportConfig) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val result = VectorExporter.exportDocument(
                    context = getApplication(),
                    document = _uiState.value.document,
                    config = config
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = result,
                        statusMessage = "Exported ${config.format.name} (${result.widthPx}x${result.heightPx} px, ${result.fileSizeFormatted})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Export failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
