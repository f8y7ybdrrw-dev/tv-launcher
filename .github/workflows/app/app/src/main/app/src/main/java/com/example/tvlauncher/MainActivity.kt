package com.example.tvlauncher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var greet: TextView
    private lateinit var clock: TextView
    private lateinit var rows: LinearLayout
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val tick = object : Runnable {
        override fun run() {
            clock.text = fmt.format(Date())
            val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            greet.text = when {
                hr < 12 -> "Good morning"
                hr < 18 -> "Good afternoon"
                else -> "Good evening"
            }
            h.postDelayed(this, 1000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = ScrollView(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0B0F1A.toInt(), 0xFF1B2440.toInt())
        )
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(32), dp(48), dp(32))
        }
        greet = TextView(this).apply {
            textSize = 22f
            setTextColor(0xCCFFFFFF.toInt())
        }
        clock = TextView(this).apply {
            textSize = 56f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
        }
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(greet)
        col.addView(clock)
        col.addView(rows)
        root.addView(col)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        build()
        h.post(tick)
    }

    override fun onPause() {
        h.removeCallbacks(tick)
        super.onPause()
    }

    override fun onBackPressed() {}

    private fun build() {
        rows.removeAllViews()
        val pm = packageManager
        fun q(cat: String) = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(cat), 0
        ).filter { it.activityInfo.packageName != packageName }
        val tv = q(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        val tvPk = tv.map { it.activityInfo.packageName }.toSet()
        val more = q(Intent.CATEGORY_LAUNCHER)
            .filter { it.activityInfo.packageName !in tvPk }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        addRow("Apps", tv)
        addRow("More", more)
    }

    private fun addRow(title: String, items: List<ResolveInfo>) {
        if (items.isEmpty()) return
        rows.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(0x99FFFFFF.toInt())
            setPadding(0, dp(24), 0, dp(8))
        })
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        items.forEach { strip.addView(tile(it)) }
        rows.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8), dp(16), dp(8), dp(16))
            addView(strip)
        })
    }

    private fun tile(ri: ResolveInfo): View {
        val pm = packageManager
        fun glass(f: Boolean) = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(if (f) 0x40FFFFFF else 0x1AFFFFFF)
            setStroke(dp(if (f) 2 else 1), if (f) 0xAAFFFFFF.toInt() else 0x33FFFFFF)
        }
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(130)).apply {
                setMargins(dp(8), 0, dp(8), 0)
            }
            background = glass(false)
            isFocusable = true
            isClickable = true
            addView(ImageView(ctx).apply {
                setImageDrawable(ri.loadIcon(pm))
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            })
            addView(TextView(ctx).apply {
                text = ri.loadLabel(pm)
                setTextColor(Color.WHITE)
                textSize = 14f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(8), dp(8), 0)
            })
            setOnFocusChangeListener { v, f ->
                v.background = glass(f)
                val s = if (f) 1.1f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(120).start()
            }
            setOnClickListener {
                startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .setComponent(ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
