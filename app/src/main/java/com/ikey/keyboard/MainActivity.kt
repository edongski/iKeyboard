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
        layout.addView(label("Phase 3 • Swipe-to-Type Keyboard", 16f))
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
        layout.addView(label("Swipe across letters to enter common English and Filipino words. The starter dictionary is limited and runs offline.", 13f))
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
        layout.addView(label("Phase 3: offline starter swipe recognition with visible gesture trail and swipe toggle. Phase 2 adaptive touch controls remain available. Advanced dictionary and contextual predictions are future work.", 13f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}