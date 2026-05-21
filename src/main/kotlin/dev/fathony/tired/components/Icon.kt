package dev.fathony.tired.components

import Icons
import kotlinx.html.*

sealed class SvgPaint {
    data object None : SvgPaint()

    data object CurrentColor : SvgPaint()

    data class Color(
        val value: String,
    ) : SvgPaint()

    override fun toString() =
        when (this) {
            is None -> "none"
            is CurrentColor -> "currentColor"
            is Color -> value
        }
}

enum class StrokeLinecap(
    val value: String,
) {
    Butt("butt"),
    Round("round"),
    Square("square"),
}

enum class StrokeLinejoin(
    val value: String,
) {
    Miter("miter"),
    Round("round"),
    Bevel("bevel"),
    MiterClip("miter-clip"),
    Arcs("arcs"),
}

fun FlowContent.icon(
    icon: Icons,
    size: Int = 24,
    classes: String = "",
    fill: SvgPaint = SvgPaint.None,
    stroke: SvgPaint = SvgPaint.CurrentColor,
    strokeWidth: Number = 2,
    strokeLinecap: StrokeLinecap = StrokeLinecap.Round,
    strokeLinejoin: StrokeLinejoin = StrokeLinejoin.Round,
) {
    val classAttr = if (classes.isNotEmpty()) """ class="$classes"""" else ""
    consumer.onTagContentUnsafe {
        +"""<svg
            |width="$size"
            |height="$size"
            |fill="$fill"
            |stroke="$stroke"
            |stroke-width="$strokeWidth"
            |stroke-linecap="${strokeLinecap.value}"
            |stroke-linejoin="${strokeLinejoin.value}"
            |$classAttr>
            |<use href="/${AssetManifest.icons_svg}#${icon.lucideIcon}"></use>
            |</svg>
        """.trimMargin()
            .replace(Regex("\n\\s*"), " ")
            .replace(Regex("< "), "<")
            .replace(Regex(" >"), ">")
            .replace(Regex("> <"), "><")
    }
}
