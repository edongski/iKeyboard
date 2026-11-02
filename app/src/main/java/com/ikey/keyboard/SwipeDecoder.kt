package com.ikey.keyboard

import kotlin.math.abs

/**
 * Offline, deterministic Phase 3 swipe recognizer.
 * Matches a compressed key path to a small English/Filipino starter lexicon.
 * No typed text or gestures leave the device.
 */
object SwipeDecoder {
    private val words = ("""
the of and to in is you that it he was for on are as with his they i at be this have from
or one had by word but not what all were we when your can said there use an each which
she do how their if will up other about out many then them these so some her would make
like him into time has look two more write go see number no way could people my than
first water been call who oil its now find long down day did get come made may part
hello hi hey thanks thank please yes good morning afternoon evening night today tomorrow
yesterday keyboard phone android iphone swipe typing quick brown fox jumps over lazy dog
how are you doing where when why what who can will should would could love great nice
happy birthday congratulations welcome friend family work home school office meeting
message email send receive okay ok sure later soon see take care sorry excuse me
filipino tagalog taglish kumusta kamusta salamat po opo oo hindi ako ikaw tayo kami kayo
sila siya siya naman na nga ba pa lang kasi sana sige ingat mahal kita ko mo niya nila
natin namin inyong ating magandang umaga hapon gabi tanghali araw bukas kahapon
tara kain tayo na dito doon saan bakit paano kailan sino ano meron wala mayroon
pwede puwede gusto ayoko ayaw kailangan talaga sobra masarap pagkain tubig kanin
bahay trabaho pera pamilya kaibigan mahal kita miss na kita mamaya sandali ngayon
""").trim().split(Regex("\\s+")).filter { it.length>=2 }.distinct()
    private fun compact(s: String): String {
        val out=StringBuilder()
        for(c in s.lowercase()) if(c in 'a'..'z' && (out.isEmpty() || out.last()!=c)) out.append(c)
        return out.toString()
    }
    private fun distance(a:String,b:String):Int {
        if(a.isEmpty()) return b.length
        if(b.isEmpty()) return a.length
        var prev=IntArray(b.length+1){it}
        for(i in a.indices) {
            val curr=IntArray(b.length+1)
            curr[0]=i+1
            for(j in b.indices) curr[j+1]=minOf(
                curr[j]+1, prev[j+1]+1,
                prev[j]+if(a[i]==b[j]) 0 else 1
            )
            prev=curr
        }
        return prev[b.length]
    }
    fun decode(raw:String):String? {
        val path=compact(raw)
        if(path.length<2) return null
        val exact=words.firstOrNull { compact(it)==path }
        if(exact!=null) return exact
        return words.asSequence().filter {
            val candidate=compact(it)
            candidate.firstOrNull()==path.firstOrNull() &&
            candidate.lastOrNull()==path.lastOrNull() &&
            abs(candidate.length-path.length)<=3
        }.map { it to distance(path,compact(it)) }
         .minWithOrNull(compareBy<Pair<String,Int>> { it.second }.thenBy { abs(it.first.length-path.length) })
         ?.takeIf { it.second<=maxOf(1,path.length/3) }?.first
    }
}
