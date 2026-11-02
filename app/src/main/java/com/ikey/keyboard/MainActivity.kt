package com.ikey.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("ikey", MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 24)
            setBackgroundColor(Color.rgb(247, 247, 250))
        }
        fun label(s: String, size: Float) = TextView(this).apply { text=s; textSize=size; setTextColor(Color.rgb(30,30,35)); setPadding(0,12,0,12) }
        fun button(s: String, action: () -> Unit) = Button(this).apply { text=s; setOnClickListener { action() } }
        layout.addView(label("iKey Keyboard", 30f))
        layout.addView(button("✓  APPLY KEYBOARD CHANGES") {
            prefs.edit().putLong("settings_apply_version",System.currentTimeMillis()).apply()
            Toast.makeText(this,"Keyboard settings applied",Toast.LENGTH_SHORT).show()
        })
        layout.addView(label("Tap Apply after changing settings. If the keyboard is open, it will refresh immediately.",13f))
        layout.addView(label("Enhanced Keyboard • Phase 6+", 16f))
        layout.addView(label("1. Enable iKey in Android keyboard settings.\n2. Select iKey as your keyboard.\n3. Test typing below.", 15f))
        layout.addView(button("1  ENABLE KEYBOARD") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        layout.addView(button("2  SELECT KEYBOARD") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        val dark = Switch(this).apply {
            text="Dark keyboard theme"; textSize=16f
            isChecked=prefs.getBoolean("dark",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("dark",checked).apply() }
        }
        layout.addView(dark)

        layout.addView(Switch(this).apply {
            text="Enable swipe-to-type"; textSize=16f
            isChecked=prefs.getBoolean("swipe_enabled",true)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("swipe_enabled",checked).apply() }
        })
        layout.addView(label("Swipe across letters for offline English and Filipino word recognition. Suggestions update as you type.", 13f))
        layout.addView(Switch(this).apply {
            text="Show numbers above QWERTY letters"; textSize=16f
            isChecked=prefs.getBoolean("number_hints",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("number_hints",checked).apply() }
        })
        layout.addView(Switch(this).apply {
            text="Add a separate number row above letters"; textSize=16f
            isChecked=prefs.getBoolean("number_row",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("number_row",checked).apply() }
        })
        layout.addView(label("When enabled, Q–P show 1–0 as small labels. Hold those letters to enter the number. Reopen the keyboard to apply.",13f))
        layout.addView(label("Keyboard accent color",15f))
        val accentOptions=arrayOf("Blue","Purple","Green","Orange","Pink")
        val accentSpinner=Spinner(this)
        accentSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,accentOptions)
        accentSpinner.setSelection(accentOptions.indexOf(prefs.getString("accent","Blue")).coerceAtLeast(0))
        accentSpinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long) {
                prefs.edit().putString("accent",accentOptions[position]).apply()
            }
        }
        layout.addView(accentSpinner)
        layout.addView(label("Predictions are OFF by default. Turn them on below if wanted.",13f))
        layout.addView(label("Phase 4: Suggestions and autocorrect",18f))
        layout.addView(Switch(this).apply {
            text="Show word suggestions"; textSize=16f
            isChecked=prefs.getBoolean("predictions",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("predictions",checked).apply() }
        })
        layout.addView(Switch(this).apply {
            text="Autocorrect on space"; textSize=16f
            isChecked=prefs.getBoolean("autocorrect",true)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("autocorrect",checked).apply() }
        })
        layout.addView(label("Suggestions are processed offline using bundled open-license English and Filipino wordlists. Predictions are disabled in password fields.",13f))
        layout.addView(label("Phase 5: Keyboard layout and shortcuts",18f))
        val handOptions=arrayOf("Off","Left","Right")
        val handSpinner=Spinner(this)
        handSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,handOptions)
        handSpinner.setSelection(handOptions.indexOf(prefs.getString("one_hand","Off")).coerceAtLeast(0))
        handSpinner.onItemSelectedListener=object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long) {
                prefs.edit().putString("one_hand",handOptions[position]).apply()
            }
        }
        layout.addView(label("One-handed keyboard: Off / Left / Right",15f))
        layout.addView(handSpinner)
        layout.addView(label("Text shortcuts (one per line: shortcut=expanded text)",15f))
        val shortcuts=EditText(this).apply {
            hint="brb=be right back"
            setSingleLine(false); minLines=3; maxLines=6
            setText(prefs.getString("shortcuts",""))
        }
        layout.addView(shortcuts)
        layout.addView(button("SAVE TEXT SHORTCUTS") {
            prefs.edit().putString("shortcuts",shortcuts.text.toString().take(4000)).apply()
            Toast.makeText(this,"Shortcuts saved",Toast.LENGTH_SHORT).show()
        })
        layout.addView(label("Emoji: tap 😊. Clipboard: tap 📋 then PASTE. Clipboard history is stored only if explicitly enabled and saved.",13f))
        layout.addView(label("Phase 6: Comfort and accessibility",18f))
        layout.addView(Switch(this).apply {
            text="Vibration on key press"; textSize=16f
            isChecked=prefs.getBoolean("haptics",true)
            setOnCheckedChangeListener { _,enabled -> prefs.edit().putBoolean("haptics",enabled).apply() }
        })
        layout.addView(Switch(this).apply {
            text="Show swipe trail"; textSize=16f
            isChecked=prefs.getBoolean("swipe_trail",true)
            setOnCheckedChangeListener { _,enabled -> prefs.edit().putBoolean("swipe_trail",enabled).apply() }
        })
        layout.addView(label("Key height (38–90 dp) — or drag the keyboard top handle",15f))
        val keyHeight=SeekBar(this).apply {
            max=52
            progress=prefs.getInt("key_height",53).coerceIn(38,90)-38
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar:SeekBar?,progress:Int,fromUser:Boolean) {
                    if(fromUser) prefs.edit().putInt("key_height",progress+38).apply()
                }
                override fun onStartTrackingTouch(bar:SeekBar?) {}
                override fun onStopTrackingTouch(bar:SeekBar?) {}
            })
        }
        layout.addView(keyHeight)
        layout.addView(label("Long-press a/e/i/o/u/n/c for accents. Long-press backspace to delete a word. Drag the small handle above suggestions up or down to resize the keyboard immediately.",13f))
        layout.addView(label("Expanded keyboard features",18f))
        fun toggleSetting(title:String,key:String,default:Boolean) {
            layout.addView(Switch(this).apply {
                text=title; textSize=16f
                isChecked=prefs.getBoolean(key,default)
                setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean(key,checked).apply() }
            })
        }
        toggleSetting("Auto-capitalize sentences","auto_caps",true)
        toggleSetting("Double-space inserts a period","double_space_period",true)
        toggleSetting("Learn words locally (opt-in)","learn_words",false)
        toggleSetting("Save clipboard history locally (opt-in)","clipboard_history_enabled",false)
        layout.addView(label("Personal dictionary (one word per line)",15f))
        val dictionaryInput=EditText(this).apply {
            minLines=2; maxLines=5
            hint="Add words that should not be autocorrected"
            setText(prefs.getStringSet("learned_words",emptySet())?.sorted()?.joinToString("\n") ?: "")
        }
        layout.addView(dictionaryInput)
        layout.addView(button("SAVE PERSONAL DICTIONARY") {
            val words=dictionaryInput.text.toString().lineSequence()
                .map { it.trim().lowercase() }
                .filter { it.length in 2..24 && it.all(Char::isLetter) }
                .distinct().take(500).toSet()
            prefs.edit().putStringSet("learned_words",words).apply()
            Toast.makeText(this,"${words.size} words saved",Toast.LENGTH_SHORT).show()
        })
        layout.addView(button("CLEAR LEARNED WORDS") {
            prefs.edit().remove("learned_words").apply()
            Toast.makeText(this,"Learned words cleared",Toast.LENGTH_SHORT).show()
        })
        layout.addView(button("CLEAR CLIPBOARD HISTORY") {
            prefs.edit().remove("clipboard_history").apply()
            Toast.makeText(this,"Clipboard history cleared",Toast.LENGTH_SHORT).show()
        })
        layout.addView(label("Clipboard history saves up to 9 items only when you tap SAVE in the clipboard panel. Saved clips stay on this device until deleted. Never save passwords or sensitive text.",13f))
        layout.addView(label("Touch detection mode", 18f))
        val modes = arrayOf("Classic","Smart","Adaptive")
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        spinner.setSelection(modes.indexOf(prefs.getString("touch_mode","Adaptive")).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putString("touch_mode",modes[position]).apply()
            }
        }
        layout.addView(spinner)
        fun slider(title: String, key: String, min: Int, max: Int, initial: Int) {
            val text = label(title, 15f)
            layout.addView(text)
            val seek = SeekBar(this)
            seek.max = max-min
            seek.progress = (prefs.getInt(key,initial)-min).coerceIn(0,max-min)
            text.text = title + ": " + (seek.progress+min)
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    text.text = title + ": " + (progress+min)
                    if(fromUser) prefs.edit().putInt(key,progress+min).apply()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
            layout.addView(seek)
        }
        slider("Touch sensitivity", "sensitivity", 0, 100, 50)
        slider("Horizontal correction (dp)", "offset_x", -30, 30, 0)
        slider("Vertical correction (dp)", "offset_y", -30, 30, 0)
        layout.addView(button("RESET TOUCH SETTINGS") {
            prefs.edit().putInt("sensitivity",50).putInt("offset_x",0).putInt("offset_y",0).putString("touch_mode","Adaptive").apply()
            recreate()
        })
        layout.addView(label("Touch settings apply when the keyboard next opens. Drag sideways on the spacebar to move the cursor.", 13f))
        layout.addView(label("Try your keyboard:", 17f))
        layout.addView(EditText(this).apply { hint="Tap here and start typing"; minLines=3; gravity=Gravity.TOP; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE })
        layout.addView(label("Expanded: larger offline dictionary, emoji pages, opt-in local vocabulary learning, opt-in clipboard history, auto-capitalization, and double-space punctuation. Swipe recognition and prediction remain approximate and require device testing.", 13f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}