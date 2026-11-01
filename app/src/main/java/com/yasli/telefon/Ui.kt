package com.yasli.telefon

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

val TR: Locale = Locale("tr", "TR")

object Renk {
    val Zemin = Color(0xFFF4F1EA)
    val Murekkep = Color(0xFF1B1B1B)
    val Soluk = Color(0xFF4A453D)
    val Cizgi = Color(0xFFD6D0C4)
    val Kirmizi = Color(0xFFB3261E)
    val Yesil = Color(0xFF1E7B3A)
    val Sari = Color(0xFFE9B23A)
    val Beyaz = Color.White
    val avatarlar = listOf(
        Color(0xFF2F5DA8), Color(0xFFB5541C), Color(0xFF2E7D4F),
        Color(0xFF6B4FA0), Color(0xFF1F6F75), Color(0xFF7A5230)
    )
}

fun avatarRengi(isim: String): Color =
    Renk.avatarlar[(isim.hashCode() and 0x7fffffff) % Renk.avatarlar.size]

fun basHarf(isim: String): String =
    isim.trim().firstOrNull()?.toString()?.uppercase(TR) ?: "?"

/** Bir fotoğraf değişince tüm yuvarlak resimler yeniden yüklensin diye. */
val fotoSurumu = mutableIntStateOf(0)

/** Kişinin fotoğrafı varsa onu, yoksa renkli daire içinde baş harfini gösterir. */
@Composable
fun Avatar(isim: String, kisiId: Long?, boyut: Dp) {
    val ctx = LocalContext.current
    val foto by produceState<Bitmap?>(initialValue = null, kisiId, fotoSurumu.intValue) {
        value = if (kisiId != null) withContext(Dispatchers.IO) { Veri.kisiFotosu(ctx, kisiId) } else null
    }
    Box(
        modifier = Modifier.size(boyut).clip(CircleShape).background(avatarRengi(isim)),
        contentAlignment = Alignment.Center
    ) {
        val f = foto
        if (f != null) {
            Image(
                bitmap = f.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = basHarf(isim),
                color = Color.White,
                fontSize = (boyut.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Arama ekranındaki gibi kocaman düğme. */
@Composable
fun BuyukDugme(
    yazi: String,
    zemin: Color,
    modifier: Modifier = Modifier,
    yaziRengi: Color = Color.White,
    kenar: Color? = null,
    yukseklik: Dp = 112.dp,
    fontBoyu: TextUnit = 38.sp,
    ikon: Int? = null,
    tikla: () -> Unit
) {
    val sekil = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(yukseklik)
            .clip(sekil)
            .background(zemin)
            .then(if (kenar != null) Modifier.border(2.dp, kenar, sekil) else Modifier)
            .clickable(onClick = tikla),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (ikon != null) {
            Icon(
                painter = painterResource(ikon),
                contentDescription = null,
                tint = yaziRengi,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(text = yazi, color = yaziRengi, fontSize = fontBoyu, fontWeight = FontWeight.Bold)
    }
}

/** Üstte büyük GERİ düğmesi ve sayfa başlığı. */
@Composable
fun GeriBaslik(baslik: String, geri: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Renk.Murekkep)
                .clickable(onClick = geri)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("‹ GERİ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = baslik,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Renk.Murekkep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
