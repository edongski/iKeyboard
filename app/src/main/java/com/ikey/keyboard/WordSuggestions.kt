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
    private val extraWords = ("""
a an am is are was were be been being do does did done can could shall should may might must
have has had having would will just also only even still already almost always never often
usually sometimes every each any all some many much more most less few several another
someone anyone everyone nobody something anything everything nothing here there everywhere
why how where when which whose whom whatever whenever wherever however although though
while until since unless whether either neither both together apart around across through
inside outside above below under behind beside near far left right north south east west
first second third last next previous new old young big small large little long short tall
high low fast slow early late easy hard simple difficult possible impossible important
different same similar better best worse worst strong weak healthy sick safe dangerous
beautiful pretty handsome cute funny serious kind gentle honest true false correct wrong
open close start stop finish continue begin end wait stay leave arrive return travel drive
walk run jump swim dance sing read write listen speak say tell ask answer learn teach study
think know understand remember forget believe feel hope wish want need choose decide try
help share give take bring buy sell pay spend save earn find lose keep hold carry put move
make create build fix repair change improve develop design plan organize manage review test
check verify download upload install uninstall update connect disconnect enable disable
account password username profile settings options preferences notification notifications
privacy security permission permissions data file folder document picture photo image video
audio music sound voice microphone camera screen display battery charging network internet
wifi bluetooth mobile tablet laptop desktop windows mac linux application software program
project feature function button menu tab page search filter sort edit delete copy paste cut
select selected selection cursor sentence paragraph character letter word dictionary language
english filipino tagalog taglish translation translate grammar spelling punctuation suggestion
prediction predictive correction autocorrect keyboard swipe gesture touch typing type text
keyboard layout theme color dark light background height width size font style appearance
accessibility vibration haptic feedback sound emoji sticker gif clipboard shortcut history
friendship relationship husband wife mother father brother sister son daughter children child
baby parent cousin aunt uncle grandmother grandfather teacher student manager employee boss
client customer buyer seller partner colleague teammate group team leader member community
company business marketing sales finance budget payment price cost income expense profit
investment investor real estate property house condominium apartment rent lease contract
meeting conference seminar workshop training presentation event party celebration award
birthday anniversary holiday vacation weekend weekday monday tuesday wednesday thursday
friday saturday sunday january february march april june july august september october
november december one two three four five six seven eight nine ten hundred thousand million
zero twenty thirty forty fifty sixty seventy eighty ninety morning noon afternoon evening
midnight breakfast lunch dinner snack restaurant kitchen cooking cook cooked recipe ingredients
chicken pork beef fish shrimp vegetable vegetables fruit fruits banana mango apple orange
rice bread egg eggs coffee tea milk juice water soup noodles pasta pizza burger sandwich
salad dessert sweet spicy salty sour bitter delicious hungry thirsty full empty fresh hot cold
weather rain sunny cloudy windy storm typhoon flood traffic road street city town province
country island beach mountain river lake ocean sea sky sun moon star stars world earth nature
school university college classroom class lesson exam question questions score grade result
hospital doctor nurse medicine health wellness exercise workout running marathon walk walking
sleep sleeping wake waking rest relaxing happy sad angry excited nervous tired sleepy bored
love loved loving miss missed missing care caring sorry apologize thank thanks welcome
please congratulations amazing awesome excellent perfect fantastic wonderful great nice okay
hello hi hey goodbye bye goodnight goodmorning goodafternoon goodevening
ang mga ng sa si sina kay kina para mula mula sa tungkol tungkol sa ayon ayon
ito iyan iyon itong iyang iyong ganito ganyan ganoon dito diyan doon
ako ikaw siya kami tayo kayo sila akin iyo kanya amin atin inyo kanila
ko mo niya namin natin ninyo nila kong mong niyang naming nating inyong nilang
akin akin ka naman talaga pala daw raw nga din rin lang lamang pa na ba po opo
hindi oo wala mayroon meron pwede puwede maaari bawal dapat kailangan
gusto ayaw ayoko gusto ko gusto mo alam ko alam mo hindi ko hindi mo
kasi dahil kaya pero ngunit subalit kung kapag habang kahit bagaman
at o saka pati rin din naman lamang lang sobrang masyado talagang
mahal kita mahal ko mahal mo miss na kita miss kita ingat ka ingat kayo
kamusta kumusta kamusta ka kumusta po kumusta kayo salamat maraming
salamat po maraming salamat walang anuman pasensya paumanhin
magandang umaga magandang hapon magandang gabi magandang araw
magandang tanghali magandang buhay maligayang kaarawan maligayang pasko
bagong taon taon araw buwan linggo oras minuto segundo ngayon bukas kahapon
kanina mamaya mamayang gabi mamayang umaga mamayang hapon sandali muna
tara na tayo kain na tayo uwi na tayo alis na tayo punta tayo
saan bakit paano kailan sino ano alin ilan magkano gaano
bahay tahanan kwarto silid kusina banyo sala pinto bintana bubong
trabaho opisina empleyado amo boss sahod pera bayad presyo halaga
utang ipon tipid negosyo tindahan palengke bilihan pagkain ulam
kanin sinangag adobo sinigang tinola menudo kaldereta bistek sisig
tapa tocino longganisa itlog isda hipon gulay prutas saging mangga
mansanas pakwan pinya kape gatas tsaa tubig juice malamig mainit
masarap malinamnam matamis maasim maalat mapait maanghang busog gutom
pamilya nanay tatay ate kuya bunso kapatid anak asawa lolo lola
kaibigan kasamahan kasama katrabaho kapitbahay kaklase guro estudyante
paaralan unibersidad aral pagaaral pagsusulit tanong sagot
lakad takbo takbuhan tumakbo naglakad nagtrabaho nagtatrabaho
pagod pahinga tulog gising masaya malungkot galit takot kaba
maganda gwapo pogi cute mabait masipag matalino matapang malakas
mahina mabilis mabagal malaki maliit mataas mababa mahaba maikli
bago luma tama mali totoo hindi totoo oo naman sige okay ayos
maayos mabuti mabuhay paalam kita tayo ulit mamaya bukas
""").trim().split(Regex("\\s+")).distinct()
    private val commonFilipino = """
    abala abangan abante abot agad agahan agila ahas akala aklat akyat alaala alaga
    alalahanin alangan alamin alam araw araw-araw asahan asikaso asim atensyon
    bagal bagyo bahala balak balita balik balikan balikat balot bangon bantay basa
    basahin batay batang bata bayani bayan bayaran bentahan bili bilhin bilis bitbit
    bukas bukod buhay bulaklak bundok buong buo buti buto
    dahilan dalawa dalhin daliri damdamin dami damihan damo dating dati
    dayuhan dagdag dagdagan dalaw dalawin daloy dasal dasalan dahan dahan-dahan
    desisyon dibdib dilim dilaw diretso diskarte diwa doble doktor dulo dumaan
    dumating dumiretso dumulas dumami dumalo dumating dumaan dun
    gabi gabi-gabi gabay galaw galit gamit gamitin ganda ganito ganun gawa
    gawin gawi ginamit ginawa gising gulay gulong gubat guro
    hakbang halos halaga halimbawa hanap hanapin hangin hanggang handa handaan
    handog hapunan harap harapan hapon hati hatid hatiran hawak hawakan
    hirap hiramin hinay hintay hintayin hinto hiling hinahanap hinahanap
    ibaba ibalik ibigay ibili ibon idagdag idaan ihatid iisip isip
    ilagay ilabas ilaw ilayo ilang ilog imahe imbitahan inaalala inaasahan
    ingatan ininom iniisip inip init inumin ipakita ipasa ipon
    isa isang isama isulat isuko itabi itaas itago itapon itigil ito
    kaagad kababayan kabaitan kabuhayan kadalasan kagabi kagandahan kahapon
    kaharap kahit kailan kaibigan kailangan kakain kakilala kakayanin kalaban
    kalamangan kalangitan kalat kalayaan kalimutan kalinawan kalikasan
    kalusugan kamay kaming kanina kanan kandila kantahan kapatid kapag
    kapalit kapayapaan karapatan karaniwan karanasan kasalanan kasal kasali
    kasambahay kasama kasiyahan kasunod katabi katapusan katawan katotohanan
    kawalan kawawa kaya kayang kaya-kaya kilala kilalanin kilos kinabukasan
    kinain kinuha kita kulay kumain kumanta kumilos kumpleto kumuha
    laban labas labanan lagay lagyan lahat lakas lalaki lamang lambing
    langit lapit lapitan laro laruan lasang layo likod likas liham
    libre ligaya ligtas linis linisin lipat lumabas lumakad lumaki lumapit
    lumayo lumilipad lumipat lumunok luto lutong luwag
    madali madalas magaan maganda magaling maganda magbago magbayad magbigay
    magbukas magdala magdasal maghanap maghintay magisip maglakad maglaro
    magluto magmahal magmaneho magpahinga magpaalam magpasalamat magpunta
    magsabi magsama magsimula magsulat magtanong magtrabaho magturo
    mahaba mahala mahalaga mahirap mahinahon mahina mahuli mainam mainit
    makakaya makalipas makapunta makatao makita malakas malapit malayo
    maliban maligaya malinis malumanay maluwag mamaya manalo manamit
    maniwala manok marami masaya masipag matagal matakot matalino matanda
    matapang matibay matino mayaman mayroon medyo minsan minuto
    naaalala naalala nagaaral nagbago nagbigay nagdaan nagdala naghahanap
    naghihintay naglakad naglalaro nagluto nagmahal nagpunta nagsabi
    nagsimula nagtapos nagtatanong nagtrabaho nagulat nakita nalaman
    naligo nalimutan nanalo nandito nandiyan nandoon napagod narinig
    nasaan nasabi nasaktan nasanay natapos natulog natural nawala
    ngiti ngumiti nilaga nilagay niluto ninanais nobela numero
    oras oras-oras paalala paalam paakyat pagasa pagbayad pagbili pagdaan
    pagdating pagibig pagkain paglalakbay paglalakad paglalaro paglipat
    pagmamahal pagmamadali pagod pagpili pagpunta pagsabi pagsama
    pagsisimula pagtatapos pagtatanong pagtawa pagtulong paguwi
    pahinga pahiram pakain pakialam pakiramdam palagi palakasin
    palengke palitan pamahalaan pamasahe panahon panalo panaginip
    panalangin pananaw panay pandesal pangako panganay pangarap
    pangit pangyayari pansin pansinin pantay panto pantulog
    paraan pareho parating parte pasok pasukan pataas patak patuloy
    patingin patnubay payapa payong pera perpekto pili piliin pinili
    pinoy pintuan pisngi pito plano po porma posibleng presyo
    problema proseso programa proyekto puno punong punas puntahan
    puso puti puyat
    regalo relasyon respeto resulta riles rosas
    sabi sabihin saglit sakit saksi sakyan sala salamin salita salubong
    sama samahan samantala sandali sanggol sapatos sapat sarap sariwa
    sarili sasakyan sasama sasayaw sasabihin sayang sayaw seryoso
    serbisyo sigla sigurado silid simula simulan simpleng singil
    sinubukan sipag sitwasyon sobra solusyon sulat sumagot sumakay
    sumali sumama sumayaw sumigaw sumulat sundin sunod susi
    taas tabi tabihan tagal tagalog tagumpay tahimik takbo takot
    takpan tala talagang talino tama tamaan tambay tanaw tanong
    tapang tapos tapusin taralets tatay tawag tawagan tawa tawanan
    tayo teknolohiya telepono tiwala tiyak tindahan tingin tingnan
    tipid tira tirahan tulog tulong tulungan tumakbo tumawa tumingin
    tumira tumulong tunog tunay tuwa tuwing
    ubos uhaw ulam ulan ulap uli ulit umaga umalis umakyat umasa
    umiinom umiiyak umiikot umikot umalis umuwi unang unahan
    unawain unti unti-unti usap usapan uso utak utang utos uwi
    wagas wala walang walis wasto wika
    """.trim().split(Regex("\\s+")).filter { it.all { ch -> ch in 'a'..'z' } }
    val vocabulary:List<String> by lazy { (words+extraWords+commonFilipino).distinct() }
    private val byInitial by lazy { vocabulary.groupBy { it.firstOrNull() } }
    private val correctionCache=object:LinkedHashMap<String,String?>(256,0.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,String?>?):Boolean = size>256
    }
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
        val dictionary=(learned+byInitial[p.firstOrNull()].orEmpty()).filter { it.length>=2 }.distinct()
        val starts=dictionary.filter { it.startsWith(p) && it!=p }.sortedWith(compareBy<String>{it.length}.thenBy{it}).take(3)
        val corrections=if(p.length>=3) dictionary.filter { it.firstOrNull()==p.firstOrNull() && kotlin.math.abs(it.length-p.length)<=2 && it!=p }
            .map { it to distance(p,it) }.filter { it.second<=if(p.length>=6) 2 else 1 }
            .sortedBy { it.second }.map { it.first }.take(3) else emptyList()
        return (listOf(p)+starts+corrections).distinct().take(3)
    }
    fun correction(word:String, learned:Collection<String> = emptyList()):String? {
        val p=word.lowercase()
        if(p.length<3 || p in vocabulary || p in learned) return null
        if(learned.isEmpty() && correctionCache.containsKey(p)) return correctionCache[p]
        val result=(learned+byInitial[p.firstOrNull()].orEmpty()).asSequence()
            .filter { it.length>=2 && kotlin.math.abs(it.length-p.length)<=1 && it.firstOrNull()==p.firstOrNull() }
            .map { it to distance(p,it) }
            .filter { it.second==1 }
            .minWithOrNull(compareBy<Pair<String,Int>> { it.second }.thenBy { kotlin.math.abs(it.first.length-p.length) })
            ?.first
        if(learned.isEmpty()) correctionCache[p]=result
        return result
    }
}
