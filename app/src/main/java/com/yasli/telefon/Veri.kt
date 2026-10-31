package com.yasli.telefon

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone as P
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object Veri {

    data class Kisi(val id: Long, val isim: String, val numara: String)
    data class Cagri(val isim: String?, val numara: String, val tur: Int, val tarih: Long)
    data class Hizli(val etiket: String, val numara: String, val kisiId: Long?)

    const val KUTU_SAYISI = 6

    fun izinVar(ctx: Context, izin: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, izin) == PackageManager.PERMISSION_GRANTED

    // ---------- Rehber ----------

    fun kisiler(ctx: Context): List<Kisi> {
        if (!izinVar(ctx, Manifest.permission.READ_CONTACTS)) return emptyList()
        val sonuc = LinkedHashMap<Long, Kisi>()
        val alanlar = arrayOf(P.CONTACT_ID, P.DISPLAY_NAME_PRIMARY, P.NUMBER, P.IS_SUPER_PRIMARY)
        try {
            ctx.contentResolver.query(P.CONTENT_URI, alanlar, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val isim = c.getString(1) ?: continue
                    val numara = c.getString(2) ?: continue
                    val birincil = c.getInt(3) == 1
                    if (!sonuc.containsKey(id) || birincil) sonuc[id] = Kisi(id, isim, numara)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        val siralayici = Collator.getInstance(TR)
        return sonuc.values.sortedWith(Comparator { a, b -> siralayici.compare(a.isim, b.isim) })
    }

    fun kisiFotosu(ctx: Context, kisiId: Long): Bitmap? = try {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, kisiId)
        ContactsContract.Contacts.openContactPhotoInputStream(ctx.contentResolver, uri, true)
            ?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    /** Numaradan rehberdeki kişiyi bulur: (kişi no, isim) */
    fun numaradanKisi(ctx: Context, numara: String): Pair<Long, String>? {
        if (numara.isBlank() || !izinVar(ctx, Manifest.permission.READ_CONTACTS)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numara))
            ctx.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) Pair(c.getLong(0), c.getString(1) ?: numara) else null }
        } catch (e: Exception) {
            null
        }
    }

    /** Telefon hafızasına yeni kişi kaydeder. Başarılıysa true döner. */
    fun kisiEkle(ctx: Context, isim: String, numara: String): Boolean {
        if (!izinVar(ctx, Manifest.permission.WRITE_CONTACTS)) return false
        val islemler = arrayListOf(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, isim)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, P.CONTENT_ITEM_TYPE)
                .withValue(P.NUMBER, numara)
                .withValue(P.TYPE, P.TYPE_MOBILE)
                .build()
        )
        return try {
            ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, islemler)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---------- Son çağrılar ----------

    fun sonCagrilar(ctx: Context): List<Cagri> {
        if (!izinVar(ctx, Manifest.permission.READ_CALL_LOG)) return emptyList()
        val liste = ArrayList<Cagri>()
        try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                while (c.moveToNext() && liste.size < 50) {
                    liste.add(
                        Cagri(
                            isim = c.getString(1)?.takeIf { it.isNotBlank() },
                            numara = c.getString(0) ?: "",
                            tur = c.getInt(2),
                            tarih = c.getLong(3)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return liste
    }

    fun turYazisi(tur: Int): String = when (tur) {
        CallLog.Calls.INCOMING_TYPE -> "Sizi aradı"
        CallLog.Calls.OUTGOING_TYPE -> "Siz aradınız"
        CallLog.Calls.MISSED_TYPE -> "Cevapsız"
        CallLog.Calls.REJECTED_TYPE -> "Reddedildi"
        else -> "Arama"
    }

    fun cevapsizMi(tur: Int) = tur == CallLog.Calls.MISSED_TYPE || tur == CallLog.Calls.REJECTED_TYPE

    fun zamanYazisi(zaman: Long): String {
        val o = Calendar.getInstance().apply { timeInMillis = zaman }
        val bugun = Calendar.getInstance()
        val dun = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val saat = SimpleDateFormat("HH:mm", TR).format(Date(zaman))
        fun ayniGun(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            ayniGun(o, bugun) -> "Bugün $saat"
            ayniGun(o, dun) -> "Dün $saat"
            else -> SimpleDateFormat("d MMMM HH:mm", TR).format(Date(zaman))
        }
    }

    // ---------- Hızlı arama kutuları ----------

    private fun tercihler(ctx: Context) = ctx.getSharedPreferences("hizli_arama", Context.MODE_PRIVATE)

    fun hizliOku(ctx: Context): List<Hizli?> = (0 until KUTU_SAYISI).map { i ->
        tercihler(ctx).getString("kutu$i", null)?.split("|")?.let { p ->
            if (p.size >= 2) Hizli(p[0], p[1], p.getOrNull(2)?.toLongOrNull()) else null
        }
    }

    fun hizliYaz(ctx: Context, i: Int, h: Hizli?) {
        tercihler(ctx).edit {
            if (h == null) remove("kutu$i")
            else putString("kutu$i", "${h.etiket.replace("|", " ")}|${h.numara}|${h.kisiId ?: ""}")
        }
    }

    // ---------- Arama şekli ----------

    /** true: dokununca hemen arar. false: önce ARA / KAPAT ekranı çıkar. */
    fun tekDokunus(ctx: Context): Boolean = tercihler(ctx).getBoolean("tek_dokunus", true)

    fun tekDokunusYaz(ctx: Context, deger: Boolean) {
        tercihler(ctx).edit { putBoolean("tek_dokunus", deger) }
    }

    // ---------- Arama ----------

    @SuppressLint("MissingPermission")
    fun ara(ctx: Context, numara: String) {
        if (numara.isBlank()) return
        if (!izinVar(ctx, Manifest.permission.CALL_PHONE)) {
            Toast.makeText(ctx, "Arama izni yok. Ayarlar'dan izinleri verin.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val tm = ctx.getSystemService(TelecomManager::class.java)
            tm.placeCall(Uri.fromParts("tel", numara, null), Bundle())
        } catch (e: Exception) {
            Toast.makeText(ctx, "Arama yapılamadı", Toast.LENGTH_LONG).show()
        }
    }
}

/** Telefonun arkasındaki flaşı fener olarak açıp kapatır. */
object Fener {
    fun ayarla(ctx: Context, ac: Boolean): Boolean = try {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        if (id == null) false else {
            cm.setTorchMode(id, ac)
            true
        }
    } catch (e: Exception) {
        false
    }
}
