import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import javax.inject.Inject

abstract class WebAssetsExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val icons: IconsHandler = objects.newInstance(IconsHandler::class.java)

        fun icons(configure: Action<in IconsHandler>) {
            configure.execute(icons)
        }
    }

abstract class IconsHandler {
    /**
     * Lucide icon name to the `Icons` enum constant generated for it. Keyed by icon
     * name so that two icons never silently collapse into one; clashing constants are
     * reported by the `generateIcons` task instead.
     */
    abstract val constants: MapProperty<String, String>

    /**
     * Bundles [lucideIcon] into the sprite, exposed as `Icons.[alias]`. [alias] defaults
     * to the PascalCase form of the icon name, and is only needed when two icons derive
     * the same constant (`arrow-down-0-1` and `arrow-down-01`, say).
     */
    fun add(
        lucideIcon: String,
        alias: String? = null,
    ) {
        val constant = alias ?: pascalCase(lucideIcon)
        require(constant.matches(IDENTIFIER)) {
            "Icon '$lucideIcon' maps to '$constant', which is not a valid Kotlin identifier."
        }
        constants.put(lucideIcon, constant)
    }
}

private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

private fun pascalCase(lucideIcon: String) =
    lucideIcon
        .split('-')
        .joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }
