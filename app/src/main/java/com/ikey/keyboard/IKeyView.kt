package com.ikey.keyboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.HapticFeedbackConstants
import kotlin.math.*

class IKeyView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private data class Key(val text: String, val rect: RectF)
    private val keys = mutableListOf<Key>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var symbols = false
    private var symbolPage=0
    private var shift = false
    private var numeric = false
    private var dark = false
    private fun accent():Int = when(prefs.getString("accent","Blue")) {
        "Purple" -> Color.rgb(132,90,205)
        "Green" -> Color.rgb(38,148,105)
        "Orange" -> Color.rgb(220,120,48)
        "Pink" -> Color.rgb(211,91,142)
        else -> Color.rgb(44,112,225)
    }
    private var emojiMode = false
    private var clipboardMode = false
    private var dictionaryMode = false
    private var clipboardPreview = ""
    private var clipboardItems:List<String> = emptyList()
    private var emojiPage=0
    private var downKey:Key?=null
    private var lastTapSpace=0L
    fun setClipboardItems(items:List<String>) {
        clipboardItems=items.take(12)
        clipboardPreview=if(items.isEmpty()) "No saved clips" else "${items.size} saved clips"
        clipboardMode=true; emojiMode=false; rebuild(width); invalidate()
    }
    fun setClipboardPreview(value:String) { clipboardPreview=value.take(80); clipboardMode=true; emojiMode=false; rebuild(width); invalidate() }
    private val emojis = listOf("😀","😂","🥰","😍","😊","😎","😭","🥺","😡","👍","👏","🙏","❤️","💕","🔥","🎉","💯","✨","👋","🤝","😅","🤔","😘","😁","😴","😇","🙌","💪","🇵🇭","🍔","☕","🎂")

    private val extraEmojis = listOf("😃","😄","😆","😋","😛","😜","🤪","🤗","🤩","🥳","😌","😔","😢","😤","😠","🤬","😱","😨","😰","😳","🤯","🤒","🤕","🤢","🤮","😷","🤠","🥸","😈","👻","💀","👽","🤖","😺","😻","🙈","🙉","🙊","💋","💌","💘","💖","💝","💞","💓","💗","💟","💔","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💐","🌹","🌷","🌻","🌺","🌸","🌼","🌈","☀️","🌙","⭐","🌟","⚡","☁️","🌧️","❄️","🌊","🌴","🌲","🌿","🍀","🍎","🍌","🍉","🍇","🍓","🍒","🍍","🥭","🥑","🍕","🍟","🍗","🍜","🍣","🍩","🍰","🍦","🥤","🍺","🏀","⚽","🏆","🎮","🎧","🎵","🎶","🎤","🎬","📱","💻","⌚","🚗","✈️","🚀","🏠","🏢","🎁","💰","💵","📌","📍","✅","❌","⚠️","❗","❓","💬","💭","🫶","🤌","🤞","✌️","🤟","👌","👊","✊","🫰","🖐️","👀","🧠","💃","🕺","🏃","🚶","👨","👩","👶","👨‍👩‍👧","🇺🇸","🇬🇧","🇯🇵","🇰🇷")
    private val allEmojis by lazy { (emojis+extraEmojis).distinct() }
    private var suggestions: List<String> = emptyList()
    private val resizeHandleHeight = 16f * resources.displayMetrics.density
    private val barHeight get() = if(showPredictionBar()) 55f*density else resizeHandleHeight
    private val accessoryHeight = 45f * resources.displayMetrics.density
    private fun numberRowEnabled()=prefs.getBoolean("number_row",false)
    private fun showPredictionBar()=prefs.getBoolean("predictions",false)
    private var resizing = false
    private var accessoryHandled = false
    private var resizeStartY = 0f
    private var resizeStartHeight = 53
    fun setSuggestions(items:List<String>) { suggestions=items.take(3); invalidate() }
    private var pressed: Key? = null
    private val prefs = context.getSharedPreferences("ikey", Context.MODE_PRIVATE)
    private var downX = 0f
    private var downY = 0f
    private var spaceDrag = false
    private var cursorSteps = 0
    private var cursorVerticalSteps = 0
    private var backspaceHeld=false
    private var backspaceStart=0L
    private val repeatBackspace=object:Runnable { override fun run() {
        if(!backspaceHeld) return
        val elapsed=android.os.SystemClock.uptimeMillis()-backspaceStart
        onKey(if(elapsed>=2000L) "DELETE_WORD" else "⌫")
        longPressHandler.postDelayed(this,if(elapsed>=2000L) 135L else 65L)
    } }
    private fun stopBackspace() { backspaceHeld=false; longPressHandler.removeCallbacks(repeatBackspace) }
    private val swipePoints = mutableListOf<PointF>()
    private val swipeLetters = StringBuilder()
    private var swipeActive = false
    private var previousSwipeX=0f
    private var previousSwipeY=0f
    private fun swipeEnabled() = prefs.getBoolean("swipe_enabled",true)
    private fun addSwipePoint(x:Float,y:Float) {
        if(swipePoints.size<600) swipePoints.add(PointF(x,y))
        val hit=keys.firstOrNull { it.text.length==1 && it.text[0] in 'a'..'z' && it.rect.contains(x,y) }?.text
            ?: keys.filter { it.text.length==1 && it.text[0] in 'a'..'z' }
                .minByOrNull { hypot(it.rect.centerX()-x,it.rect.centerY()-y) }
                ?.takeIf { hypot(it.rect.centerX()-x,it.rect.centerY()-y)<rowHeight*0.78f }?.text
        if(hit!=null && hit.length==1 && hit[0] in 'a'..'z' &&
            (swipeLetters.isEmpty() || swipeLetters.last().toString()!=hit)) swipeLetters.append(hit)
    }
    private fun smartHit(x: Float, y: Float): Key? {
        if (keys.isEmpty()) return null
        val px = x - prefs.getInt("offset_x",0).coerceIn(-30,30)*density
        val py = y - prefs.getInt("offset_y",0).coerceIn(-30,30)*density
        val mode = prefs.getString("touch_mode","Adaptive") ?: "Adaptive"
        val sensitivity = prefs.getInt("sensitivity",50).coerceIn(0,100)/100f
        val expansion = if(mode=="Classic") 0f else (3f+13f*sensitivity)*density
        return keys.filter {
            px>=it.rect.left-expansion && px<=it.rect.right+expansion &&
            py>=it.rect.top-expansion && py<=it.rect.bottom+expansion
        }.minByOrNull {
            val nx=(px-it.rect.centerX())/max(it.rect.width()/2,1f)
            val ny=(py-it.rect.centerY())/max(it.rect.height()/2,1f)
            nx*nx+ny*ny*(if(mode=="Adaptive") 0.75f else 1f)
        }
    }

    private val density = resources.displayMetrics.density
    private val rowHeight get() = prefs.getInt("key_height",53).coerceIn(38,90)*density
    private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var longPressTriggered = false
    private val longPressTask = Runnable {
        val key=pressed?.text
        if(key=="⌫") {
            // Backspace repeat is handled separately: letters first, words after two seconds.
        } else if(showNumberHints() && key!=null && key.length==1 && "qwertyuiop".contains(key)) {
            val index="qwertyuiop".indexOf(key)
            longPressTriggered=true
            onKey("CHAR:"+(if(index==9) "0" else (index+1).toString()))
            invalidate()
        } else if(key in listOf("a","e","i","o","u","n","c")) {
            val accents=mapOf("a" to "á","e" to "é","i" to "í","o" to "ó","u" to "ú","n" to "ñ","c" to "ç")
            val selected=accents[key]
            if(selected!=null) { longPressTriggered=true; onKey("CHAR:"+selected); invalidate() }
        }
    }
    private fun cancelPendingLongPress() { longPressHandler.removeCallbacks(longPressTask) }
    private val margin = 3f * density
    private fun showNumberHints() = prefs.getBoolean("number_hints",false)
    init { isClickable = true; setLayerType(LAYER_TYPE_SOFTWARE,null) }
    fun update(s: Boolean, sh: Boolean, n: Boolean, d: Boolean) { symbols=s; shift=sh; numeric=n; dark=d; rebuild(width); invalidate() }
    fun nextSymbolPage() { symbolPage=(symbolPage+1)%2; rebuild(width); invalidate() }
    fun showEmoji() { emojiMode=!emojiMode; clipboardMode=false; rebuild(width); invalidate() }
    fun showLetters() { emojiMode=false; clipboardMode=false; dictionaryMode=false; rebuild(width); invalidate() }
    fun showDictionary() { dictionaryMode=!dictionaryMode; emojiMode=false; clipboardMode=false; rebuild(width); invalidate() }
    fun nextEmojiPage(delta:Int) { emojiPage=(emojiPage+delta+(allEmojis.size+29)/30)%((allEmojis.size+29)/30); rebuild(width); invalidate() }
    fun currentClipboardItem(index:Int)=clipboardItems.getOrNull(index)
    fun showSwipeAlternatives(items:List<String>) { suggestions=items.take(3); invalidate() }
    fun isEmojiMode() = emojiMode
    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        val rows=4+(if(numberRowEnabled() && !numeric && !symbols && !emojiMode && !clipboardMode && !dictionaryMode) 1 else 0)
        setMeasuredDimension(width, (rows*rowHeight+barHeight+accessoryHeight).toInt() + (12*density).toInt())
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh); rebuild(w) }
    private fun rebuild(w:Int) {
        if(w<=0)return
        keys.clear()
        val rows = if(dictionaryMode) listOf(
            listOf("PERSONAL DICTIONARY"),listOf("ADD WORD"),listOf("CLEAR LEARNED"),listOf("ABC")
        ) else if(clipboardMode) listOf(
            (0..2).map { "CLIP:$it" },
            (3..5).map { "CLIP:$it" },
            (6..8).map { "CLIP:$it" },
            listOf("ABC","PASTE","SAVE","DELETE CLIP","CLEAR ALL")
        ) else if(emojiMode) {
            val pageCount=(allEmojis.size+29)/30
            val start=(emojiPage%pageCount)*30
            val page=allEmojis.drop(start).take(30)
            listOf(page.take(10),page.drop(10).take(10),page.drop(20).take(10),
                listOf("ABC","◀","▶","⌫","space","↵"))
        } else if(numeric) listOf(listOf("+","1","2","3","%"),listOf("=","4","5","6","-"),listOf(",","7","8","9","⌫"),listOf("ABC","#+=","0",".","↵"))
        else if(symbols && symbolPage==1) listOf(
            listOf("~","\u0060","|","•","√","∏","÷","×","¶","∆"),
            listOf("£","€","¥","$","^","°","=","{","}","\\"),
            listOf("123","%","©","®","™","‰","[","]","⌫"),
            listOf("ABC","<",">","space",".","↵")
        ) else if(symbols) listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","₱","_","&","-","+","(",")","/"),
            listOf("#+=","*","\"","'",":",";","!","?","⌫"),
            listOf("ABC",",","space",".","↵")
        )
        else listOf("qwertyuiop".map{it.toString()},"asdfghjkl".map{it.toString()},listOf("⇧")+"zxcvbnm".map{it.toString()}+"⌫",listOf("?123",",","space",".","↵"))
        val finalRows=if(numberRowEnabled() && !numeric && !symbols && !emojiMode && !clipboardMode && !dictionaryMode) listOf(listOf("1","2","3","4","5","6","7","8","9","0"))+rows else rows
        finalRows.forEachIndexed { row, items ->
            val weights=items.map { when(it) { "space"->4.0f; "⇧","⌫","?123","ABC","↵","PASTE","CLEAR"->1.45f; "🌐","😊","📋"->1.1f; else->1f } }
            val total=weights.sum().coerceAtLeast(1f)
            val hand=prefs.getString("one_hand","Off") ?: "Off"
            val scale=if(hand=="Off") 1f else 0.78f
            val usable=(w-2*margin)*scale
            val origin=if(hand=="Right") w-margin-usable else margin
            val unit=usable/total
            var x=origin
            if(row==(if(numberRowEnabled()) 2 else 1) && !symbols && !numeric && !emojiMode && !clipboardMode && !dictionaryMode) {
                val natural=usable/10f*9f
                x+=(usable-natural)/2
            }
            items.forEachIndexed { i, t ->
                val width= if(row==(if(numberRowEnabled()) 2 else 1) && !symbols && !numeric && !emojiMode && !clipboardMode && !dictionaryMode) usable/10f else weights[i]*unit
                val y=barHeight+row*rowHeight+margin
                keys.add(Key(t,RectF(x+margin,y,x+width-margin,y+rowHeight-2*margin)))
                x+=width
            }
        }
    }
    override fun onDraw(c:Canvas) {
        c.drawColor(if(dark) Color.rgb(42,44,49) else Color.rgb(220,224,230))
        paint.color=if(dark) Color.rgb(48,50,55) else Color.rgb(224,227,234)
        c.drawRect(0f,0f,width.toFloat(),barHeight,paint)
        paint.color=Color.rgb(230,230,230) // 90% gray (#E6E6E6)
        c.drawRoundRect(RectF(width/2f-23f*density,5f*density,width/2f+23f*density,10f*density),4f*density,4f*density,paint)
        c.save()
        c.translate(0f,resizeHandleHeight)
        if(clipboardMode) {
            paint.color=if(dark) Color.WHITE else Color.BLACK
            paint.textSize=13f*density
            paint.textAlign=Paint.Align.CENTER
            c.drawText("Clipboard: "+clipboardPreview.take(32),width/2f,(barHeight-resizeHandleHeight)/2f-(paint.ascent()+paint.descent())/2f,paint)
        } else if(showPredictionBar() && suggestions.isNotEmpty()) {
            suggestions.forEachIndexed { index, word ->
                val left=index*width/3f
                paint.color=if(dark) Color.WHITE else Color.rgb(26,30,40)
                paint.textAlign=Paint.Align.CENTER
                paint.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
                paint.textSize=15f*density
                c.drawText(word,left+width/6f,(barHeight-resizeHandleHeight)/2f-(paint.ascent()+paint.descent())/2f,paint)
                if(index<2) {
                    paint.color=if(dark) Color.DKGRAY else Color.LTGRAY
                    c.drawRect((index+1)*width/3f-0.5f*density,7f*density,(index+1)*width/3f+0.5f*density,barHeight-7f*density,paint)
                }
            }
        }
        c.restore()
        // iOS-inspired lower accessory row: emoji, clipboard, dictionary and globe.
        val accessoryY=height-accessoryHeight+4f*density
        val accessories=listOf("😊","📋","📖","🌐")
        paint.color=if(dark) Color.WHITE else Color.rgb(35,38,44)
        paint.textAlign=Paint.Align.CENTER
        paint.typeface=Typeface.create(prefs.getString("keyboard_font","sans-serif") ?: "sans-serif",prefs.getInt("keyboard_font_style",0))
        paint.textSize=23f*density
        accessories.forEachIndexed { i, symbol ->
            val center=width*(i+0.5f)/4f
            c.drawText(symbol,center,accessoryY+24f*density,paint)
        }
        keys.forEach { key ->
            val special=key.text in listOf("⇧","⌫","?123","ABC","↵","🌐","◀","▶","PASTE","SAVE","DELETE CLIP","CLEAR ALL")
            val color=when {
                pressed===key -> accent()
                dark && special -> Color.rgb(85,88,95)
                dark -> Color.rgb(66,69,76)
                special -> Color.rgb(174,180,191)
                else -> Color.WHITE
            }
            paint.color=color
            paint.setShadowLayer(1.5f*density,0f,1.5f*density,0x50000000)
            c.drawRoundRect(key.rect,5*density,5*density,paint)
            paint.clearShadowLayer()
            paint.color=if(pressed===key) Color.WHITE else if(dark) Color.WHITE else Color.rgb(23,25,31)
            paint.textAlign=Paint.Align.CENTER
            paint.typeface=Typeface.create(prefs.getString("keyboard_font","sans-serif") ?: "sans-serif",prefs.getInt("keyboard_font_style",0))
            paint.textSize=(if(key.text=="space") 13 else if(key.text.startsWith("CLIP:")) 12 else if(key.text.length>2) 12 else if(key.text.length>1) 19 else 25)*density
            val text=if(clipboardMode && key.text.startsWith("CLIP:")) {
                val index=key.text.substringAfter(":").toIntOrNull() ?: -1
                clipboardItems.getOrNull(index)?.replace("\n"," ")?.take(12) ?: "—"
            } else if(shift && !symbols && !emojiMode && !clipboardMode) key.text.uppercase() else key.text
            val cy=key.rect.centerY()-(paint.ascent()+paint.descent())/2
            val hint=if(showNumberHints() && !symbols && !numeric && !emojiMode && !clipboardMode)
                "qwertyuiop".indexOf(key.text).takeIf { it>=0 }?.let { if(it==9) "0" else (it+1).toString() }
                else null
            if(key.text in listOf("⇧","⌫","↵")) {
                val cx=key.rect.centerX()
                val y=key.rect.centerY()
                paint.style=Paint.Style.STROKE
                paint.strokeWidth=2.2f*density
                paint.strokeCap=Paint.Cap.ROUND
                paint.strokeJoin=Paint.Join.ROUND
                val path=Path()
                when(key.text) {
                    "⇧" -> {
                        path.moveTo(cx-12*density,y)
                        path.lineTo(cx,y-12*density)
                        path.lineTo(cx+12*density,y)
                        path.lineTo(cx+6*density,y)
                        path.lineTo(cx+6*density,y+11*density)
                        path.lineTo(cx-6*density,y+11*density)
                        path.lineTo(cx-6*density,y)
                        path.close()
                    }
                    "⌫" -> {
                        path.moveTo(cx-13*density,y)
                        path.lineTo(cx-5*density,y-10*density)
                        path.lineTo(cx+13*density,y-10*density)
                        path.lineTo(cx+13*density,y+10*density)
                        path.lineTo(cx-5*density,y+10*density)
                        path.close()
                        path.moveTo(cx-1*density,y-5*density)
                        path.lineTo(cx+7*density,y+5*density)
                        path.moveTo(cx+7*density,y-5*density)
                        path.lineTo(cx-1*density,y+5*density)
                    }
                    else -> {
                        path.moveTo(cx+10*density,y-9*density)
                        path.lineTo(cx+10*density,y+3*density)
                        path.lineTo(cx-9*density,y+3*density)
                        path.moveTo(cx-3*density,y-3*density)
                        path.lineTo(cx-9*density,y+3*density)
                        path.lineTo(cx-3*density,y+9*density)
                    }
                }
                c.drawPath(path,paint)
                paint.style=Paint.Style.FILL
                return@forEach
            }
            if(hint!=null) {
                paint.textSize=22f*density
                c.drawText(text,key.rect.centerX(),cy+5f*density,paint)
                paint.textSize=10f*density
                paint.color=if(dark) Color.LTGRAY else Color.DKGRAY
                paint.textAlign=Paint.Align.RIGHT
                c.drawText(hint,key.rect.right-4f*density,key.rect.top+11f*density,paint)
            } else c.drawText(text,key.rect.centerX(),cy,paint)
        }
        if(swipeActive && swipePoints.size>1 && prefs.getBoolean("swipe_trail",true)) {
            paint.color=accent()
            paint.style=Paint.Style.STROKE
            paint.strokeWidth=4f*density
            paint.strokeCap=Paint.Cap.ROUND
            val trail=Path()
            trail.moveTo(swipePoints[0].x,swipePoints[0].y)
            for(i in 1 until swipePoints.size) trail.lineTo(swipePoints[i].x,swipePoints[i].y)
            c.drawPath(trail,paint)
            paint.style=Paint.Style.FILL
        }
        pressed?.takeIf { it.text.length==1 && !numeric && !swipeActive }?.let { key ->
            val r=RectF(key.rect.left-5*density,key.rect.top-48*density,key.rect.right+5*density,key.rect.top+3*density)
            paint.color=if(dark) Color.rgb(100,103,110) else Color.WHITE
            c.drawRoundRect(r,8*density,8*density,paint)
            paint.color=if(dark) Color.WHITE else Color.BLACK
            paint.textSize=29*density
            c.drawText(if(shift&&!symbols)key.text.uppercase() else key.text,r.centerX(),r.centerY()-(paint.ascent()+paint.descent())/2,paint)
        }
    }

    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                accessoryHandled=false
                parent?.requestDisallowInterceptTouchEvent(true)
                if(event.y<resizeHandleHeight) {
                    resizing=true; resizeStartY=event.rawY
                    resizeStartHeight=prefs.getInt("key_height",53).coerceIn(38,90)
                    cancelPendingLongPress(); return true
                }
                if(event.y>height-accessoryHeight) {
                    accessoryHandled=true
                    val index=(event.x/(width/4f)).toInt().coerceIn(0,3)
                    onKey(listOf("😊","📋","📖","🌐")[index])
                    return true
                }
                downX=event.x; downY=event.y; previousSwipeX=event.x; previousSwipeY=event.y; spaceDrag=false; cursorSteps=0; cursorVerticalSteps=0
                swipeActive=false; swipePoints.clear(); swipeLetters.clear()
                if(swipeEnabled() && !symbols && !numeric && !emojiMode && !clipboardMode) addSwipePoint(event.x,event.y)
                pressed=smartHit(event.x,event.y)
                downKey=pressed
                if(downKey?.text=="⌫") {
                    backspaceHeld=true; backspaceStart=android.os.SystemClock.uptimeMillis()
                    longPressHandler.postDelayed(repeatBackspace,430L)
                }
                longPressTriggered=false
                cancelPendingLongPress()
                if(pressed!=null) longPressHandler.postDelayed(longPressTask,550L)
                invalidate(); return true
            }
            MotionEvent.ACTION_MOVE -> {
                if(accessoryHandled) return true
                if(resizing) {
                    val next=(resizeStartHeight+((resizeStartY-event.rawY)/density/4f).toInt()).coerceIn(38,90)
                    if(next!=prefs.getInt("key_height",53)) {
                        prefs.edit().putInt("key_height",next).apply()
                        requestLayout(); rebuild(width); invalidate()
                    }
                    return true
                }
                if(hypot(event.x-downX,event.y-downY)>12f*density) cancelPendingLongPress()
                if(longPressTriggered) return true
                if(downKey?.text=="space" && (abs(event.x-downX)>14f*density || abs(event.y-downY)>14f*density)) {
                    spaceDrag=true
                    val target=((event.x-downX)/(23f*density)).toInt()
                    val delta=(target-cursorSteps).coerceIn(-20,20)
                    if(delta>0) onKey("CURSOR_RIGHT:" + delta)
                    if(delta<0) onKey("CURSOR_LEFT:" + (-delta))
                    cursorSteps+=delta
                    val verticalTarget=((event.y-downY)/(26f*density)).toInt()
                    val verticalDelta=(verticalTarget-cursorVerticalSteps).coerceIn(-12,12)
                    if(verticalDelta>0) onKey("CURSOR_DOWN:"+verticalDelta)
                    if(verticalDelta<0) onKey("CURSOR_UP:"+(-verticalDelta))
                    cursorVerticalSteps+=verticalDelta
                } else if(!spaceDrag && swipeEnabled() && !symbols && !numeric && !emojiMode && !clipboardMode &&
                    downKey?.text?.length==1 && downKey?.text?.firstOrNull() in 'a'..'z' &&
                    hypot(event.x-downX,event.y-downY)>22f*density) {
                    if(!swipeActive) {
                        swipeLetters.clear()
                        downKey?.text?.let { swipeLetters.append(it) }
                        previousSwipeX=downX; previousSwipeY=downY
                    }
                    swipeActive=true
                    fun interpolate(x:Float,y:Float) {
                        val distance=hypot(x-previousSwipeX,y-previousSwipeY)
                        val steps=(distance/(9f*density)).toInt().coerceIn(1,50)
                        for(step in 1..steps) {
                            val ratio=step.toFloat()/steps
                            addSwipePoint(previousSwipeX+(x-previousSwipeX)*ratio,previousSwipeY+(y-previousSwipeY)*ratio)
                        }
                        previousSwipeX=x; previousSwipeY=y
                    }
                    for(i in 0 until event.historySize) interpolate(event.getHistoricalX(i),event.getHistoricalY(i))
                    interpolate(event.x,event.y)
                } else if(!spaceDrag && hypot(event.x-downX,event.y-downY)>18f*density) {
                    pressed=smartHit(event.x,event.y) ?: pressed
                }
                invalidate(); return true
            }
            MotionEvent.ACTION_UP -> {
                val heldBackspace=backspaceHeld
                stopBackspace()
                if(heldBackspace && android.os.SystemClock.uptimeMillis()-backspaceStart>=430L) { pressed=null; invalidate(); performClick(); return true }
                if(accessoryHandled) { accessoryHandled=false; performClick(); return true }
                if(resizing) { resizing=false; performClick(); return true }
                cancelPendingLongPress()
                if(longPressTriggered) {
                    longPressTriggered=false; pressed=null; invalidate(); performClick(); return true
                }
                if(swipeActive && !spaceDrag) {
                    addSwipePoint(event.x,event.y)
                    val rawSwipe=swipeLetters.toString()
                    val decoded=SwipeDecoder.decode(rawSwipe)
                    val alternatives=SwipeDecoder.alternatives(swipeLetters.toString())
                    swipeActive=false; swipePoints.clear(); swipeLetters.clear(); pressed=null
                    invalidate()
                    if(decoded!=null) { onKey("SWIPE_WORD:"+decoded); if(alternatives.size>1) setSuggestions(alternatives) }
                    else if(rawSwipe.isNotEmpty()) onKey("SWIPE_WORD:"+rawSwipe)
                    performClick()
                    return true
                }
                if(!clipboardMode && !emojiMode && downY<barHeight && event.y<barHeight && suggestions.isNotEmpty()) {
                    val index=(event.x/(width/3f)).toInt().coerceIn(0,suggestions.size-1)
                    pressed=null; invalidate()
                    onKey("SUGGEST:"+suggestions[index])
                    performClick()
                    return true
                }
                val key=if(spaceDrag) null else smartHit(event.x,event.y) ?: pressed
                pressed=null; invalidate()
                if(key!=null && abs(event.y-downY)<rowHeight*1.4f) {
                    if(prefs.getBoolean("haptics",true)) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if(key.text=="space" && prefs.getBoolean("double_space_period",true) && !emojiMode) {
                        val now=android.os.SystemClock.uptimeMillis()
                        if(now-lastTapSpace<350) { onKey("DOUBLE_SPACE_PERIOD"); lastTapSpace=0L }
                        else { onKey(key.text); lastTapSpace=now }
                    } else {
                        if(key.text!="space") lastTapSpace=0L
                        onKey(key.text)
                    }
                }
                performClick(); return true
            }
            MotionEvent.ACTION_CANCEL -> { stopBackspace(); accessoryHandled=false; resizing=false; cancelPendingLongPress(); longPressTriggered=false; pressed=null; spaceDrag=false; swipeActive=false; swipePoints.clear(); swipeLetters.clear(); invalidate(); return true }
        }
        return true
    }
    override fun onDetachedFromWindow() { stopBackspace(); cancelPendingLongPress(); super.onDetachedFromWindow() }
    override fun performClick():Boolean { super.performClick(); return true }
}
