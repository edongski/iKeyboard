package com.ikey.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent

class IKeyService : InputMethodService() {
    private var keyboard: IKeyView? = null
    private var shifted = false
    private var caps = false
    private var symbols = false
    private var numeric = false
    override fun onCreateInputView(): View {
        keyboard = IKeyView(this) { key -> handle(key) }
        refresh()
        return keyboard!!
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shifted = false; caps = false; symbols = false
        val type = attribute?.inputType ?: 0
        numeric = (type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER || (type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_PHONE || (type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_DATETIME
        refresh()
    }
    private fun refresh() { keyboard?.update(symbols, shifted || caps, numeric, getSharedPreferences("ikey", MODE_PRIVATE).getBoolean("dark",false)) }
    private fun handle(key: String) {
        val ic = currentInputConnection ?: return
        if (key.startsWith("CURSOR_LEFT:") || key.startsWith("CURSOR_RIGHT:")) {
            val right = key.startsWith("CURSOR_RIGHT:")
            val count = key.substringAfter(":").toIntOrNull()?.coerceIn(1,20) ?: return
            repeat(count) {
                val code = if (right) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
            return
        }
        when(key) {
            "⇧" -> { if (shifted) { caps = !caps; shifted = caps } else shifted = true; refresh() }
            "⌫" -> { val selected=ic.getSelectedText(0); if (!selected.isNullOrEmpty()) ic.commitText("",1) else ic.deleteSurroundingTextInCodePoints(1,0) }
            "?123" -> { symbols = true; refresh() }
            "ABC" -> { symbols = false; numeric = false; refresh() }
            "#+=" -> { symbols = true; refresh() }
            "space" -> ic.commitText(" ",1)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else ic.commitText("\n",1)
            }
            "🌐" -> switchToNextInputMethod(false)
            else -> {
                ic.commitText(if (!symbols && (shifted || caps)) key.uppercase() else key,1)
                if (shifted && !caps && !symbols) { shifted = false; refresh() }
            }
        }
    }
}