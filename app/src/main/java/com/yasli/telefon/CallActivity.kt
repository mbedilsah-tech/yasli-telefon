package com.yasli.telefon

import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Gelen arama (Cevap ver / Reddet) ve konuşma (Hoparlör / Kapat) ekranı. */
class CallActivity : ComponentActivity() {

    private var yakinlikKilidi: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CagriEkrani() }
    }

    override fun onDestroy() {
        yakinlikAyarla(false)
        super.onDestroy()
    }

    /** Konuşurken telefon kulağa gelince ekranı kapatır, yanağın düğmelere basmasını önler. */
    private fun yakinlikAyarla(ac: Boolean) {
        val pm = getSystemService(PowerManager::class.java)
        if (ac) {
            if (yakinlikKilidi == null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                yakinlikKilidi = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "yasli:yakinlik")
                    .also { it.acquire(3 * 60 * 60 * 1000L) }
            }
        } else {
            yakinlikKilidi?.let { if (it.isHeld) it.release() }
            yakinlikKilidi = null
        }
    }

    @Composable
    private fun CagriEkrani() {
        val ctx = LocalContext.current
        val cagri by CallManager.cagri.collectAsState()
        val durum by CallManager.durum.collectAsState()
        val hoparlor by CallManager.hoparlor.collectAsState()

        LaunchedEffect(cagri) {
            if (cagri == null) {
                delay(900)
                finish()
            }
        }
        val konusmada = durum == Call.STATE_ACTIVE || durum == Call.STATE_DIALING || durum == Call.STATE_CONNECTING
        LaunchedEffect(konusmada, hoparlor) { yakinlikAyarla(konusmada && !hoparlor) }

        val numara = cagri?.details?.handle?.schemeSpecificPart ?: ""
        val kisi by produceState<Pair<Long, String>?>(initialValue = null, numara) {
            value = withContext(Dispatchers.IO) { Veri.numaradanKisi(ctx, numara) }
        }
        val simdi by produceState(System.currentTimeMillis()) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1000)
            }
        }
        val isim = kisi?.second ?: numara.ifBlank { "Gizli numara" }
        val baglanma = cagri?.details?.connectTimeMillis ?: 0L
        val altYazi = when (durum) {
            Call.STATE_RINGING -> "sizi arıyor"
            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW, Call.STATE_SELECT_PHONE_ACCOUNT -> "aranıyor…"
            Call.STATE_ACTIVE -> if (baglanma > 0) "Konuşuyor  " + sure((simdi - baglanma) / 1000) else "Konuşuyor"
            Call.STATE_HOLDING -> "Beklemede"
            else -> "Görüşme bitti"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Renk.Zemin)
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val caliyor = durum == Call.STATE_RINGING
            Avatar(isim, kisi?.first, if (caliyor) 130.dp else 200.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = isim.uppercase(TR),
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Renk.Murekkep,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Text(text = altYazi, fontSize = 30.sp, color = Renk.Soluk, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))

            if (caliyor) {
                // Tek dokunuş yeter, kaydırma yok. Cevap düğmesi bilerek en büyüğü.
                Spacer(Modifier.height(16.dp))
                BuyukDugme("CEVAP VER", Renk.Yesil, yukseklik = 190.dp, fontBoyu = 50.sp, ikon = R.drawable.ik_telefon) {
                    CallManager.cevapla()
                }
                Spacer(Modifier.height(20.dp))
                BuyukDugme("REDDET", Renk.Kirmizi, yukseklik = 140.dp, fontBoyu = 42.sp, ikon = R.drawable.ik_kapat) {
                    CallManager.reddet()
                }
            } else {
                BuyukDugme(
                    yazi = if (hoparlor) "HOPARLÖR AÇIK" else "HOPARLÖR",
                    zemin = if (hoparlor) Renk.Murekkep else Renk.Beyaz,
                    yaziRengi = if (hoparlor) Renk.Beyaz else Renk.Murekkep,
                    kenar = if (hoparlor) null else Renk.Cizgi,
                    yukseklik = 90.dp,
                    fontBoyu = 28.sp,
                    ikon = R.drawable.ik_hoparlor
                ) { CallManager.hoparlorDegistir() }
                Spacer(Modifier.height(18.dp))
                BuyukDugme("KAPAT", Renk.Kirmizi, yukseklik = 170.dp, fontBoyu = 48.sp, ikon = R.drawable.ik_kapat) {
                    CallManager.kapat()
                }
            }
        }
    }

    private fun sure(saniye: Long): String {
        val s = if (saniye < 0) 0 else saniye
        return "%d:%02d".format(s / 60, s % 60)
    }
}
