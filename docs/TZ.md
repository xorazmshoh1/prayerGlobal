# Texnik topshiriq (TZ)

> PDF dan avtomatik ko'chirilgan matn. Asl hujjat: "3 ta Android ilova uchun texnik topshiriq va dizayn".

```text

Nov 17, 2026 · ​@Xumoyun

Umumiy qism
Uchala ilova bitta texnologik asosda quriladi: Kotlin + Jetpack Compose, internetsiz ishlash, AdMob reklamasi. Shunda birinchi ilovada yozilgan kod (reklama, sozlamalar, dizayn tizimi) keyingi ikkitasida qayta ishlatiladi. Maqsad: kunlik ishlatiladigan, reklama orqali daromad keltiradigan kichik ilovalar. Har biri 1–2 haftada MVP holatiga keltiriladi. Prava va Abituriyent O'zbekiston bozori uchun, Namoz vaqtlari esa butun dunyo uchun global ilova.
Umumiy texnologiyalar

Qatlam Til UI Arxitektura DI Ma'lumotlar bazasi Sozlamalar Navigatsiya Reklama
Rozilik Analitika
Fon ishlari

Tanlov Kotlin 2.x Jetpack Compose + Material 3 MVVM + Repository Hilt Room (SQLite)

Izoh Android rasmiy tili Barcha ekranlar Compose'da ViewModel, StateFlow Bog'liqliklarni ulash Testlar, natijalar, sozlamalar

DataStore Preferences Navigation Compose Google Mobile Ads SDK (AdMob) Google UMP SDK Firebase Analytics + Crashlytics WorkManager, AlarmManager

Til, tema, oxirgi holat Ekranlar orasida o'tish Banner, interstitial, rewarded
GDPR/rozilik oynasi Bepul, xatolarni kuzatish
Eslatma va bildirishnomalar


3 ta Android ilova uchun texnik topshiriq va dizayn

Qatlam Min SDK
Target SDK

Tanlov 24 (Android 7.0)
Google Play talab qilgan oxirgi versiya

Izoh
O'zbekistondagi eski telefonlar ham qamraladi Play Console'da tekshiring

Umumiy reklama siyosati
Reklama foydalanuvchini haydab yubormasligi kerak, aks holda reyting tushadi va ilova tarqalmaydi.
Banner: asosiy ekranlarning pastida, test yechish paytida ko'rsatilmaydi. Interstitial (to'liq ekran): faqat tabiiy tanaffuslarda (test tugaganda, natija ekraniga o'tishda). Kamida 3 daqiqa oraliq, sessiyada ko'pi bilan 3 marta. Rewarded (mukofotli): ixtiyoriy, foydalanuvchi o'zi tanlaydi. Masalan: "Xatolar tahlilini ochish uchun reklama ko'ring". App Open: ilova ochilganda, kuniga ko'pi bilan 1 marta. Reklama yuklanmasa, ilova kutmasdan ishlashda davom etadi. Ishlab chiqishda faqat test reklama IDlari ishlatiladi. Haqiqiy ID faqat relizda qo'yiladi, aks holda AdMob akkaunti bloklanishi mumkin.
Google Play talablari
Data safety formasi: AdMob va Firebase ma'lumot yig'ishini ko'rsatish. Maxfiylik siyosati sahifasi (URL kerak): bepul GitHub Pages yoki Google Sites'da joylash mumkin. Kontent reytingi anketasi va reklama bor belgisi. Maqsadli auditoriya: 13 yoshdan katta (16+ tavsiya). Bolalarga mo'ljallangan deb belgilansa, reklama cheklovlari ancha qattiq bo'ladi. Ilova nomi va tavsifida davlat idoralari nomi yoki logotipi ishlatilmaydi, ilova rasmiy emasligi tavsifda yoziladi.
Tillar
Prava va Abituriyent: o'zbekcha (lotin) asosiy til, ruscha ikkinchi til. Namoz vaqtlari: inglizcha asosiy til va jami 12 til (3-ilova bo'limida). O'zbekcha kirill ixtiyoriy, keyingi versiyada. Barcha matnlar strings.xml resurslarida, kodda matn yozilmaydi.


3 ta Android ilova uchun texnik topshiriq va dizayn

Umumiy dizayn tamoyillari
Material 3, yorug' va qorong'i tema, tizim temasiga moslashadi. Shrift: Inter (yoki tizim shrifti), o'lchamlar: sarlavha 22–24 sp, oddiy matn 16 sp, izoh 13 sp. Bosiladigan elementlar kamida 48 dp, burchaklar 12–16 dp. Ikonkalar: Material Symbols (Rounded). Har bir ilovaning o'z asosiy rangi bor (bo'limlarda ko'rsatilgan), qolgan ranglar Material 3 tomonidan shu rangdan yaratiladi. Bo'sh holatlar (empty state) va xatolik holatlari uchun alohida ekran yoki karta bo'ladi.

1-ilova: Haydovchilik guvohnomasi testlari
Ishchi nomi: "Prava Test 2026" (yakuniy nom Play'da band emasligini tekshirib tanlanadi). Ilova nazariy imtihonni aynan real formatda mashq qildiradi: 20 savol, 25 daqiqa, ko'pi bilan 2 xato.
Real imtihon formati (ilova shunga moslashadi)

Parametr

Qiymat

Savollar soni

20 ta

Vaqt

25 daqiqa, vaqt tugasa test avtomatik to'xtaydi

O'tish chegarasi A, B, C, D toifalari uchun kamida 18 to'g'ri, A1 uchun 16

Javob variantlari har savolda 2 tadan 5 tagacha, bittasi to'g'ri

Til

o'zbek, rus (Qoraqalpog'istonda qoraqalpoq)

Manbalar: Spot.uz, Infoedu.uz. Bozordagi raqobatchilar savollar bazasini taxminan 1240– 1270 ta deb ko'rsatadi (OsonPrava, Prava UZ).
Raqobatchilardan farqimiz
Bozorda OsonPrava, Prava UZ, Avtotest kabi ilovalar bor. Biz quyidagilar bilan ajralib turamiz:
To'liq internetsiz ishlaydi (savollar va rasmlar ilova ichida).


3 ta Android ilova uchun texnik topshiriq va dizayn
Aqlli takrorlash: xato qilingan savollar keyingi kunlarda qayta chiqadi (oddiy Leitner tizimi). Imtihonga tayyorlik foizi: oxirgi 5 ta imtihon simulyatsiyasi asosida "Siz 85% tayyorsiz" ko'rsatkichi. Ilova hajmi kichik (25 MB dan kam), eski telefonlarda ham tez ishlaydi.
Savollar bazasi: muhim ogohlantirish
Savollar bazasini raqobatchi ilova yoki saytlardan nusxalash mumkin emas: bu mualliflik huquqini buzadi va Google Play ilovani o'chirishi mumkin. Savollar rasmiy manbadan (YHXBB tomonidan e'lon qilingan materiallar, Yo'l harakati qoidalari matni) olinadi, izohlar esa o'zimiz yozamiz. Yo'l belgilarining rasmlari standart bo'yicha qayta chiziladi (SVG). Qaror: savollar bazasi rasmiy manbalardan (YHXBB e'lon qilgan materiallar va Yo'l harakati qoidalari matni) qo'lda yig'iladi, har bir savol manbasi JSON'da `source` maydonida yoziladi.
Funksiyalar
MVP (1-versiya): 1. Biletlar: savollar 20 talik biletlarga bo'lingan, har bir bilet natijasi saqlanadi. 2. Mavzular bo'yicha mashq: yo'l belgilari, chorrahalar, quvib o'tish, to'xtash, tibbiy yordam va h.k. 3. Imtihon rejimi: tasodifiy 20 savol, 25 daqiqalik taymer, 3-xatodan keyin "Imtihondan o'tolmadingiz" oynasi (real imtihondagidek). 4. Xatolar ustida ishlash: xato qilingan barcha savollar alohida ro'yxatda. 5. Saralanganlar: savolni yulduzcha bilan belgilash. 6. Statistika: yechilgan savollar, to'g'ri javoblar foizi, mavzular bo'yicha zaif joylar. 7. Til: o'zbekcha (lotin) va ruscha, sozlamalardan almashtiriladi. 8. Toifa tanlash: A1 uchun o'tish chegarasi 16, qolganlar uchun 18.
2-versiya: Yo'l belgilari ma'lumotnomasi (qidiruv bilan). Kunlik maqsad va eslatma ("Bugun 20 ta savol yeching"). Qorong'i rejim uchun alohida rasmlar. Qoraqalpoq tili.

3 ta Android ilova uchun texnik topshiriq va dizayn

Ekranlar

# Ekran

Asosiy elementlar

Reklama

1

Splash + til

Logo, o'zbekcha/ruscha tugmalari

yo'q

tanlash

(faqat birinchi ochilishda)

2

Bosh sahifa

Tayyorlik foizi halqasi, 4 ta katta karta: Biletlar, Mavzular, Imtihon, Xatolarim

banner pastda

3

Biletlar ro'yxati 2 ustunli setka, har bir bilet holati:

yechilmagan / o'tdi / yiqildi

banner

4

Mavzular

ro'yxati

Mavzu nomi, savollar soni, progress chizig'i

banner

5

Savol ekrani

Savol raqami (5/20), rasm, savol matni, yo'q javob variantlari, "Keyingi" tugmasi, taymer (imtihonda)

6

Natija ekrani

To'g'ri/xato soni, o'tdi/yiqildi, "Xatolarni ko'rish" tugmasi

interstitial ekranga kirishda

7

Xatolar tahlili

Har xato savol: sizning javobingiz, to'g'ri rewarded: batafsil

javob, izoh

izohni ochish

8

Statistika

Kunlar bo'yicha grafik, mavzular bo'yicha foiz

banner

9

Sozlamalar

Til, toifa, tema, ovoz, maxfiylik siyosati, yo'q baholash

Ma'lumotlar modeli (Room)
Question(id, ticketNo, topicId, textUz, textRu, imageRes, explanationUz, explanationRu) Answer(id, questionId, textUz, textRu, isCorrect, order) Topic(id, nameUz, nameRu, order) Attempt(id, mode, ticketNo?, startedAt, finishedAt, correct, wrong, passed) AttemptAnswer(attemptId, questionId, answerId, isCorrect)


3 ta Android ilova uchun texnik topshiriq va dizayn
QuestionStat(questionId, timesSeen, timesWrong, box, nextReviewAt, isFavorite)
Savollar bazasi assets/questions.json faylida keladi va birinchi ishga tushishda Room'ga yuklanadi. Rasmlar assets/images/ papkasida WebP formatida.
Dizayn
Asosiy rang: yo'l belgisi ko'ki #1565C0 . Urg'u rangi sariq #FFB300 (ogohlantirish belgilari kabi). To'g'ri javob: yashil #2E7D32 fon 12% shaffoflik bilan. Xato: qizil #C62828 . Bosh sahifa: tepada katta aylana progress ("Tayyorlik 72%"), ostida 2x2 setkada 4 ta karta, har birida ikonka (kitob, ro'yxat, taymer, ogohlantirish). Savol ekrani: rasm ekran kengligida, 16:9; javob variantlari raqamlangan katta kartalar (F1, F2, F3 kabi real imtihondagi belgilar bilan). Tanlangan javob darhol rangga bo'yaladi (mashq rejimida) yoki faqat belgilanadi (imtihon rejimida). Taymer: tepada o'ngda, oxirgi 2 daqiqada qizil rangga o'tadi. Ikonka (launcher): ko'k fonda oq rul va kichik tasdiq belgisi.
Ish bosqichlari
Loyiha skeleti, tema, navigatsiya, til almashtirish Room bazasi va JSON'dan yuklash (avval 40 ta namunaviy savol bilan) Savol ekrani: mashq va imtihon rejimlari Biletlar, mavzular, xatolar, saralanganlar Statistika va tayyorlik foizi AdMob (test ID) va UMP rozilik oynasi To'liq savollar bazasini qo'shish va tekshirish Play Console: ichki test, yopiq test, reliz
2-ilova: Abituriyent testlari
Ishchi nomi: "Abituriyent 189". Ilova kirish testini real ball tizimi bilan mashq qildiradi va abituriyentga "hozir necha ball olasiz" degan savolga javob beradi.

3 ta Android ilova uchun texnik topshiriq va dizayn

Real test formati (2026/2027 o'quv yili)

Blok
Majburiy
1-mutaxassislik fani 2-mutaxassislik fani Jami

Fanlar

Savollar

Ona tili, Matematika, O'zbekiston 3 × 10 =

tarixi

30

Yo'nalishga qarab

30

1 savol balli 1,1
3,1

Yo'nalishga qarab

30

2,1

5 fan

90

Maks. ball 33
93
63
189

Manbalar: Oliygoh.uz, Anhor.uz. Mezonlar o'tgan yilgi qabul bilan bir xil qoldirilgan. Test davomiyligi relizdan oldin rasmiy manbadan tekshirilib, taymerga kiritiladi (sozlamada o'zgaruvchi qilib qo'yiladi).
Kontent: eng katta ish
Ilova sifati savollar bazasiga bog'liq. BMBA test topshiriqlarini yoki boshqa nashrlarning testlarini nusxalash mumkin emas. Variantlar:
1. O'zimiz yozamiz: maktab darsliklari mavzulari bo'yicha, Claude yordamida savol yaratib, o'qituvchi tekshiruvidan o'tkazamiz.
2. Muallif bilan hamkorlik: test to'plami muallifidan litsenziya olish (daromaddan ulush evaziga).
MVP'da quyidagi fanlar bo'ladi (har bir to'plamda kamida 300 ta savol):

Bo'lim Majburiy fanlar (alohida rejim) 1-yo'nalish 2-yo'nalish
3-yo'nalish

Fanlar
Ona tili, Matematika, O'zbekiston tarixi
Matematika + Fizika
Ingliz tili + Ona tili va adabiyot
Kimyo + Biologiya

Izoh
30 savol, 33 ball; yo'nalish tanlamasdan ham ishlaydi
mutaxassislik darajasidagi savollar


3 ta Android ilova uchun texnik topshiriq va dizayn

Bo'lim Alohida fan

Fanlar Rus tili

Izoh
fan bo'yicha mashqda; juftligi keyin belgilanadi

Jami 10 ta savollar to'plami: majburiy darajadagi 3 ta va mutaxassislik darajasidagi 7 ta (Matematika va Ona tili ikki darajada alohida tuziladi).
Savollarni yangilash: savollar bazasi Firebase Storage'dagi JSON fayldan yuklanadi va ilova ichida saqlanadi. Yangi savol qo'shish uchun ilovani qayta chiqarish shart emas, versiya raqami o'zgarsa, ilova bazani yangilaydi.

Funksiyalar
MVP:
1. Yo'nalish tanlash: foydalanuvchi 3 ta yo'nalishdan birini tanlaydi yoki faqat majburiy fanlar rejimida ishlaydi; yo'nalishni istalgan vaqtda almashtirish mumkin.
2. To'liq simulyatsiya: 90 savol, 5 fan, real ball hisobi (1,1 / 3,1 / 2,1), umumiy ball 189 dan. 3. Fan bo'yicha mashq: bitta fandan 10, 20 yoki 30 savollik test. 4. Mavzu bo'yicha mashq: har fan mavzularga bo'lingan (masalan, Matematika →
Tenglamalar). 5. Kunlik test: har kuni 10 ta yangi savol, ketma-ket kunlar seriyasi (streak). 6. Natijalar tarixi: har simulyatsiya balli, ball o'zgarishi grafigi. 7. Xatolar ustida ishlash va saralanganlar (1-ilovadagi kod qayta ishlatiladi). 8. Ball kalkulyatori: to'g'ri javoblar sonini kiritib, umumiy ballni hisoblash.
2-versiya:
Barcha mutaxassislik fanlari. Formulalar uchun LaTeX ko'rsatish (KaTeX yoki MathView). Do'stlar bilan ball solishtirish (ulashish kartasi rasmi: "Men 152 ball oldim"). Imtihonga necha kun qolgani hisoblagichi.

Ekranlar

# Ekran

Asosiy elementlar

1

Onboarding 3 ta slayd, yo'nalish juftligini tanlash

Reklama yo'q


3 ta Android ilova uchun texnik topshiriq va dizayn

# Ekran

Asosiy elementlar

Reklama

2

Bosh sahifa Oxirgi ball (masalan 142/189), kunlik test

kartasi, streak, 3 ta tugma: Simulyatsiya,

Fanlar, Xatolarim

banner

3

Fanlar

5 fan kartasi, har birida progress va o'rtacha banner foiz

4

Mavzular

Mavzu ro'yxati va har biridagi to'g'ri javoblar banner foizi

5

Savol ekrani Fan nomi, savol raqami, matn/formula/rasm, yo'q

4 variant (A, B, C, D), taymer, savollar

xaritasi

6

Natija

Umumiy ball, fanlar bo'yicha ball jadvali, o'tgan safarga nisbatan o'zgarish

interstitial ekranga kirishda

7

Xatolar

tahlili

Javobingiz, to'g'ri javob, izoh

rewarded: izohni ochish

8

Tarix

Ball grafigi, simulyatsiyalar ro'yxati

banner

9

Sozlamalar Til, yo'nalish, tema, eslatma vaqti

yo'q

Ma'lumotlar modeli (Room)
Subject(id, nameUz, nameRu, block) (block: MANDATORY / SPECIAL)
Topic(id, subjectId, nameUz, nameRu) Question(id, subjectId, topicId, text, latex?, imageUrl?, explanation, difficulty) Option(id, questionId, label, text, isCorrect) Exam(id, type, subject1Id, subject2Id, startedAt, finishedAt, score)
ExamAnswer(examId, questionId, optionId, isCorrect, weight) (weight = 1.1 / 3.1 / 2.1)
DailyStreak(date, completed)
Ball hisoblash: score = Σ (isCorrect × weight) , bitta funksiyada va unit test bilan.


3 ta Android ilova uchun texnik topshiriq va dizayn
Dizayn
Asosiy rang: indigo #3949AB , urg'u rangi to'q sariq #FB8C00 (streak va mukofotlar uchun). Bosh sahifa: tepada katta raqam "142 / 189" va kichik o'sish ko'rsatkichi (+6), ostida kunlik test kartasi olov ikonkasi bilan. Fanlar: har fanning o'z rangi bor (Matematika ko'k, Tarix jigarrang, Ona tili yashil va h.k.), kartalar va grafiklarda shu rang ishlatiladi. Savol ekrani: pastda "savollar xaritasi" (1–90 raqamli kvadratchalar: javob berilgan, berilmagan, belgilangan), tepada fan nomi va taymer. Natija ekrani: konfetti animatsiyasi faqat shaxsiy rekord yangilanganda. Ikonka: indigo fonda oq bitiruvchi qalpoq va "189" raqami.
Ish bosqichlari
1-ilovadan umumiy modullarni ajratib olish (reklama, sozlamalar, savol ekrani) Fanlar, mavzular, savollar modeli va Firebase'dan yuklash Ball hisoblash funksiyasi va unit testlar Simulyatsiya, fan va mavzu bo'yicha mashq Kunlik test va streak, eslatma bildirishnomasi Natijalar tarixi va grafik Kontent: 10 ta to'plam × 300 savol, tekshiruv (avval majburiy fanlar, keyin yo'nalishlar) AdMob, Play Console, reliz (qabul mavsumidan 2–3 oy oldin)
3-ilova: Namoz vaqtlari, qibla va tasbeh
Ishchi nomi: "Prayer Times: Azan & Qibla" (har tilda o'z nomi: Namoz vaqti, Namaz Vakti, Waktu Sholat, ‫)مواقيت الصلاة‬. Global ilova: foydalanuvchi qayerda bo'lsa, o'sha joyning koordinatasi va mahalliy hisoblash usuli bo'yicha vaqtlarni ko'rsatadi, 12 tilda ishlaydi va internetsiz hisoblaydi.
Vaqtlarni hisoblash
Vaqtlar telefonning o'zida, internetsiz hisoblanadi: adhan-kotlin kutubxonasi. Kirish ma'lumotlari: koordinata, sana, vaqt mintaqasi, hisoblash usuli, Asr mazhabi. Hisoblash usuli davlatga qarab avtomatik tanlanadi (pastdagi jadval), foydalanuvchi sozlamada istalgan usulni qo'lda tanlay oladi.

3 ta Android ilova uchun texnik topshiriq va dizayn

Asr mazhabi: Shofi'iy (standart) yoki Hanafiy; Turkiya, Markaziy Osiyo, Janubiy Osiyo va Afg'onistonda Hanafiy avtomatik tanlanadi. Shimoliy kengliklar (Buyuk Britaniya, Skandinaviya, Rossiya shimoli): yozda Bomdod va Xufton aniqlanmasa, "tunning yettidan biri" yoki "tun o'rtasi" qoidasi qo'llanadi (sozlamada tanlanadi). O'zbekiston uchun maxsus parametrlar: O'zbekiston musulmonlari idorasi taqvimi bilan solishtirib sozlanadi, chunki rasmiy taqvim Toshkent bo'yicha chiqarilib, boshqa shaharlar uchun daqiqalardagi farqlar e'lon qilinadi (Uzum media). Qabul mezoni: sinov shaharlarida (Toshkent, Istanbul, Makka, Jakarta, Karachi, London, Nyu-York) bir yillik hisob mahalliy rasmiy taqvimdan 2 daqiqadan ortiq farq qilmasligi kerak. Buni unit testlar tekshiradi. Foydalanuvchi har bir vaqtga qo'lda ± daqiqa qo'sha oladi.
Davlat bo'yicha standart usul:

Hudud / davlat Saudiya Arabistoni, Yaman BAA Quvayt / Qatar Misr, Shimoliy va G'arbiy Afrika Pokiston, Hindiston, Bangladesh, Afg'oniston Turkiya, Bolqon O'zbekiston, Qozog'iston, Qirg'iziston, Tojikiston, Turkmaniston Indoneziya, Malayziya, Singapur, Bruney AQSh, Kanada Buyuk Britaniya Qolgan barcha davlatlar

Usul Umm al-Qura Dubai Kuwait / Qatar Egyptian Karachi
Diyanet parametrlari Maxsus (O'zbekiston taqvimiga moslangan) Singapore ISNA (North America) Moonsighting Committee Muslim World League

Asr Shofi'iy Shofi'iy Shofi'iy Shofi'iy Hanafiy
Hanafiy Hanafiy
Shofi'iy Shofi'iy Shofi'iy Shofi'iy

Jadval assets/methods.json faylida saqlanadi (davlat kodi → usul), uni yangilash uchun kod o'zgarmaydi.


3 ta Android ilova uchun texnik topshiriq va dizayn
Funksiyalar
MVP: 1. Bugungi vaqtlar: Bomdod, Quyosh, Peshin, Asr, Shom, Xufton va keyingi namozgacha qolgan vaqt (teskari sanoq). 2. Joylashuv: GPS orqali avtomatik yoki dunyo bo'yicha shahar qidirish (GeoNames ma'lumotlari asosida, aholisi 15 mingdan ortiq shaharlar, har birida vaqt mintaqasi). Bir nechta joyni saqlash mumkin (uy, ish, safar). Safarda joylashuv o'zgarganini sezib, "Vaqtlarni yangi joyga moslaymi?" deb so'raydi. GPS ruxsati ixtiyoriy. 3. Bildirishnomalar: har namoz uchun alohida yoqish/o'chirish, ovoz tanlash (azon, oddiy signal, jim), namozdan X daqiqa oldin eslatish. 4. Oylik taqvim: butun oy jadvali, Ramazonda saharlik va iftorlik ustunlari bilan. 5. Qibla kompasi: magnit sensor orqali Ka'ba yo'nalishi, kalibrlash ko'rsatmasi. 6. Tasbeh: katta tugma, tebranish, 33 / 99 / ixtiyoriy maqsad, zikrlar ro'yxati, kunlik hisob saqlanadi. 7. Ekran vidjeti: bosh ekranda bugungi vaqtlar va keyingi namoz (Jetpack Glance).
2-versiya: Hijriy taqvim va muhim kunlar (Ramazon, hayitlar). Ramazon rejimi: saharlik va iftorlik duolari, ro'za kunlari hisobi. Qazo namozlar hisobchisi. Wear OS vidjeti.
Bildirishnomalar: texnik talablar
AlarmManager.setExactAndAllowWhileIdle bilan aniq vaqtda ishga tushadi, har kuni yarim tunda keyingi kun rejalashtiriladi. Android 12+ da SCHEDULE_EXACT_ALARM ruxsati foydalanuvchidan so'raladi, Android 13+ da POST_NOTIFICATIONS . Telefon qayta yoqilganda ( BOOT_COMPLETED ) va vaqt mintaqasi o'zgarganda signallar qayta o'rnatiladi. Xiaomi, Huawei, Samsung kabi telefonlarda batareya tejash ilovani to'xtatib qo'yadi. Sozlamalarda "Bildirishnomalar kelmayaptimi?" yo'riqnomasi bo'ladi (avtoishga tushirish va batareya cheklovini o'chirish).

3 ta Android ilova uchun texnik topshiriq va dizayn

Tillar va lokalizatsiya
Ilova birinchi ochilganda telefon tilini oladi, sozlamada istalgan tilga almashtiriladi (Android 13+ da tizimning "App language" sozlamasi ham ishlaydi).

Til

Kod Yo'nalish

Inglizcha (standart) en chapdan o'ngga

Arabcha

ar o'ngdan chapga

Indonezcha

id chapdan o'ngga

Urducha

ur o'ngdan chapga

Bengalcha

bn chapdan o'ngga

Turkcha

tr chapdan o'ngga

Forscha

fa o'ngdan chapga

Malaycha

ms chapdan o'ngga

Fransuzcha

fr chapdan o'ngga

Ruscha

ru chapdan o'ngga

O'zbekcha

uz chapdan o'ngga

Nemischa

de chapdan o'ngga

RTL: arabcha, urducha va forscha uchun butun interfeys o'ngdan chapga aylanadi (Compose buni o'zi qiladi, lekin har bir ekran RTL'da tekshiriladi; kompas va strelkalar aylantirilmaydi). Raqamlar va sana: har til o'z formatida (arabcha raqamlar, 12/24 soatlik format telefon sozlamasiga qarab). Namoz nomlari: har tilda mahalliy nomi bilan (Bomdod / Fajr / Sabah / Subuh). Tarjima: Claude bilan qilinadi, har til uchun ona tili egasi tekshiradi (Fiverr yoki tanishlar orqali). Avval 5 til (en, ar, id, tr, ur), qolganlari 2-bosqichda. Play Store sahifasi ham har tilda alohida: nom, qisqa tavsif, skrinshotlar. Global ilovada yuklab olishlarning asosiy manbai shu (ASO). Hijriy sana: HijrahChronology (Umm al-Qura) asosida, sozlamada ±1 kun tuzatish.


3 ta Android ilova uchun texnik topshiriq va dizayn

Ekranlar

# Ekran

1

Onboarding

2

Bosh sahifa

3

Oylik taqvim

4

Qibla

5

Tasbeh

6

Bildirishnoma

sozlamalari

7

Sozlamalar

Asosiy elementlar
Til, joylashuv, avtomatik tanlangan hisoblash usuli (o'zgartirish mumkin), bildirishnoma ruxsati
Shahar va sana (milodiy + hijriy), katta teskari sanoq, 6 vaqtli ro'yxat (joriy namoz ajratilgan)
Jadval: sana, 6 vaqt, bugungi qator ajratilgan
Kompas, Ka'ba belgisi, gradus, aniqlik holati
Katta aylana tugma, hisob, maqsad, zikr nomi
Har namoz uchun kalit, ovoz, oldindan eslatish
Saqlangan joylar, til, hisoblash usuli, Asr mazhabi, shimoliy kenglik qoidasi, tema, vaqt tuzatishlari, yordam

Reklama yo'q
banner pastda
banner yo'q yo'q yo'q rewarded: "Mualliflarni qo'llab-quvvatlash" tugmasi

Reklama: alohida qoidalar
Bu diniy ilova, shuning uchun reklama boshqa ikkisidan ancha cheklangan bo'ladi:
Interstitial ishlatilmaydi. Faqat banner va ixtiyoriy rewarded. AdMob'da mos kelmaydigan toifalar bloklanadi: qimor, spirtli ichimliklar, tanishuv saytlari, astrologiya, sezgir mavzular (AdMob → Blocking controls). Qibla va tasbeh ekranlarida reklama yo'q. Daromad asosan foydalanuvchilar soni va kunlik ochilishlar hisobiga keladi.


3 ta Android ilova uchun texnik topshiriq va dizayn
Dizayn
Asosiy rang: zumrad yashil #00695C , urg'u rangi oltin #C9A227 . Bosh sahifa fonda kun vaqtiga qarab gradient: tong (moviy-pushti), kunduz (och moviy), shom (to'q sariq), tun (to'q ko'k). Bu ilovaga o'ziga xos ko'rinish beradi. Teskari sanoq: ekran markazida katta raqamlar "Asrgacha 01:24:10". Vaqtlar ro'yxati: har qatorda ikonka (quyosh holati), namoz nomi, vaqt, qo'ng'iroq belgisi (bildirishnoma yoqiq/o'chiq). Joriy namoz oltin rangli ramkada. Bezak: islomiy geometrik naqsh, juda och (5–8% shaffoflik) fon sifatida. Masjid yoki shaxs tasvirlari ishlatilmaydi. Shrift: raqamlar uchun tabular (teng kenglikdagi) raqamlar, arabcha matnlar uchun Amiri yoki Noto Naskh Arabic. Ikonka: zumrad fonda oltin yarim oy va soat milli.
Ish bosqichlari
adhan kutubxonasi bilan hisoblash va 7 ta sinov shahrining rasmiy taqvimlari bilan solishtirish testlari Shaharlar bazasi (GeoNames, vaqt mintaqalari), davlat bo'yicha usulni avtomatik tanlash Bosh sahifa, teskari sanoq, oylik taqvim Bildirishnomalar va qayta yuklashdan keyin tiklash Qibla va tasbeh Ekran vidjeti AdMob (faqat banner + rewarded, bloklangan toifalar) Tarjimalar (avval 5 til), RTL tekshiruvi, Play sahifasini har tilda tayyorlash, turli telefon brendlarida bildirishnoma testi, reliz (Ramazondan 1–2 oy oldin ideal)
Claude Code uchun reja va tayyor promptlar
Uchala ilova bitta Git repozitoriyda, umumiy modullar bilan quriladi. Birinchi bo'lib 3-ilova (Namoz vaqtlari) chiqariladi: unga savollar bazasi kerak emas, shuning uchun eng tez Play'ga chiqadi va umumiy modullar shu yerda sinovdan o'tadi.

3 ta Android ilova uchun texnik topshiriq va dizayn
Loyiha tuzilmasi
Umumiy kod core-* modullarida, har bir ilova esa alohida app-* modul: o'zgarish bir joyda qilinsa, uchala ilovaga ham tushadi.
modullar tuzilmasi · 3 ilova, 4 umumiy modul
Ilovalar bir-biriga bog'lanmaydi, faqat umumiy modullardan foydalanadi.
Chiqarish tartibi
1. Prayer Times (global): 1–2 hafta, kontent kerak emas. 2. Prava Test: 2 hafta + savollar bazasini tayyorlash vaqti. 3. Abituriyent 189: 2–3 hafta + kontent (eng ko'p vaqt oladi), qabul mavsumidan oldin
chiqariladi.
CLAUDE.md (repozitoriy ildiziga qo'yiladi)
# Loyiha: 3 ta Android ilova (Prayer Times, Prava Test, Abituriyent 189) ## Texnologiyalar Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, Room, DataStore, Navigation Compose, Google Mobile Ads (AdMob), UMP, Firebase Analytics/Crashlytics. minSdk 24, Gradle version catalog (libs.versions.toml). ## Modullar core-ui, core-ads, core-data, core-common, app-namoz, app-prava, appabituriyent.

3 ta Android ilova uchun texnik topshiriq va dizayn
Umumiy kod faqat core-* modullarida, ilovalar bir-biriga bog'lanmaydi.
## Qoidalar - Barcha matnlar strings.xml da (values = o'zbekcha lotin, values-ru = ruscha; app-namoz da values = inglizcha + 11 til, RTL qo'llab-quvvatlanadi). - Faqat AdMob TEST reklama ID lari ishlatilsin; haqiqiy ID lar local.properties dan olinadi. - Har bir ViewModel va hisoblash funksiyasi uchun unit test yozilsin. - Har bir bosqichdan keyin: ./gradlew assembleDebug testDebugUnitTest muvaffaqiyatli o'tishi shart. - Har bir bosqich alohida commit, aniq xabar bilan.
## Texnik topshiriq To'liq TZ: docs/TZ.md
Bu hujjatni Markdown sifatida eksport qilib, repozitoriyga docs/TZ.md nomi bilan qo'ying: Claude Code har safar unga murojaat qiladi.
Bosqichma-bosqich promptlar
Har bir promptni alohida beriladi, natija ishlagach keyingisiga o'tiladi. 0. Skelet:
docs/TZ.md va CLAUDE.md ni o'qi. Multi-modul Android loyiha skeletini yarat: core-ui (Material 3 tema, ranglar, shriftlar, umumiy komponentlar), core-ads, core-data, core-common va app-namoz moduli. app-namoz bo'sh bosh ekran bilan ishga tushsin. Build va testlar o'tishini tekshir, commit qil.
1. Reklama moduli:
core-ads modulini yoz: UMP rozilik oynasi, BannerAd composable, InterstitialManager (kamida 3 daqiqa oraliq, sessiyada ko'pi bilan 3 marta), RewardedManager, AppOpenManager (kuniga 1 marta). Faqat test ID lar. Reklama yuklanmasa ilova kutmasin. TZ dagi "Umumiy reklama siyosati" ga amal qil.
2. Namoz vaqtlari hisoblash:

3 ta Android ilova uchun texnik topshiriq va dizayn
TZ dagi 3-ilova "Vaqtlarni hisoblash" bo'limini bajar: adhan kutubxonasi, davlatga qarab usulni avtomatik tanlash (methods.json), Asr mazhabi, shimoliy kenglik qoidasi, GeoNames shaharlar bazasi. Toshkent, Istanbul, Makka, Jakarta, Karachi, London va Nyu-York uchun bir oylik vaqtlarni chiqarib ber, men rasmiy taqvim bilan solishtiraman.
3. Ekranlar:
TZ dagi 3-ilova ekranlarini Dizayn bo'limiga mos qilib yasa: bosh sahifa (kun vaqtiga qarab gradient, teskari sanoq), oylik taqvim, qibla, tasbeh, sozlamalar. Har bir ekran uchun Compose Preview bo'lsin.
4. Bildirishnomalar va vidjet:
TZ dagi "Bildirishnomalar: texnik talablar" ni bajar: aniq signallar, ruxsatlar, BOOT_COMPLETED dan keyin tiklash, ovoz tanlash. Keyin Glance vidjetini qo'sh.
5. Reliz tayyorlash:
Release build sozla: imzolash (keystore local.properties dan), R8/minify, versiya raqamlari, haqiqiy AdMob ID lar local.properties dan. Maxfiylik siyosati matnini o'zbek va rus tilida docs/privacy.md ga yoz.
Keyingi ilovalar uchun xuddi shu tartib: avval app-prava modulini yaratish, keyin TZ'dagi tegishli bo'lim bo'yicha bosqichma-bosqich.
Play'ga chiqarishdan oldingi tekshiruv
Haqiqiy reklama ID lari qo'yilgan, test ID lar olib tashlangan Maxfiylik siyosati URL manzili ishlaydi Data safety formasi to'ldirilgan (AdMob, Firebase) Kontent reytingi anketasi to'ldirilgan Skrinshotlar (kamida 4 ta), qisqa va to'liq tavsif o'zbek va rus tilida Ichki test, keyin Play Console talab qilgan yopiq test o'tkazilgan Kamida 3 xil brenddagi telefonda sinab ko'rilgan


```
