package com.yasli.telefon

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** O anki aramayı uygulamanın her yerinden takip etmek için. */
object CallManager {
    val cagri = MutableStateFlow<Call?>(null)
    val durum = MutableStateFlow(Call.STATE_NEW)
    val hoparlor = MutableStateFlow(false)
    var servis: CallService? = null

    private val dinleyici = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (call == cagri.value) durum.value = state
            servis?.bildirimGoster()
        }
    }

    fun durumu(c: Call): Int =
        if (Build.VERSION.SDK_INT >= 31) c.details.state else @Suppress("DEPRECATION") c.state

    fun ayarla(c: Call?) {
        cagri.value?.unregisterCallback(dinleyici)
        cagri.value = c
        if (c != null) {
            c.registerCallback(dinleyici)
            durum.value = durumu(c)
        } else {
            durum.value = Call.STATE_DISCONNECTED
        }
    }

    fun cevapla() {
        cagri.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reddet() {
        cagri.value?.reject(false, null)
    }

    fun kapat() {
        cagri.value?.disconnect()
    }

    @Suppress("DEPRECATION")
    fun hoparlorDegistir() {
        val s = servis ?: return
        s.setAudioRoute(
            if (hoparlor.value) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
        )
    }
}

/** Telefon "varsayılan telefon uygulaması" olunca Android aramaları buraya bildirir. */
class CallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.servis = this
        CallManager.ayarla(call)
        bildirimGoster()
        ekraniAc()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (CallManager.cagri.value == call) {
            CallManager.ayarla(calls.lastOrNull { it != call })
        }
        if (CallManager.cagri.value == null) {
            NotificationManagerCompat.from(this).cancel(BILDIRIM_NO)
        } else {
            bildirimGoster()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.hoparlor.value = audioState?.route == CallAudioState.ROUTE_SPEAKER
    }

    override fun onDestroy() {
        if (CallManager.servis == this) CallManager.servis = null
        super.onDestroy()
    }

    private fun ekraniAc() {
        try {
            startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Bildirimdeki tam ekran görünüm devreye girer.
        }
    }

    @SuppressLint("MissingPermission")
    fun bildirimGoster() {
        val c = CallManager.cagri.value ?: return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL) == null) {
            val kanal = NotificationChannel(KANAL, "Aramalar", NotificationManager.IMPORTANCE_HIGH)
            kanal.setSound(null, null) // Zil sesini telefonun kendisi çalıyor
            nm.createNotificationChannel(kanal)
        }
        val ac = PendingIntent.getActivity(
            this, 0,
            Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val caliyor = CallManager.durumu(c) == Call.STATE_RINGING
        val b = NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ik_telefon)
            .setContentTitle(if (caliyor) "Gelen arama" else "Görüşme sürüyor")
            .setContentText("Açmak için dokunun")
            .setContentIntent(ac)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
        if (caliyor) b.setFullScreenIntent(ac, true)
        try {
            NotificationManagerCompat.from(this).notify(BILDIRIM_NO, b.build())
        } catch (e: SecurityException) {
        }
    }

    companion object {
        const val KANAL = "aramalar"
        const val BILDIRIM_NO = 1
    }
}
