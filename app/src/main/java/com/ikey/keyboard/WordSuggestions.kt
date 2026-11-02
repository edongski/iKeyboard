package com.ikey.keyboard

/**
 * Small offline prediction and typo-correction engine.
 * Designed as a starter dictionary, not a full language model.
 */
object WordSuggestions {
    private val words = ("""
the and you that this with have from your what when where there their they them then
will would could should about after before again always because between people really
hello thanks thank please sorry good great morning afternoon evening night today
tomorrow yesterday welcome congratulations birthday happy friend friends family
message messages meeting meetings keyboard typing android iphone phone computer
work working office home school food water coffee lunch dinner breakfast
yes okay sure maybe later soon see talk call send email reply update
kumusta kamusta salamat magandang umaga hapon gabi tanghali po opo oo hindi
ako ikaw tayo kami kayo sila siya natin namin nila niya ko mo ka na pa ba
lang naman kasi sana sige ingat mahal kita gusto ayaw ayoko kailangan
pwede puwede talaga sobrang masarap pagkain kanin tubig bahay trabaho
pamilya kaibigan ngayon mamaya bukas kahapon tara kain dito doon saan
bakit paano kailan sino ano meron wala mayroon
""").trim().split(Regex("\\s+")).distinct()
    private val next = mapOf(
        "good" to listOf("morning","night","afternoon"),
        "thank" to listOf("you","you","you"),
        "how" to listOf("are","much","to"),
        "i" to listOf("am","will","have"),
        "you" to listOf("are","can","have"),
        "see" to listOf("you","it","more"),
        "magandang" to listOf("umaga","hapon","gabi"),
        "salamat" to listOf("po","sa","talaga"),
        "kumusta" to listOf("ka","po","kayo"),
        "mahal" to listOf("kita","ko","namin"),
        "tara" to listOf("na","kain","tayo")
    )
    private fun distance(a:String,b:String):Int {
        var prev=IntArray(b.length+1){it}
        for(i in a.indices) {
            val cur=IntArray(b.length+1)
            cur[0]=i+1
            for(j in b.indices) cur[j+1]=minOf(cur[j]+1,prev[j+1]+1,prev[j]+if(a[i]==b[j]) 0 else 1)
            prev=cur
        }
        return prev[b.length]
    }
    fun candidates(prefix:String, previous:String="", learned:Collection<String> = emptyList()):List<String> {
        val p=prefix.lowercase().filter { it.isLetter() }
        if(p.isEmpty()) return (next[previous.lowercase()] ?: listOf("the","and","you")).distinct().take(3)
        val dictionary=(learned+words).filter { it.length>=2 }.distinct()
        val starts=dictionary.filter { it.startsWith(p) && it!=p }.sortedWith(compareBy<String>{it.length}.thenBy{it}).take(3)
        val corrections=if(p.length>=3) dictionary.filter { it.firstOrNull()==p.firstOrNull() && kotlin.math.abs(it.length-p.length)<=2 && it!=p }
            .map { it to distance(p,it) }.filter { it.second<=if(p.length>=6) 2 else 1 }
            .sortedBy { it.second }.map { it.first }.take(3) else emptyList()
        return (listOf(p)+starts+corrections).distinct().take(3)
    }
    fun correction(word:String, learned:Collection<String> = emptyList()):String? {
        val p=word.lowercase()
        if(p.length<3 || p in words || p in learned) return null
        return candidates(p,"",learned).drop(1).firstOrNull { distance(p,it)<=1 }
    }
}
