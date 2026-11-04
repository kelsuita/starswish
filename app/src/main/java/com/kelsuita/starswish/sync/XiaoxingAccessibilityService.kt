package com.kelsuita.starswish.sync

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kelsuita.starswish.data.BalanceCandidate
import com.kelsuita.starswish.data.CaptureRequest
import com.kelsuita.starswish.data.SavingsStore

class XiaoxingAccessibilityService : AccessibilityService() {
    private val store by lazy { SavingsStore(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val capsule by lazy { BalanceReadCapsule(this) }
    private var detailWindowId: Int? = null
    private var readPending = false
    // Accessibility events can arrive in bursts; avoid re-parsing the request JSON on each one.
    private var cachedRequest: CaptureRequest? = null
    private var cachedRequestAt = 0L
    private fun currentRequest(): CaptureRequest? {
        val now = System.currentTimeMillis()
        if (now - cachedRequestAt >= 500) {
            cachedRequest = runCatching { store.request() }.getOrNull()
            cachedRequestAt = now
        }
        return cachedRequest
    }
    private val readPage = Runnable {
        readPending = false
        captureDetail()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != XIAOXING_PACKAGE) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Content/scroll event class names describe views, not the containing Activity.
            detailWindowId = if (event.className?.toString() == ACCOUNT_DETAIL_ACTIVITY) event.windowId else null
            cachedRequestAt = 0L
            handler.removeCallbacks(readPage)
            readPending = false
        }
        if (detailWindowId != event.windowId || currentRequest() == null) return
        // Wait for the page's text nodes to populate; coalesce bursts without losing the last event.
        if (!readPending) {
            readPending = true
            handler.postDelayed(readPage, 650)
        }
    }
    private fun captureDetail() {
        val request = currentRequest() ?: return
        val now = System.currentTimeMillis()
        if (now > request.expiresAt) {
            cachedRequest = null
            cachedRequestAt = 0L
            runCatching { store.cancelCapture() }
            return
        }
        val account = store.accounts().firstOrNull { it.id == request.accountId } ?: return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != XIAOXING_PACKAGE || root.windowId != detailWindowId) return
        val lines = ArrayList<String>()
        collect(root, lines, 0)
        val balance = BalanceParser.parseAccountDetail(lines, account.name) ?: return
        // Persist only the proposed value; no balance changes until confirmed inside our app.
        store.stage(BalanceCandidate(account.id, balance, now))
        cachedRequest = null
        cachedRequestAt = 0L
        capsule.show()
    }
    private fun collect(node: AccessibilityNodeInfo, lines: MutableList<String>, depth: Int) {
        if (depth > 30 || lines.size >= 400) return
        if (node.isVisibleToUser && !node.isPassword) {
            val text = node.text?.toString()?.take(200)
            if (!text.isNullOrBlank()) lines += text
            else node.contentDescription?.toString()?.take(200)?.takeIf { it.isNotBlank() }?.let { lines += it }
        }
        for (i in 0 until node.childCount.coerceAtMost(100)) {
            val child = node.getChild(i) ?: continue
            collect(child, lines, depth + 1)
        }
    }
    override fun onInterrupt() {
        capsule.hide()
        handler.removeCallbacks(readPage)
        readPending = false
        detailWindowId = null
        cachedRequest = null
        cachedRequestAt = 0L
        runCatching { store.cancelCapture() }
    }
    override fun onDestroy() {
        capsule.hide()
        handler.removeCallbacks(readPage)
        readPending = false
        detailWindowId = null
        cachedRequest = null
        runCatching { store.cancelCapture() }
        super.onDestroy()
    }
    companion object {
        const val XIAOXING_PACKAGE = "com.cxincx.xxjz"
        const val ACCOUNT_DETAIL_ACTIVITY = "com.cxincx.xxjz.account.AccountDetailActivity"
    }
}
