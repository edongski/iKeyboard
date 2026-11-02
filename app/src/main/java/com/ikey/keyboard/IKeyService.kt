package com.ikey.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.content.ClipboardManager
import android.content.Context

class IKeyService : InputMethodService() {
    private var keyboard: IKeyView? = null
    private var shifted = false
    private var caps = false
    private var symbols = false
    private var numeric = false
    private var suggestionsAllowed = true
    private val prefs by lazy { getSharedPreferences("ikey", MODE_PRIVATE) }
    private fun learned():Set<String> = prefs.getStringSet("learned_words",emptySet()) ?: emptySet()
    private fun contextWords():Pair<String,String> {
        val text=currentInputConnection?.getTextBeforeCursor(90,0)?.toString().orEmpty()
        val match=Regex("[\\p{L}']+$").find(text)
        val prefix=match?.value.orEmpty()
        val before=text.dropLast(prefix.length).trimEnd()
        val previous=Regex("[\\p{L}']+").findAll(before).lastOrNull()?.value.orEmpty()
        return prefix to previous
    }
    private fun updateSuggestions() {
        if(!suggestionsAllowed || numeric || symbols || !prefs.getBoolean("predictions",true)) {
            keyboard?.setSuggestions(emptyList()); return
        }
        val (prefix,previous)=contextWords()
        keyboard?.setSuggestions(WordSuggestions.candidates(prefix,previous,learned()))
    }
    private fun commitSeparator(separator:String) {
        val ic=currentInputConnection ?: return
        val (prefix,_)=contextWords()
        if(prefix.isNotBlank() && prefs.getBoolean("autocorrect",true) && suggestionsAllowed) {
            val replacement=WordSuggestions.correction(prefix,learned())
            if(replacement!=null && replacement!=prefix.lowercase()) {
                ic.deleteSurroundingText(prefix.length,0)
                ic.commitText(if(prefix.first().isUpperCase()) replacement.replaceFirstChar { it.uppercase() } else replacement,1)
            }
        }
        ic.commitText(separator,1)
    }
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
        val variation=type and InputType.TYPE_MASK_VARIATION
        suggestionsAllowed = (type and InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_TEXT &&
            variation!=InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation!=InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation!=InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD &&
            (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)==0
        refresh()
    }
    private fun refresh() { keyboard?.update(symbols, shifted || caps, numeric, prefs.getBoolean("dark",false)); updateSuggestions() }
    private fun handle(key: String) {
        val ic = currentInputConnection ?: return
        if(key.startsWith("SUGGEST:")) {
            val suggestion=key.removePrefix("SUGGEST:")
            val (prefix,_)=contextWords()
            if(suggestionsAllowed && suggestion.isNotBlank()) {
                if(prefix.isNotEmpty()) ic.deleteSurroundingText(prefix.length,0)
                ic.commitText(suggestion+" ",1)
                updateSuggestions()
            }
            return
        }
        if(key.startsWith("SWIPE_WORD:")) {
            val word=key.removePrefix("SWIPE_WORD:")
            if(word.isNotBlank()) {
                val value=if(shifted || caps) word.replaceFirstChar { it.uppercase() } else word
                ic.commitText(value+" ",1)
                if(shifted && !caps) { shifted=false; refresh() }
                updateSuggestions()
            }
            updateSuggestions()
            return
        }
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
            "😊" -> keyboard?.showEmoji()
            "📋" -> {
                if(!suggestionsAllowed) return
                val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val preview=if(clipboard.hasPrimaryClip()) "Ready to paste" else "Empty"
                keyboard?.setClipboardPreview(preview)
            }
            "PASTE" -> {
                if(!suggestionsAllowed) return
                val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip=clipboard.primaryClip
                val item=if(clip!=null && clip.itemCount>0) clip.getItemAt(0) else null
                val value=item?.coerceToText(this)?.toString()
                if(!value.isNullOrEmpty()) ic.commitText(value.take(10000),1)
                keyboard?.showLetters(); updateSuggestions()
            }
            "CLEAR" -> { keyboard?.showLetters(); updateSuggestions() }
            "⇧" -> { if (shifted) { caps = !caps; shifted = caps } else shifted = true; refresh() }
            "⌫" -> { val selected=ic.getSelectedText(0); if (!selected.isNullOrEmpty()) ic.commitText("",1) else ic.deleteSurroundingTextInCodePoints(1,0); updateSuggestions() }
            "?123" -> { symbols = true; refresh() }
            "ABC" -> { symbols = false; numeric = false; keyboard?.showLetters(); refresh() }
            "#+=" -> { symbols = true; refresh() }
            "space" -> { commitSeparator(" "); updateSuggestions() }
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else commitSeparator("\n")
                updateSuggestions()
            }
            "🌐" -> switchToNextInputMethod(false)
            else -> {
                val shortcuts=prefs.getString("shortcuts","").orEmpty().lineSequence().mapNotNull {
                    val parts=it.split("=",limit=2)
                    if(parts.size==2 && parts[0].isNotBlank()) parts[0].trim() to parts[1].trim() else null
                }.toMap()
                val value=if(!symbols && (shifted || caps)) key.uppercase() else key
                ic.commitText(value,1)
                if (shifted && !caps && !symbols) { shifted = false; refresh() }
                updateSuggestions()
            }
        }
    }
}