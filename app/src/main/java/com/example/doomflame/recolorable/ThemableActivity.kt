package com.example.doomflame.recolorable

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toolbar
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import com.example.doomflame.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.getOrSet
import kotlin.system.measureNanoTime

val supportedAttributes = intArrayOf(
    // View
    android.R.attr.background,
    android.R.attr.foreground,
    // Text
    android.R.attr.textColor,
    android.R.attr.textColorHint,
    android.R.attr.textColorHighlight,
    android.R.attr.textColorLink,
    // Toolbar
    android.R.attr.titleTextColor,
    // Switch
    android.R.attr.track,
    android.R.attr.thumb,
).apply {
    // obtainStyledAttributes, attrs must be in ascending order
    sort()
}

class IntMap {
    var values = IntArray(0)
        private set

    var positions = IntArray(0)
        private set

    operator fun set(position: Int, value: Int) {
        val i = positions.indexOf(position)
        if (i < 0) {
            positions += position
            values += value
        } else {
            values[positions[i]] = value
        }
    }

    operator fun get(position: Int, default: Int): Int {
        return positions.indexOf(position).let {
            if (it < 0) default
            else values[it]
        }
    }

    override fun toString(): String = buildString {
        val size = positions.size
        var i = 0
        append("{")
        while (i < size) {
            append(positions[i])
            append(":")
            append(positions[i])
            if (i < size - 1) append(", ")
            i++
        }
        append("}")
    }
}

data class ColorSet(
    val style: Int,
    val attrs: IntMap,
)

class ColoringAdapter {
    @PublishedApi
    internal var theme: Resources.Theme? = null

    @PublishedApi
    internal var set: ColorSet? = null

    @PublishedApi
    internal var attrs: TypedArray? = null

    fun init(theme: Theme, set: ColorSet) {
        this.theme = theme
        this.set = set
        this.attrs = set.style
            .takeIf { it != 0 }
            ?.let { theme.obtainStyledAttributes(it, supportedAttributes) }
    }

    fun recycle() {
        theme = null
        set = null
        attrs?.recycle()
        attrs = null
    }

    inline fun typedWithReturn(attr: Int, onHit: (TypedValue) -> Boolean): Boolean {
        val set = set ?: return false
        val theme = theme ?: return false
        val attrs = attrs

        val typedValue = localTypedValue

        val themed = set.attrs[attr, 0]
        if (themed != 0 && theme.resolveAttribute(themed, typedValue, true))
            return onHit(typedValue)

        if (attrs != null) {
            val idx = attrs.getIndex(supportedAttributes.indexOf(attr))
            if (attrs.getValue(idx, typedValue)) return onHit(typedValue)
        }

        return false
    }

    inline fun color(attr: Int, onHit: (Int) -> Unit) = typedWithReturn(attr) {
        if (it.isColor) {
            onHit(it.data)
            true
        } else false
    }

    inline fun drawable(attr: Int, onHit: (Drawable) -> Unit) = typedWithReturn(attr) {
        when {
            it.isColor -> onHit(ColorDrawable(it.data))
            it.resourceId != 0 -> onHit(theme!!.getDrawable(it.resourceId))
            // Maybe need to handle other cases
            else -> {
                println("Unknown resource for attr: $attr, typedValue: $it")
                return false
            }
        }
        true
    }
}

@PublishedApi
internal val typedValueProvider = ThreadLocal<TypedValue>()

val localTypedValue: TypedValue
    get() = typedValueProvider.getOrSet { TypedValue() }

@PublishedApi
internal val coloringAdapterProvider = ThreadLocal<MutableList<ColoringAdapter>>()

inline fun withColoringAdapter(theme: Theme, set: ColorSet, action: (ColoringAdapter) -> Unit) {
    val adapterList = coloringAdapterProvider.getOrSet { mutableListOf() }
    val adapter = adapterList.removeLastOrNull() ?: ColoringAdapter()
    try {
        adapter.init(theme, set)
        action(adapter)
    } finally {
        adapter.recycle()
        adapterList.add(adapter)
    }
}

val TypedValue.isColor: Boolean
    get() = type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT

fun View.recolor(theme: Resources.Theme) {
    if (this is ViewGroup) forEach { it.recolor(theme) }

    val set = getTag(R.id.color_set) as? ColorSet ?: return
    withColoringAdapter(theme, set) { c ->
        c.drawable(android.R.attr.background) { background = it }
        c.drawable(android.R.attr.foreground) { foreground = it }

        if (this is Switch) {
//            c.drawable(android.R.attr.thumb) { thumbDrawable = it }
//            c.drawable(android.R.attr.track) { trackDrawable = it }
        }
        if (this is TextView) {
            c.color(android.R.attr.textColor) { setTextColor(it) }
            c.color(android.R.attr.textColorHint) { setHintTextColor(it) }
            c.color(android.R.attr.textColorLink) { setLinkTextColor(it) }
        }
        if (this is Toolbar) {
            c.color(android.R.attr.titleTextColor) { setTitleTextColor(it) }
        }
    }
}

private fun AttributeSet.androidAttrId(name: String): Int {
    val a = getAttributeValue("http://schemas.android.com/apk/res/android", name)
    println("Android $name got $a")
    return if (a != null && a.startsWith("?"))
        a.substring(1).toInt()
    else
        0
}

private fun AttributeSet.autoResAttrId(name: String): Int {
    val a = getAttributeValue("http://schemas.android.com/apk/res-auto", name)
    println("Autores $name got $a")
    return if (a != null && a.startsWith("?"))
        a.substring(1).toInt()
    else
        0
}

private fun AttributeSet.toColorSet(): ColorSet {
    fun AttributeSet.fillAndroid(map: IntMap, name: String, attr: Int) {
        androidAttrId(name).let { if (it > 0) map[attr] = it }
    }

    val attrs = IntMap().also { map ->
        fillAndroid(map, "background", android.R.attr.background)
        fillAndroid(map, "foreground", android.R.attr.foreground)
        fillAndroid(map, "textColor", android.R.attr.textColor)
    }
    return ColorSet(
        style = styleAttribute,
        attrs = attrs,
    )
}

class ThemingInflaterFactory(private val inflater: LayoutInflater) : LayoutInflater.Factory2 {
    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        println("Try create view $name")
        val hasPrefix = name.contains(".")
        val prefix = if (hasPrefix) ""
        else "android.widget."
        println("Inflate $prefix .. $name")
        val view = inflater.createView(name, prefix, attrs)
        return view?.also {
            println("Inflate $prefix .. $name success")
            val colorSet = attrs.toColorSet()
            it.setTag(R.id.color_set, colorSet)
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }
}

private class ThemingInflater : LayoutInflater {
    constructor(original: LayoutInflater, newContext: Context?) : super(original, newContext) {
        factory2 = ThemingInflaterFactory(original)
    }

    override fun inflate(resource: Int, root: ViewGroup?, attachToRoot: Boolean): View? {
        val counter = counters.getOrSet { AtomicBoolean(false) }
        return if (counter.compareAndSet(false, true)) {
            val name = context.resources.getResourceName(resource)
            val view: View?
            val time = measureNanoTime {
                view = super.inflate(resource, root, attachToRoot)
            }
            println("Inflate $name took ${time / 1e6} ms")
            counter.set(false)
            view
        } else super.inflate(resource, root, attachToRoot)
    }

    override fun cloneInContext(newContext: Context?): LayoutInflater =
        ThemingInflater(this, newContext)

    companion object {
        private val counters = ThreadLocal<AtomicBoolean>()
    }
}

open class ThemableActivity(
    @LayoutRes
    contentLayoutId: Int = 0
) : AppCompatActivity(contentLayoutId) {

    private val inflater by lazy {
        ThemingInflater(
            super.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            this
        )
    }

    private val contentView: View by lazy {
        findViewById(contentId)
    }

    @IdRes
    open val contentId: Int = android.R.id.content

    open fun recolor() {
        contentView.recolor(theme)
    }

    override fun setTheme(theme: Resources.Theme?) {
        if (this.theme != theme) {
            super.setTheme(theme)
            measureNanoTime {
                recolor()
            }.also {
                println("Recolor took ${it / 1e3} micros")
            }
        }
    }

    override fun setTheme(resId: Int) {
        theme = resources.newTheme().apply {
            applyStyle(resId, true)
        }
    }

    override fun getLayoutInflater(): LayoutInflater = inflater

    override fun getSystemService(name: String): Any? {
        return if (name == Context.LAYOUT_INFLATER_SERVICE) inflater
        else super.getSystemService(name)
    }

}