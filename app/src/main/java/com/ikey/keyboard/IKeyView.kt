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
    private var shift = false
    private var numeric = false
    private var dark = false
    private var pressed: Key? = null
    private val prefs = context.getSharedPreferences("ikey", Context.MODE_PRIVATE)
    private var downX = 0f
    private var downY = 0f
    private var spaceDrag = false
    private var cursorSteps = 0
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
    private val rowHeight = 53f * density
    private val margin = 3f * density
    init { isClickable = true; setLayerType(LAYER_TYPE_SOFTWARE,null) }
    fun update(s: Boolean, sh: Boolean, n: Boolean, d: Boolean) { symbols=s; shift=sh; numeric=n; dark=d; rebuild(width); invalidate() }
    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        setMeasuredDimension(width, (4*rowHeight).toInt() + (12*density).toInt())
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh); rebuild(w) }
    private fun rebuild(w:Int) {
        if(w<=0)return
        keys.clear()
        val rows = if(numeric) listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("ABC","0","⌫","↵"))
        else if(symbols) listOf(listOf("1","2","3","4","5","6","7","8","9","0"),listOf("@","#","$","%","&","-","+","(",")","/"),listOf("*","\" ",":",";","!","?","'","⌫"),listOf("ABC",",","🌐","space",".","↵"))
        else listOf("qwertyuiop".map{it.toString()},"asdfghjkl".map{it.toString()},listOf("⇧")+"zxcvbnm".map{it.toString()}+"⌫",listOf("?123",",","🌐","space",".","↵"))
        rows.forEachIndexed { row, items ->
            val weights=items.map { when(it) { "space"->4.0f; "⇧","⌫","?123","ABC","↵"->1.45f; "🌐"->1.1f; else->1f } }
            val total=weights.sum()
            val usable=w-2*margin
            val unit=usable/total
            var x=margin
            if(row==1 && !symbols && !numeric) {
                val natural=usable/10f*9f
                x+=(usable-natural)/2
            }
            items.forEachIndexed { i, t ->
                val width= if(row==1 && !symbols && !numeric) usable/10f else weights[i]*unit
                val y=row*rowHeight+margin
                keys.add(Key(t,RectF(x+margin,y,x+width-margin,y+rowHeight-2*margin)))
                x+=width
            }
        }
    }
    override fun onDraw(c:Canvas) {
        c.drawColor(if(dark) Color.rgb(42,44,49) else Color.rgb(208,211,219))
        keys.forEach { key ->
            val special=key.text in listOf("⇧","⌫","?123","ABC","↵","🌐")
            val color=when {
                pressed===key -> if(dark) Color.rgb(105,108,116) else Color.rgb(244,245,247)
                dark && special -> Color.rgb(85,88,95)
                dark -> Color.rgb(66,69,76)
                special -> Color.rgb(171,177,188)
                else -> Color.WHITE
            }
            paint.color=color
            paint.setShadowLayer(1.5f*density,0f,1.5f*density,0x50000000)
            c.drawRoundRect(key.rect,5*density,5*density,paint)
            paint.clearShadowLayer()
            paint.color=if(dark) Color.WHITE else Color.rgb(23,25,31)
            paint.textAlign=Paint.Align.CENTER
            paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
            paint.textSize=(if(key.text=="space") 13 else if(key.text.length>1) 14 else 23)*density
            val text=if(shift && !symbols) key.text.uppercase() else key.text
            val cy=key.rect.centerY()-(paint.ascent()+paint.descent())/2
            c.drawText(text,key.rect.centerX(),cy,paint)
        }
        pressed?.takeIf { it.text.length==1 && !numeric }?.let { key ->
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
                downX=event.x; downY=event.y; spaceDrag=false; cursorSteps=0
                pressed=smartHit(event.x,event.y)
                invalidate(); return true
            }
            MotionEvent.ACTION_MOVE -> {
                if(pressed?.text=="space" && abs(event.x-downX)>14f*density) {
                    spaceDrag=true
                    val target=((event.x-downX)/(23f*density)).toInt()
                    val delta=(target-cursorSteps).coerceIn(-20,20)
                    if(delta>0) onKey("CURSOR_RIGHT:" + delta)
                    if(delta<0) onKey("CURSOR_LEFT:" + (-delta))
                    cursorSteps+=delta
                } else if(!spaceDrag && hypot(event.x-downX,event.y-downY)>18f*density) {
                    pressed=smartHit(event.x,event.y) ?: pressed
                }
                invalidate(); return true
            }
            MotionEvent.ACTION_UP -> {
                val key=if(spaceDrag) null else smartHit(event.x,event.y) ?: pressed
                pressed=null; invalidate()
                if(key!=null && abs(event.y-downY)<rowHeight*1.4f) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onKey(key.text)
                }
                performClick(); return true
            }
            MotionEvent.ACTION_CANCEL -> { pressed=null; spaceDrag=false; invalidate(); return true }
        }
        return true
    }
    override fun performClick():Boolean { super.performClick(); return true }
}
