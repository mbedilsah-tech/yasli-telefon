package com.yasli.telefon

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

/** Ana ekran. Telefon açılınca hep bu gelir (varsayılan ana ekran yapılınca). */
class MainActivity : ComponentActivity() {

    // Hangi sayfadayız: "ana", "kisiler", "son", "ayarlar", "sec0".."sec5"
    private val ekran = mutableStateOf("ana")
    // Geri gelince listeleri tazelemek için sayaç
    private val yenile = mutableIntStateOf(0)

    private val izinIstegi =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { yenile.intValue++ }
    private val rolIstegi =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { yenile.intValue++ }

    private fun gerekliIzinler(): Array<String> {
        val l = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
        return l.toTypedArray()
    }

    private fun eksikIzinVar() = gerekliIzinler().any { !Veri.izinVar(this, it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Uygulama() }
        if (eksikIzinVar()) izinIstegi.launch(gerekliIzinler())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ekran.value = "ana" // Ana ekran tuşuna basınca hep başa dön
    }

    override fun onResume() {
        super.onResume()
        yenile.intValue++
    }

    private fun varsayilanTelefonMu(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm.isRoleAvailable(RoleManager.ROLE_DIALER) && rm.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun varsayilanTelefonYap() {
        val rm = getSystemService(RoleManager::class.java)
        if (!rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, "Bu telefonda desteklenmiyor", Toast.LENGTH_LONG).show()
        } else if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
            Toast.makeText(this, "Zaten varsayılan telefon uygulaması", Toast.LENGTH_LONG).show()
        } else {
            rolIstegi.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    private fun ayarAc(eylem: String) {
        try {
            startActivity(Intent(eylem))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Xiaomi'de "arka planda pencere açma" gibi izinlerin olduğu sayfa. */
    private fun uygulamaIzinleri() {
        val miui = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", packageName)
        try {
            startActivity(miui)
        } catch (e: Exception) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }
    }

    @Composable
    private fun Uygulama() {
        val y = yenile.intValue
        BackHandler {
            ekran.value = if (ekran.value.startsWith("sec")) "ayarlar" else "ana"
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Renk.Zemin)
                .systemBarsPadding()
        ) {
            val e = ekran.value
            when {
                e == "kisiler" -> KisilerEkrani(y, secimKutusu = null)
                e == "son" -> SonCagrilarEkrani(y)
                e == "ayarlar" -> AyarlarEkrani(y)
                e.startsWith("sec") -> KisilerEkrani(y, secimKutusu = e.removePrefix("sec").toIntOrNull() ?: 0)
                else -> AnaEkran(y)
            }
        }
    }

    // ================= ANA EKRAN =================

    @Composable
    private fun AnaEkran(y: Int) {
        val ctx = LocalContext.current
        val hizli = remember(y) { Veri.hizliOku(ctx) }
        val aktifCagri by CallManager.cagri.collectAsState()
        var fenerAcik by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Saat()
            if (aktifCagri != null) {
                BuyukDugme("GÖRÜŞMEYE DÖN", Renk.Yesil, yukseklik = 72.dp, fontBoyu = 26.sp, ikon = R.drawable.ik_telefon) {
                    startActivity(Intent(ctx, CallActivity::class.java))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (satir in 0 until 3) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (sutun in 0 until 2) {
                            HizliKutu(hizli[satir * 2 + sutun], Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            Row(modifier = Modifier.height(78.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuDugmesi("KİŞİLER", R.drawable.ik_kisi, Renk.Murekkep, Renk.Beyaz, Modifier.weight(1f)) {
                    ekran.value = "kisiler"
                }
                MenuDugmesi("SON ÇAĞRILAR", R.drawable.ik_gecmis, Renk.Murekkep, Renk.Beyaz, Modifier.weight(1f)) {
                    ekran.value = "son"
                }
            }
            Row(modifier = Modifier.height(78.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuDugmesi("NUMARA ÇEVİR", R.drawable.ik_tuslar, Renk.Yesil, Renk.Beyaz, Modifier.weight(1f)) {
                    startActivity(Intent(ctx, DialActivity::class.java))
                }
                MenuDugmesi(
                    if (fenerAcik) "FENER AÇIK" else "FENER",
                    R.drawable.ik_fener,
                    if (fenerAcik) Color(0xFFFFD54F) else Renk.Sari,
                    Renk.Murekkep,
                    Modifier.weight(1f)
                ) {
                    val yeni = !fenerAcik
                    if (Fener.ayarla(ctx, yeni)) fenerAcik = yeni
                    else Toast.makeText(ctx, "Fener açılamadı", Toast.LENGTH_SHORT).show()
                }
            }
            AyarDugmesi { ekran.value = "ayarlar" }
        }
    }

    @Composable
    private fun Saat() {
        val simdi by produceState(System.currentTimeMillis()) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1000)
            }
        }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("HH:mm", TR).format(Date(simdi)),
                fontSize = 68.sp, fontWeight = FontWeight.Bold, color = Renk.Murekkep,
                style = TextStyle(lineHeight = 72.sp)
            )
            Text(
                SimpleDateFormat("EEEE, d MMMM", TR).format(Date(simdi)),
                fontSize = 22.sp, color = Renk.Soluk
            )
        }
    }

    @Composable
    private fun HizliKutu(h: Veri.Hizli?, modifier: Modifier) {
        val ctx = LocalContext.current
        val sekil = RoundedCornerShape(20.dp)
        Column(
            modifier = modifier
                .clip(sekil)
                .background(Renk.Beyaz)
                .border(2.dp, Renk.Cizgi, sekil)
                .clickable(enabled = h != null) { if (h != null) Veri.ara(ctx, h.numara) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (h == null) {
                Text("Boş", fontSize = 22.sp, color = Renk.Soluk)
            } else {
                Avatar(h.etiket, h.kisiId, 72.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = h.etiket.uppercase(TR),
                    fontSize = if (h.etiket.length > 9) 18.sp else 24.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Renk.Murekkep,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }

    @Composable
    private fun MenuDugmesi(
        yazi: String, ikon: Int, zemin: Color, yaziRengi: Color, modifier: Modifier, tikla: () -> Unit
    ) {
        Row(
            modifier = modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(zemin)
                .clickable(onClick = tikla)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(ikon), contentDescription = null, tint = yaziRengi, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                yazi, color = yaziRengi, fontSize = 19.sp, lineHeight = 21.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }

    /** Yaşlı kişi yanlışlıkla açmasın diye 3 saniye basılı tutunca açılır. */
    @Composable
    private fun AyarDugmesi(ac: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        var birakildi = false
                        withTimeoutOrNull(3000L) {
                            waitForUpOrCancellation()
                            birakildi = true
                        }
                        if (!birakildi) ac()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Renk.Soluk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ayarlar (3 saniye basılı tutun)", fontSize = 16.sp, color = Renk.Soluk)
            }
        }
    }

    // ================= KİŞİLER =================

    @Composable
    private fun KisilerEkrani(y: Int, secimKutusu: Int?) {
        val ctx = LocalContext.current
        val liste by produceState(initialValue = emptyList<Veri.Kisi>(), y) {
            value = withContext(Dispatchers.IO) { Veri.kisiler(ctx) }
        }
        var secilen by remember { mutableStateOf<Veri.Kisi?>(null) }
        var aranan by remember { mutableStateOf("") }
        val sekil = RoundedCornerShape(18.dp)

        // İsimde ya da numarada geçen her kişiyi göster (Türkçe harflere uygun)
        val arananKucuk = aranan.trim().lowercase(TR)
        val arananRakam = aranan.filter { it.isDigit() }
        val gorunen = if (arananKucuk.isEmpty()) liste else liste.filter { k ->
            k.isim.lowercase(TR).contains(arananKucuk) ||
                (arananRakam.length >= 3 && k.numara.filter { it.isDigit() }.contains(arananRakam))
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            GeriBaslik(if (secimKutusu == null) "Kişiler" else "Kutu ${secimKutusu + 1} için seçin") {
                ekran.value = if (secimKutusu == null) "ana" else "ayarlar"
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = aranan,
                onValueChange = { aranan = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 24.sp, color = Renk.Murekkep),
                placeholder = { Text("İsim yazarak arayın…", fontSize = 22.sp, color = Renk.Soluk) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Renk.Murekkep, modifier = Modifier.size(30.dp))
                },
                trailingIcon = {
                    if (aranan.isNotEmpty()) {
                        Box(
                            modifier = Modifier.size(48.dp).clickable { aranan = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Temizle", tint = Renk.Murekkep, modifier = Modifier.size(30.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Renk.Beyaz,
                    unfocusedContainerColor = Renk.Beyaz,
                    focusedBorderColor = Renk.Murekkep,
                    unfocusedBorderColor = Renk.Cizgi
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (liste.isEmpty()) {
                Text("Rehberde kimse yok ya da izin verilmedi.", fontSize = 22.sp, color = Renk.Soluk)
            } else if (gorunen.isEmpty()) {
                Text("\"$aranan\" bulunamadı.", fontSize = 22.sp, color = Renk.Soluk)
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(gorunen, key = { it.id }) { k ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(sekil)
                            .background(Renk.Beyaz)
                            .border(2.dp, Renk.Cizgi, sekil)
                            .clickable {
                                if (secimKutusu == null) Veri.ara(ctx, k.numara) else secilen = k
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(k.isim, k.id, 56.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            k.isim, fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                            color = Renk.Murekkep, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (secimKutusu == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "İsme dokunun, hemen arar.", fontSize = 19.sp, color = Renk.Soluk,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val k = secilen
        if (k != null && secimKutusu != null) {
            var etiket by remember(k) { mutableStateOf(k.isim.split(" ").first()) }
            AlertDialog(
                onDismissRequest = { secilen = null },
                title = { Text("Kutuda yazacak isim") },
                text = {
                    Column {
                        Text("Kısa olsun, örneğin: KIZIM, OĞLUM, DOKTOR", fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = etiket, onValueChange = { etiket = it.take(14) }, singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        Veri.hizliYaz(ctx, secimKutusu, Veri.Hizli(etiket.ifBlank { k.isim }, k.numara, k.id))
                        secilen = null
                        ekran.value = "ayarlar"
                    }) { Text("Kaydet") }
                },
                dismissButton = { TextButton(onClick = { secilen = null }) { Text("Vazgeç") } }
            )
        }
    }

    // ================= SON ÇAĞRILAR =================

    @Composable
    private fun SonCagrilarEkrani(y: Int) {
        val ctx = LocalContext.current
        val liste by produceState(initialValue = emptyList<Veri.Cagri>(), y) {
            value = withContext(Dispatchers.IO) { Veri.sonCagrilar(ctx) }
        }
        val sekil = RoundedCornerShape(18.dp)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            GeriBaslik("Son çağrılar") { ekran.value = "ana" }
            Spacer(Modifier.height(12.dp))
            if (liste.isEmpty()) {
                Text("Henüz arama yok.", fontSize = 22.sp, color = Renk.Soluk)
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(liste) { c ->
                    val cevapsiz = Veri.cevapsizMi(c.tur)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(sekil)
                            .background(Renk.Beyaz)
                            .border(2.dp, if (cevapsiz) Renk.Kirmizi else Renk.Cizgi, sekil)
                            .clickable(enabled = c.numara.isNotBlank()) { Veri.ara(ctx, c.numara) }
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            c.isim ?: c.numara.ifBlank { "Gizli numara" },
                            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Renk.Murekkep,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${Veri.turYazisi(c.tur)} · ${Veri.zamanYazisi(c.tarih)}",
                            fontSize = 20.sp,
                            color = if (cevapsiz) Renk.Kirmizi else Renk.Soluk,
                            fontWeight = if (cevapsiz) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Geri aramak için dokunun", fontSize = 19.sp, color = Renk.Soluk,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // ================= AYARLAR (yakınlar için) =================

    @Composable
    private fun AyarlarEkrani(y: Int) {
        val ctx = LocalContext.current
        val hizli = remember(y) { Veri.hizliOku(ctx) }
        val izinTamam = remember(y) { !eksikIzinVar() }
        val telefonTamam = remember(y) { varsayilanTelefonMu() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GeriBaslik("Ayarlar") { ekran.value = "ana" }

            Bolum("Hızlı arama kutuları")
            for (i in 0 until Veri.KUTU_SAYISI) {
                val h = hizli[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Renk.Beyaz)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${i + 1}. ${h?.etiket ?: "Boş"}", fontSize = 20.sp, color = Renk.Murekkep,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    KucukDugme("Değiştir", Renk.Murekkep) { ekran.value = "sec$i" }
                    if (h != null) {
                        Spacer(Modifier.width(8.dp))
                        KucukDugme("Sil", Renk.Kirmizi) {
                            Veri.hizliYaz(ctx, i, null)
                            yenile.intValue++
                        }
                    }
                }
            }

            Bolum("Kurulum")
            AyarSatiri("1. İzinleri ver", if (izinTamam) "Tamam" else "Eksik") { izinIstegi.launch(gerekliIzinler()) }
            AyarSatiri("2. Varsayılan telefon uygulaması yap", if (telefonTamam) "Tamam" else "Yapılmadı") {
                varsayilanTelefonYap()
            }
            AyarSatiri("3. Varsayılan ana ekran yap", null) { ayarAc(Settings.ACTION_HOME_SETTINGS) }
            AyarSatiri("4. Xiaomi özel izinleri", "Arka planda pencere açma vb.") { uygulamaIzinleri() }

            Bolum("Telefon ayarları")
            AyarSatiri("Ses", null) { ayarAc(Settings.ACTION_SOUND_SETTINGS) }
            AyarSatiri("Ekran ve parlaklık", null) { ayarAc(Settings.ACTION_DISPLAY_SETTINGS) }
            AyarSatiri("Tuş takımı (numara çevir)", null) { startActivity(Intent(ctx, DialActivity::class.java)) }
            AyarSatiri("Telefonun tüm ayarları", null) { ayarAc(Settings.ACTION_SETTINGS) }
            Spacer(Modifier.height(20.dp))
        }
    }

    @Composable
    private fun Bolum(baslik: String) {
        Text(
            baslik, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Renk.Murekkep,
            modifier = Modifier.padding(top = 12.dp)
        )
    }

    @Composable
    private fun KucukDugme(yazi: String, zemin: Color, tikla: () -> Unit) {
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(zemin)
                .clickable(onClick = tikla)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(yazi, color = Renk.Beyaz, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun AyarSatiri(baslik: String, alt: String?, tikla: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Renk.Beyaz)
                .clickable(onClick = tikla)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(baslik, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Renk.Murekkep)
            if (alt != null) Text(alt, fontSize = 16.sp, color = Renk.Soluk)
        }
    }
}
