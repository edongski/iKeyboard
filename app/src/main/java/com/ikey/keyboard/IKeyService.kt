package com.ikey.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.graphics.Color
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
    private var lastSwipeWord:String? = null
    private val prefs by lazy { getSharedPreferences("ikey", MODE_PRIVATE) }
    private val settingsListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
        if(key=="settings_apply_version") {
            keyboard?.showLetters()
            refresh()
            keyboard?.requestLayout()
            keyboard?.invalidate()
            updateSuggestions()
        }
    }
    private fun learned():Set<String> = prefs.getStringSet("learned_words",emptySet()) ?: emptySet()
    private fun rememberWord(word:String) {
        if(!prefs.getBoolean("learn_words",false) || !suggestionsAllowed) return
        val normalized=word.lowercase().trim()
        if(normalized.length !in 3..24 || !normalized.all { it.isLetter() }) return
        val updated=learned().toMutableSet()
        if(updated.size>=500 && normalized !in updated) updated.remove(updated.first())
        updated.add(normalized)
        prefs.edit().putStringSet("learned_words",updated).apply()
    }
    private fun clipboardHistory():List<String> =
        prefs.getString("clipboard_history","").orEmpty().split(1.toChar()).filter { it.isNotEmpty() }.take(9)
    private fun saveClipboard(value:String) {
        if(value.isBlank() || !prefs.getBoolean("clipboard_history_enabled",false)) return
        val updated=(listOf(value.take(1000))+clipboardHistory().filter { it!=value }).take(9)
        prefs.edit().putString("clipboard_history",updated.joinToString(1.toChar().toString())).apply()
    }
    private fun updateAutoCapitalization() {
        if(!prefs.getBoolean("auto_caps",true) || numeric || symbols) return
        val before=currentInputConnection?.getTextBeforeCursor(100,0)?.toString().orEmpty()
        val trimmed=before.trimEnd()
        val sentenceStart=before.isBlank() || (before.lastOrNull()?.isWhitespace()==true &&
            trimmed.lastOrNull() in listOf('.','!','?',10.toChar()))
        if(sentenceStart) {
            if(!shifted && !caps) { shifted=true; refresh() }
        }
    }
    private fun contextWords():Pair<String,String> {
        val text=currentInputConnection?.getTextBeforeCursor(90,0)?.toString().orEmpty()
        val match=Regex("[\\p{L}']+$").find(text)
        val prefix=match?.value.orEmpty()
        val before=text.dropLast(prefix.length).trimEnd()
        val previous=Regex("[\\p{L}']+").findAll(before).lastOrNull()?.value.orEmpty()
        return prefix to previous
    }
    private fun updateSuggestions() {
        if(!suggestionsAllowed || numeric || symbols || !prefs.getBoolean("predictions",false)) {
            keyboard?.setSuggestions(emptyList()); return
        }
        val (prefix,previous)=contextWords()
        try { keyboard?.setSuggestions(WordSuggestions.candidates(prefix,previous,learned())) }
        catch(e:Exception) {
            android.util.Log.e("iKeyboard","Suggestion engine failed",e)
            keyboard?.setSuggestions(emptyList())
        }
    }
    private fun commitSeparator(separator:String) {
        val ic=currentInputConnection ?: return
        val (prefix,_)=contextWords()
        if(prefix.isNotBlank() && suggestionsAllowed) {
            val shortcut=prefs.getString("shortcuts","").orEmpty().lineSequence().mapNotNull {
                val parts=it.split("=",limit=2)
                if(parts.size==2 && parts[0].trim().isNotBlank()) parts[0].trim() to parts[1].trim() else null
            }.firstOrNull { it.first==prefix }?.second
            if(shortcut!=null) {
                ic.deleteSurroundingText(prefix.length,0)
                ic.commitText(shortcut+separator,1)
                updateAutoCapitalization()
                return
            }
        }
        if(prefix.isNotBlank() && prefs.getBoolean("autocorrect",true) && suggestionsAllowed) {
            val replacement=WordSuggestions.correction(prefix,learned())
            if(replacement!=null && replacement!=prefix.lowercase()) {
                ic.deleteSurroundingText(prefix.length,0)
                ic.commitText(if(prefix.first().isUpperCase()) replacement.replaceFirstChar { it.uppercase() } else replacement,1)
                rememberWord(replacement)
            }
        }
        if(prefix.isNotBlank() && (!prefs.getBoolean("autocorrect",true) || WordSuggestions.correction(prefix,learned())==null)) rememberWord(prefix)
        ic.commitText(separator,1)
        updateAutoCapitalization()
    }
    override fun onCreate() {
        super.onCreate()
        prefs.registerOnSharedPreferenceChangeListener(settingsListener)
        try { WordSuggestions.initialize(this) } catch(e:Exception) {
            android.util.Log.e("iKeyboard","Dictionary initialization failed; using built-in words",e)
        }
        // Keep the IME above the system navigation bar instead of drawing behind it.
        window?.window?.let { w ->
            w.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            w.navigationBarColor=Color.rgb(208,211,219)
            @Suppress("DEPRECATION")
            val layoutFlags=View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility=w.decorView.systemUiVisibility and layoutFlags.inv()
        }
    }
    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
        super.onDestroy()
    }
    override fun onCreateInputView(): View {
        keyboard = IKeyView(this) { key ->
            try { handle(key) } catch(e:Exception) {
                android.util.Log.e("iKeyboard","Keyboard key action failed: "+key,e)
                try { keyboard?.showLetters(); keyboard?.setSuggestions(emptyList()) } catch(_:Exception) {}
            }
        }
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
        updateAutoCapitalization()
    }
    private fun refresh() { keyboard?.update(symbols, shifted || caps, numeric, prefs.getBoolean("dark",false)); updateSuggestions() }
    private fun handle(key: String) {
        val ic = currentInputConnection ?: return
        if(key.startsWith("CHAR:")) {
            val value=key.removePrefix("CHAR:")
            if(value.isNotEmpty()) ic.commitText(if(shifted || caps) value.uppercase() else value,1)
            if(shifted && !caps) { shifted=false; refresh() }
            updateSuggestions()
            return
        }
        if(key=="DELETE_WORD") {
            val text=ic.getTextBeforeCursor(120,0)?.toString().orEmpty()
            val count=Regex("\\s*[^\\s]+\\s*$").find(text)?.value?.length ?: 0
            if(count>0) ic.deleteSurroundingTextInCodePoints(count,0)
            updateSuggestions()
            return
        }
        if(key.startsWith("SUGGEST:")) {
            val swipe=lastSwipeWord
            if(swipe!=null) {
                val before=ic.getTextBeforeCursor(swipe.length+1,0)?.toString().orEmpty()
                if(before.endsWith(swipe+" ")) {
                    ic.deleteSurroundingText(swipe.length+1,0)
                    ic.commitText(key.removePrefix("SUGGEST:")+" ",1)
                    lastSwipeWord=null; updateSuggestions(); return
                }
            }
            val suggestion=key.removePrefix("SUGGEST:")
            val (prefix,_)=contextWords()
            if(suggestionsAllowed && suggestion.isNotBlank()) {
                if(prefix.isNotEmpty()) ic.deleteSurroundingText(prefix.length,0)
                ic.commitText(suggestion+" ",1)
                updateSuggestions()
            }
            return
        }
        if(key=="DOUBLE_SPACE_PERIOD") {
            val previous=ic.getTextBeforeCursor(1,0)?.toString().orEmpty()
            if(previous==" ") { ic.deleteSurroundingText(1,0); ic.commitText(". ",1) }
            else ic.commitText(" ",1)
            updateAutoCapitalization(); updateSuggestions(); return
        }
        if(key=="◀" || key=="▶") { keyboard?.nextEmojiPage(if(key=="▶") 1 else -1); return }
        if(key.startsWith("CLIP:")) {
            if(!suggestionsAllowed) return
            val index=key.substringAfter(":").toIntOrNull() ?: return
            val value=clipboardHistory().getOrNull(index) ?: return
            ic.commitText(value,1); keyboard?.showLetters(); updateSuggestions(); return
        }
        if(key=="SAVE") {
            if(!suggestionsAllowed) return
            val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip=clipboard.primaryClip
            val value=if(clip!=null && clip.itemCount>0) clip.getItemAt(0).coerceToText(this).toString() else ""
            saveClipboard(value); keyboard?.setClipboardItems(clipboardHistory()); return
        }
        if(key=="DELETE CLIP") {
            if(!suggestionsAllowed) return
            val updated=clipboardHistory().drop(1)
            prefs.edit().putString("clipboard_history",updated.joinToString(1.toChar().toString())).apply()
            keyboard?.setClipboardItems(updated); return
        }
        if(key=="CLEAR ALL") {
            if(!suggestionsAllowed) return
            prefs.edit().remove("clipboard_history").apply()
            keyboard?.setClipboardItems(emptyList()); return
        }
        if(key.startsWith("SWIPE_WORD:")) {
            val word=key.removePrefix("SWIPE_WORD:")
            if(word.isNotBlank()) {
                val value=if(shifted || caps) word.replaceFirstChar { it.uppercase() } else word
                ic.commitText(value+" ",1)
                lastSwipeWord=value
                rememberWord(value)
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
        if(key!="😊" && key!="📋") lastSwipeWord=null
        when(key) {
            "📖" -> { keyboard?.showDictionary() }
            "PERSONAL DICTIONARY" -> { keyboard?.showLetters(); startActivity(android.content.Intent(this,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            "ADD WORD" -> { keyboard?.showLetters(); startActivity(android.content.Intent(this,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            "CLEAR LEARNED" -> { prefs.edit().remove("learned_words").apply(); keyboard?.showLetters(); updateSuggestions() }
            "😊" -> { if(keyboard?.isEmojiMode()==true) { ic.commitText("😊",1); updateSuggestions() } else keyboard?.showEmoji() }
            "📋" -> {
                if(!suggestionsAllowed) return
                keyboard?.setClipboardItems(clipboardHistory())
            }
            "PASTE" -> {
                if(!suggestionsAllowed) return
                val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip=clipboard.primaryClip
                val item=if(clip!=null && clip.itemCount>0) clip.getItemAt(0) else null
                val value=item?.coerceToText(this)?.toString()
                if(!value.isNullOrEmpty()) {
                    ic.commitText(value.take(10000),1)
                    saveClipboard(value)
                }
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
                val value=if(!symbols && (shifted || caps) && keyboard?.isEmojiMode()!=true) key.uppercase() else key
                ic.commitText(value,1)
                if (shifted && !caps && !symbols) { shifted = false; refresh() }
                updateSuggestions()
            }
        }
    }
}