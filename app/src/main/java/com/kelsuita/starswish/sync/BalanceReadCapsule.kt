package com.starsavings.app.sync

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.starsavings.app.MainActivity

/** A local accessibility overlay, not an OPPO system live notification. */
internal class BalanceReadCapsule(private val service: AccessibilityService) {
    private val windows = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var capsule: View? = null
    private val dismiss = Runnable { hide(animated = true) }

    fun show() {
        hide()
        val content = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(13), dp(18), dp(13))
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 27, 33))
                cornerRadius = dp(36).toFloat()
                setStroke(dp(1), Color.rgb(65, 68, 78))
            }
            elevation = dp(10).toFloat()
            isFocusable = true
            contentDescription = "星愿，已读取，请返回星愿确认余额。点按打开账户确认页"
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        content.addView(TextView(service).apply {
            text = "✓"
            textSize = 23f
            setTextColor(Color.rgb(129, 223, 182))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(34), WindowManager.LayoutParams.WRAP_CONTENT))
        content.addView(LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(service).apply {
                text = "已读取，请返回星愿"
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                maxWidth = (service.resources.displayMetrics.widthPixels - dp(130)).coerceAtLeast(dp(100))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(TextView(service).apply {
                text = "点按核对并确认余额  ›"
                textSize = 12f
                setTextColor(Color.rgb(185, 189, 201))
                setPadding(0, dp(3), 0, 0)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
        })
        content.setOnClickListener {
            val intent = Intent(service, MainActivity::class.java).apply {
                action = MainActivity.ACTION_CONFIRM_BALANCE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            // Keep the capsule available if the device refuses to open the Activity.
            if (runCatching { service.startActivity(intent) }.isSuccess) hide()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(12)
            setTitle("星愿 · 余额读取完成")
        }
        capsule = content
        if (runCatching { windows.addView(content, params) }.isFailure) {
            hide()
            return
        }
        content.alpha = 0f
        content.translationY = -dp(16).toFloat()
        content.scaleX = .92f
        content.scaleY = .92f
        content.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(320).setInterpolator(DecelerateInterpolator()).start()
        handler.postDelayed(dismiss, 3_000L)
    }

    fun hide(animated: Boolean = false) {
        handler.removeCallbacks(dismiss)
        val view = capsule ?: return
        view.animate().cancel()
        if (animated) {
            view.animate().alpha(0f).translationY(-dp(12).toFloat()).setDuration(200)
                .withEndAction { if (capsule === view) hide() }.start()
        } else {
            capsule = null
            runCatching { windows.removeViewImmediate(view) }
        }
    }

    private fun dp(value: Int) = (value * service.resources.displayMetrics.density + .5f).toInt()
}
