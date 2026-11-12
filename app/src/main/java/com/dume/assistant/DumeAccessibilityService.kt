package com.dume.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Phone-control bridge for commands explicitly issued by the user.
 * This service does not monitor or act on screens on its own.
 */
class DumeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var activeService: DumeAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally passive. Actions happen only after a user command.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                     else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val target = findScrollable(root)
        val done = target?.performAction(action) == true
        root.recycle()
        if (done) return true

        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = if (down) metrics.heightPixels * 0.78f else metrics.heightPixels * 0.25f
        val endY = if (down) metrics.heightPixels * 0.25f else metrics.heightPixels * 0.78f
        val path = Path().apply { moveTo(x, startY); lineTo(x, endY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun clickVisibleLabel(label: String, allowSensitive: Boolean = false): Boolean {
        val normalized = label.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false
        val sensitive = listOf("send", "pay", "delete", "purchase", "buy", "transfer")
        if (!allowSensitive && sensitive.any { normalized == it || normalized.contains(it) }) {
            return false
        }

        val root = rootInActiveWindow ?: return false
        val node = findLabel(root, normalized)
        val clicked = node?.let { clickNodeOrParent(it) } == true
        root.recycle()
        return clicked
    }

    fun readFocusedFieldText(): String? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val value = focused?.text?.toString()?.takeIf { it.isNotBlank() }
        focused?.recycle()
        root.recycle()
        return value
    }

    fun typeIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val done = if (focused != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } else false
        focused?.recycle()
        root.recycle()
        return done
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    private fun findLabel(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase(Locale.ROOT).orEmpty()
        val description = node.contentDescription?.toString()?.lowercase(Locale.ROOT).orEmpty()
        if (text == label || description == label) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findLabel(child, label)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val parent = node.parent ?: return false
        val result = clickNodeOrParent(parent)
        parent.recycle()
        return result
    }
}
