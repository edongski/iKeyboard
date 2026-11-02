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
        layout.addView(label("Phase 1 • Native Android keyboard", 16f))
        layout.addView(label("1. Enable iKey in Android keyboard settings.\n2. Select iKey as your keyboard.\n3. Test typing below.", 15f))
        layout.addView(button("1  ENABLE KEYBOARD") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        layout.addView(button("2  SELECT KEYBOARD") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        val dark = Switch(this).apply {
            text="Dark keyboard theme"; textSize=16f
            isChecked=prefs.getBoolean("dark",false)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("dark",checked).apply() }
        }
        layout.addView(dark)
        layout.addView(label("Try your keyboard:", 17f))
        layout.addView(EditText(this).apply { hint="Tap here and start typing"; minLines=3; gravity=Gravity.TOP; inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE })
        layout.addView(label("Phase 1: QWERTY, Shift/Caps, symbols, numbers, backspace, enter, light/dark. Swipe typing and prediction are planned for later phases.", 13f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}