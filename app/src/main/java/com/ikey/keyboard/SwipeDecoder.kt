package com.ikey.keyboard

import kotlin.math.abs

/**
 * Offline, deterministic Phase 3 swipe recognizer.
 * Matches a compressed key path to a small English/Filipino starter lexicon.
 * No typed text or gestures leave the device.
 */
object SwipeDecoder {
    private val words by lazy { WordSuggestions.vocabulary.filter { it.length in 2..24 && it.all { ch -> ch in 'a'..'z' } } }
    private val indexed by lazy { words.groupBy { it.first() to it.last() } }
    private val lastPath = ThreadLocal<String>()
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
    private fun ranked(raw:String, learned:Collection<String>):List<Pair<String,Int>> {
        val path=compact(raw)
        if(path.length<2) return emptyList()
        val pool=indexed[path.first() to path.last()].orEmpty() +
            learned.filter { it.length>=2 && it.firstOrNull()==path.first() && it.lastOrNull()==path.last() }
        return pool.asSequence().distinct().filter { abs(compact(it).length-path.length)<=maxOf(5,path.length/2) }
            .map { word ->
                val shape=compact(word)
                val edit=distance(path,shape)
                val subsequence=shape.fold(0) { index,ch ->
                    if(index<path.length && path.indexOf(ch,index)>=0) path.indexOf(ch,index)+1 else index
                }
                val penalty=if(subsequence==0) 2 else 0
                word to (edit*3+abs(shape.length-path.length)+penalty)
            }.sortedWith(compareBy<Pair<String,Int>> { it.second }.thenBy { it.first.length })
            .take(3).toList()
    }
    fun alternatives(raw:String, learned:Collection<String> = emptyList()):List<String> =
        ranked(raw,learned).map { it.first }
    fun decode(raw:String, learned:Collection<String> = emptyList()):String? =
        ranked(raw,learned).firstOrNull()?.first
}
