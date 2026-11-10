package com.example.repository

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }

    /**
     * Creates standard professional starter templates
     */
    fun getTemplates(): List<Document> {
        return listOf(
            createLogoTemplate(),
            createBusinessCardTemplate(),
            createPosterTemplate()
        )
    }

    private fun createLogoTemplate(): Document {
        val artboard = Artboard(
            id = "ab_logo",
            name = "Logo Board",
            x = 0f,
            y = 0f,
            width = 600f,
            height = 600f
        )

        val bgCircle = ShapeElement(
            id = "el_bg_circle",
            name = "Backdrop Circle",
            shapeType = ShapeType.ELLIPSE,
            bounds = Rect2D(100f, 100f, 500f, 500f),
            fill = VpPaint.Linear(
                stops = listOf(
                    GradientStop(0f, VpColor.rgb(18, 22, 34)),
                    GradientStop(1f, VpColor.rgb(32, 38, 56))
                )
            ),
            stroke = VpPaint.Solid(VpColor.rgb(0, 229, 255)),
            strokeStyle = StrokeStyle(width = 3f)
        )

        val starEmblem = ShapeElement(
            id = "el_star_emblem",
            name = "Hexagon Star",
            shapeType = ShapeType.STAR,
            bounds = Rect2D(200f, 180f, 400f, 380f),
            starPoints = 6,
            innerRadiusRatio = 0.6f,
            fill = VpPaint.Linear(
                stops = listOf(
                    GradientStop(0f, VpColor.rgb(0, 229, 255)),
                    GradientStop(1f, VpColor.rgb(179, 136, 255))
                )
            ),
            stroke = VpPaint.None
        )

        val brandTitle = TextElement(
            id = "el_brand_title",
            name = "Brand Title",
            text = "VERTOR PHICS",
            x = 175f,
            y = 440f,
            fontSize = 32f,
            isBold = true,
            fill = VpPaint.Solid(VpColor.rgb(255, 255, 255))
        )

        val brandSubtitle = TextElement(
            id = "el_brand_sub",
            name = "Brand Subtitle",
            text = "VECTOR DESIGN STUDIO",
            x = 205f,
            y = 475f,
            fontSize = 14f,
            isBold = false,
            fill = VpPaint.Solid(VpColor.rgb(0, 229, 255))
        )

        val layer = Layer(
            id = "l_main",
            name = "Logo Artwork",
            colorHex = 0xFF00E5FF,
            elements = listOf(bgCircle, starEmblem, brandTitle, brandSubtitle)
        )

        return Document(
            id = "doc_logo_template",
            name = "Modern Brand Logo",
            colorMode = ColorMode.RGB,
            profile = ColorProfile.SRGB,
            rasterPpi = 300,
            artboards = listOf(artboard),
            layers = listOf(layer),
            selectedArtboardId = artboard.id
        )
    }

    private fun createBusinessCardTemplate(): Document {
        // Business card: 3.5in x 2in (252pt x 144pt) with 3mm (8.5pt) bleed
        val artboard = Artboard(
            id = "ab_card",
            name = "Front Card 3.5x2in",
            x = 0f,
            y = 0f,
            width = 252f,
            height = 144f,
            bleed = Bleed(top = 8.5f, right = 8.5f, bottom = 8.5f, left = 8.5f)
        )

        val cardBg = ShapeElement(
            id = "el_card_bg",
            name = "Rich Black Base",
            shapeType = ShapeType.RECTANGLE,
            bounds = Rect2D(0f, 0f, 252f, 144f),
            fill = VpPaint.Solid(VpColor.richBlack()),
            stroke = VpPaint.None
        )

        val accentStripe = ShapeElement(
            id = "el_accent_stripe",
            name = "Cyan Press Accent",
            shapeType = ShapeType.ROUNDED_RECT,
            bounds = Rect2D(0f, 0f, 10f, 144f),
            cornerRadius = 0f,
            fill = VpPaint.Solid(VpColor.cmyk(100f, 0f, 0f, 0f)), // 100% Process Cyan
            stroke = VpPaint.None
        )

        val cardName = TextElement(
            id = "el_card_name",
            name = "Designer Name",
            text = "ALEX MORGAN",
            x = 24f,
            y = 48f,
            fontSize = 18f,
            isBold = true,
            fill = VpPaint.Solid(VpColor.cmyk(0f, 0f, 0f, 0f)) // Paper white
        )

        val cardRole = TextElement(
            id = "el_card_role",
            name = "Designer Role",
            text = "Lead Vector Art Director",
            x = 24f,
            y = 66f,
            fontSize = 10f,
            isBold = false,
            fill = VpPaint.Solid(VpColor.cmyk(100f, 0f, 0f, 0f))
        )

        val cardContact = TextElement(
            id = "el_card_contact",
            name = "Contact Info",
            text = "studio@vertorphics.art  •  +1 555 0192",
            x = 24f,
            y = 110f,
            fontSize = 8.5f,
            isBold = false,
            fill = VpPaint.Solid(VpColor.cmyk(0f, 0f, 0f, 40f))
        )

        val layer = Layer(
            id = "l_card_front",
            name = "Card Graphics",
            colorHex = 0xFFFF4081,
            elements = listOf(cardBg, accentStripe, cardName, cardRole, cardContact)
        )

        return Document(
            id = "doc_card_template",
            name = "CMYK Business Card",
            colorMode = ColorMode.CMYK,
            profile = ColorProfile.US_WEB_COATED_SWOP,
            rasterPpi = 300,
            units = DocumentUnit.IN,
            artboards = listOf(artboard),
            layers = listOf(layer),
            selectedArtboardId = artboard.id
        )
    }

    private fun createPosterTemplate(): Document {
        // A4 Poster (595.28 x 841.89 pt)
        val artboard = Artboard(
            id = "ab_poster",
            name = "A4 Poster",
            x = 0f,
            y = 0f,
            width = 595.28f,
            height = 841.89f,
            bleed = Bleed(top = 9f, right = 9f, bottom = 9f, left = 9f)
        )

        val bg = ShapeElement(
            id = "el_poster_bg",
            name = "Poster Canvas",
            shapeType = ShapeType.RECTANGLE,
            bounds = Rect2D(0f, 0f, 595.28f, 841.89f),
            fill = VpPaint.Solid(VpColor.rgb(15, 17, 23)),
            stroke = VpPaint.None
        )

        val polyMesh = ShapeElement(
            id = "el_poly_mesh",
            name = "Center Hexagon",
            shapeType = ShapeType.POLYGON,
            bounds = Rect2D(147f, 250f, 447f, 550f),
            sides = 8,
            fill = VpPaint.Linear(
                stops = listOf(
                    GradientStop(0f, VpColor.rgb(0, 229, 255)),
                    GradientStop(1f, VpColor.rgb(255, 64, 129))
                )
            ),
            stroke = VpPaint.Solid(VpColor.rgb(255, 255, 255)),
            strokeStyle = StrokeStyle(width = 4f)
        )

        val posterTitle = TextElement(
            id = "el_poster_title",
            name = "Poster Header",
            text = "VECTOR FORUM 2026",
            x = 80f,
            y = 160f,
            fontSize = 38f,
            isBold = true,
            fill = VpPaint.Solid(VpColor.rgb(255, 255, 255))
        )

        val posterDate = TextElement(
            id = "el_poster_date",
            name = "Event Date",
            text = "DECEMBER 12-14  •  SAN FRANCISCO",
            x = 135f,
            y = 200f,
            fontSize = 15f,
            fill = VpPaint.Solid(VpColor.rgb(0, 229, 255))
        )

        val layer = Layer(
            id = "l_poster",
            name = "Poster Design",
            colorHex = 0xFF7C4DFF,
            elements = listOf(bg, polyMesh, posterTitle, posterDate)
        )

        return Document(
            id = "doc_poster_template",
            name = "Vector Exhibition Poster",
            colorMode = ColorMode.RGB,
            profile = ColorProfile.SRGB,
            rasterPpi = 300,
            artboards = listOf(artboard),
            layers = listOf(layer),
            selectedArtboardId = artboard.id
        )
    }

    /**
     * Saves a project to local storage
     */
    fun saveDocument(document: Document) {
        val file = File(projectsDir, "${document.id}.json")
        val json = serializeDocument(document)
        file.writeText(json.toString(2))
    }

    /**
     * Loads a project from local storage
     */
    fun loadDocument(id: String): Document? {
        val file = File(projectsDir, "$id.json")
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            deserializeDocument(json)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lists all saved projects
     */
    fun listSavedProjects(): List<Document> {
        val list = mutableListOf<Document>()
        val files = projectsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        for (f in files) {
            try {
                val json = JSONObject(f.readText())
                val doc = deserializeDocument(json)
                list.add(doc)
            } catch (_: Exception) {}
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun deleteProject(id: String) {
        File(projectsDir, "$id.json").delete()
    }

    // --- JSON SERIALIZATION ---
    private fun serializeDocument(doc: Document): JSONObject {
        val root = JSONObject()
        root.put("id", doc.id)
        root.put("name", doc.name)
        root.put("colorMode", doc.colorMode.name)
        root.put("profile", doc.profile.name)
        root.put("rasterPpi", doc.rasterPpi)
        root.put("units", doc.units.name)
        root.put("selectedArtboardId", doc.selectedArtboardId)
        root.put("lastModified", doc.lastModified)

        // Artboards
        val abArray = JSONArray()
        for (ab in doc.artboards) {
            val abObj = JSONObject()
            abObj.put("id", ab.id)
            abObj.put("name", ab.name)
            abObj.put("x", ab.x)
            abObj.put("y", ab.y)
            abObj.put("width", ab.width)
            abObj.put("height", ab.height)
            val bleedObj = JSONObject()
            bleedObj.put("top", ab.bleed.top)
            bleedObj.put("right", ab.bleed.right)
            bleedObj.put("bottom", ab.bleed.bottom)
            bleedObj.put("left", ab.bleed.left)
            abObj.put("bleed", bleedObj)
            abArray.put(abObj)
        }
        root.put("artboards", abArray)

        // Layers & Elements
        val layerArray = JSONArray()
        for (l in doc.layers) {
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("name", l.name)
            lObj.put("colorHex", l.colorHex)
            lObj.put("visible", l.visible)
            lObj.put("locked", l.locked)

            val elemArray = JSONArray()
            for (el in l.elements) {
                elemArray.put(serializeElement(el))
            }
            lObj.put("elements", elemArray)
            layerArray.put(lObj)
        }
        root.put("layers", layerArray)

        return root
    }

    private fun serializeElement(element: VectorElement): JSONObject {
        val obj = JSONObject()
        obj.put("id", element.id)
        obj.put("name", element.name)
        obj.put("opacity", element.opacity)
        obj.put("locked", element.isLocked)
        obj.put("hidden", element.isHidden)

        when (element) {
            is ShapeElement -> {
                obj.put("type", "shape")
                obj.put("shapeType", element.shapeType.name)
                obj.put("left", element.bounds.left)
                obj.put("top", element.bounds.top)
                obj.put("right", element.bounds.right)
                obj.put("bottom", element.bounds.bottom)
                obj.put("cornerRadius", element.cornerRadius)
                obj.put("sides", element.sides)
                obj.put("starPoints", element.starPoints)
            }
            is PathElement -> {
                obj.put("type", "path")
                obj.put("closed", element.closed)
                val segArray = JSONArray()
                for (s in element.segments) {
                    val sObj = JSONObject()
                    sObj.put("ax", s.anchor.x)
                    sObj.put("ay", s.anchor.y)
                    sObj.put("inX", s.inHandle.x)
                    sObj.put("inY", s.inHandle.y)
                    sObj.put("outX", s.outHandle.x)
                    sObj.put("outY", s.outHandle.y)
                    segArray.put(sObj)
                }
                obj.put("segments", segArray)
            }
            is TextElement -> {
                obj.put("type", "text")
                obj.put("text", element.text)
                obj.put("x", element.x)
                obj.put("y", element.y)
                obj.put("fontSize", element.fontSize)
                obj.put("isBold", element.isBold)
            }
            is GroupElement -> {
                obj.put("type", "group")
                val chArray = JSONArray()
                for (c in element.children) {
                    chArray.put(serializeElement(c))
                }
                obj.put("children", chArray)
            }
        }

        // Color / Paint
        if (element.fill is VpPaint.Solid) {
            val f = (element.fill as VpPaint.Solid).color
            val fObj = JSONObject()
            fObj.put("mode", f.mode.name)
            val valArray = JSONArray()
            f.values.forEach { valArray.put(it) }
            fObj.put("values", valArray)
            fObj.put("alpha", f.alpha)
            obj.put("fillSolid", fObj)
        }

        return obj
    }

    private fun deserializeDocument(json: JSONObject): Document {
        val id = json.getString("id")
        val name = json.getString("name")
        val colorMode = ColorMode.valueOf(json.optString("colorMode", "RGB"))
        val profile = ColorProfile.valueOf(json.optString("profile", "SRGB"))
        val rasterPpi = json.optInt("rasterPpi", 300)
        val units = DocumentUnit.valueOf(json.optString("units", "PT"))
        val selectedArtboardId = json.optString("selectedArtboardId", "")
        val lastModified = json.optLong("lastModified", System.currentTimeMillis())

        // Artboards
        val artboards = mutableListOf<Artboard>()
        val abArray = json.optJSONArray("artboards") ?: JSONArray()
        for (i in 0 until abArray.length()) {
            val o = abArray.getJSONObject(i)
            val bObj = o.optJSONObject("bleed")
            val bleed = if (bObj != null) {
                Bleed(
                    top = bObj.optDouble("top", 9.0).toFloat(),
                    right = bObj.optDouble("right", 9.0).toFloat(),
                    bottom = bObj.optDouble("bottom", 9.0).toFloat(),
                    left = bObj.optDouble("left", 9.0).toFloat()
                )
            } else Bleed()
            artboards.add(
                Artboard(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    x = o.getDouble("x").toFloat(),
                    y = o.getDouble("y").toFloat(),
                    width = o.getDouble("width").toFloat(),
                    height = o.getDouble("height").toFloat(),
                    bleed = bleed
                )
            )
        }

        // Layers
        val layers = mutableListOf<Layer>()
        val lArray = json.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until lArray.length()) {
            val lo = lArray.getJSONObject(i)
            val elements = mutableListOf<VectorElement>()
            val elArray = lo.optJSONArray("elements") ?: JSONArray()
            for (j in 0 until elArray.length()) {
                val eo = elArray.getJSONObject(j)
                deserializeElement(eo)?.let { elements.add(it) }
            }
            layers.add(
                Layer(
                    id = lo.getString("id"),
                    name = lo.getString("name"),
                    colorHex = lo.optLong("colorHex", 0xFF00E5FF),
                    visible = lo.optBoolean("visible", true),
                    locked = lo.optBoolean("locked", false),
                    elements = elements
                )
            )
        }

        return Document(
            id = id,
            name = name,
            colorMode = colorMode,
            profile = profile,
            rasterPpi = rasterPpi,
            units = units,
            artboards = if (artboards.isEmpty()) listOf(Artboard()) else artboards,
            layers = if (layers.isEmpty()) listOf(Layer()) else layers,
            selectedArtboardId = selectedArtboardId.ifEmpty { artboards.firstOrNull()?.id ?: "" },
            lastModified = lastModified
        )
    }

    private fun deserializeElement(obj: JSONObject): VectorElement? {
        val type = obj.optString("type")
        val id = obj.getString("id")
        val name = obj.getString("name")
        val opacity = obj.optDouble("opacity", 1.0).toFloat()
        val locked = obj.optBoolean("locked", false)
        val hidden = obj.optBoolean("hidden", false)

        // Parse solid fill if present
        var fillPaint: VpPaint = VpPaint.Solid(VpColor.rgb(0, 229, 255))
        if (obj.has("fillSolid")) {
            val fObj = obj.getJSONObject("fillSolid")
            val mode = ColorMode.valueOf(fObj.getString("mode"))
            val valArr = fObj.getJSONArray("values")
            val vals = mutableListOf<Float>()
            for (k in 0 until valArr.length()) vals.add(valArr.getDouble(k).toFloat())
            val alpha = fObj.optDouble("alpha", 1.0).toFloat()
            fillPaint = VpPaint.Solid(VpColor(mode, vals, alpha))
        }

        return when (type) {
            "shape" -> {
                val shapeType = ShapeType.valueOf(obj.optString("shapeType", "RECTANGLE"))
                val bounds = Rect2D(
                    obj.getDouble("left").toFloat(),
                    obj.getDouble("top").toFloat(),
                    obj.getDouble("right").toFloat(),
                    obj.getDouble("bottom").toFloat()
                )
                val cornerRadius = obj.optDouble("cornerRadius", 0.0).toFloat()
                val sides = obj.optInt("sides", 6)
                val starPoints = obj.optInt("starPoints", 5)
                ShapeElement(
                    id = id,
                    name = name,
                    shapeType = shapeType,
                    bounds = bounds,
                    cornerRadius = cornerRadius,
                    sides = sides,
                    starPoints = starPoints,
                    fill = fillPaint,
                    opacity = opacity,
                    isLocked = locked,
                    isHidden = hidden
                )
            }
            "path" -> {
                val closed = obj.optBoolean("closed", true)
                val segArray = obj.optJSONArray("segments") ?: JSONArray()
                val segments = mutableListOf<PathSegment>()
                for (s in 0 until segArray.length()) {
                    val so = segArray.getJSONObject(s)
                    segments.add(
                        PathSegment(
                            anchor = Point2D(so.getDouble("ax").toFloat(), so.getDouble("ay").toFloat()),
                            inHandle = Point2D(so.getDouble("inX").toFloat(), so.getDouble("inY").toFloat()),
                            outHandle = Point2D(so.getDouble("outX").toFloat(), so.getDouble("outY").toFloat())
                        )
                    )
                }
                PathElement(
                    id = id,
                    name = name,
                    segments = segments,
                    closed = closed,
                    fill = fillPaint,
                    opacity = opacity,
                    isLocked = locked,
                    isHidden = hidden
                )
            }
            "text" -> {
                val text = obj.optString("text", "Text")
                val x = obj.getDouble("x").toFloat()
                val y = obj.getDouble("y").toFloat()
                val fontSize = obj.optDouble("fontSize", 24.0).toFloat()
                val isBold = obj.optBoolean("isBold", false)
                TextElement(
                    id = id,
                    name = name,
                    text = text,
                    x = x,
                    y = y,
                    fontSize = fontSize,
                    isBold = isBold,
                    fill = fillPaint,
                    opacity = opacity,
                    isLocked = locked,
                    isHidden = hidden
                )
            }
            else -> null
        }
    }
}
