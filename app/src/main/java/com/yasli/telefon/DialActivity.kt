package com.yasli.telefon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Numara çevirme ekranı. Android, varsayılan telefon uygulamasında bunun olmasını şart koşuyor. */
class DialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ilk = intent?.data?.schemeSpecificPart ?: ""
        setContent { TusTakimi(ilk) }
    }

    @Composable
    private fun TusTakimi(ilk: String) {
        val ctx = LocalContext.current
        var numara by remember { mutableStateOf(ilk) }
        val tuslar = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        val sekil = RoundedCornerShape(18.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Renk.Zemin)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GeriBaslik("Numara çevir") { finish() }
            Text(
                text = numara.ifEmpty { " " },
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Renk.Murekkep,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tuslar.chunked(3).forEach { satir ->
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        satir.forEach { t ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(sekil)
                                    .background(Renk.Beyaz)
                                    .border(2.dp, Renk.Cizgi, sekil)
                                    .clickable { numara += t },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Renk.Murekkep)
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.height(96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BuyukDugme("SİL", Renk.Murekkep, modifier = Modifier.weight(1f), yukseklik = 96.dp, fontBoyu = 26.sp) {
                    numara = numara.dropLast(1)
                }
                BuyukDugme(
                    "ARA", Renk.Yesil, modifier = Modifier.weight(2f), yukseklik = 96.dp, fontBoyu = 32.sp,
                    ikon = R.drawable.ik_telefon
                ) {
                    if (numara.isNotBlank()) {
                        Veri.ara(ctx, numara)
                        finish()
                    }
                }
            }
        }
    }
}
