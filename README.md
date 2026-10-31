# Yaşlı Telefonu

Android telefonu yaşlılar için sadeleştiren ana ekran ve arama uygulaması.

- Ana ekranda saat, 6 hızlı arama kutusu, Kişiler, Son Çağrılar ve Fener var.
- Kişiye ya da kutuya bir kez dokununca araya menü girmeden arar.
- Gelen aramada kocaman **CEVAP VER** / **REDDET**, konuşurken **HOPARLÖR** / **KAPAT** düğmeleri çıkar.
- Ayarlar'a girmek için ana ekranın altındaki "Ayarlar" yazısına **3 saniye basılı tutulur**.

## APK nasıl alınır

Her `main` dalına yüklemede GitHub Actions uygulamayı derler. Hazır APK, deponun **Releases** bölümünde
`YasliTelefon.apk` adıyla durur.

## Telefona kurulum (Xiaomi Redmi Note 9 Pro)

1. Telefondan GitHub'a girip **Releases** bölümündeki son `YasliTelefon.apk` dosyasını indirin.
2. Dosyayı açın. "Bilinmeyen kaynaklardan yükleme" uyarısı gelirse izin verip kurun.
3. Uygulamayı açın, istenen tüm izinlere **İzin ver** deyin.
4. "Ayarlar" yazısına 3 saniye basılı tutun, **Kurulum** bölümündeki adımları sırayla yapın:
   1. İzinleri ver
   2. Varsayılan telefon uygulaması yap → "Yaşlı Telefonu"nu seçin
   3. Varsayılan ana ekran yap → "Yaşlı Telefonu"nu seçin
   4. Xiaomi özel izinleri → **Arka planda çalışırken açılır pencereleri göster** ve **Kilit ekranında göster**
      seçeneklerini açın. Bunlar kapalı olursa gelen arama ekranı görünmeyebilir.
5. Aynı yerden hızlı arama kutularına kişileri ekleyin.

Çift SIM varsa: telefonun kendi Ayarlar > SIM kartlar bölümünden aramalar için varsayılan SIM'i seçin.
Yoksa her aramada "hangi SIM" sorusu çıkar.

Yeni sürümler eskisinin üzerine kurulur, ayarlar silinmez.
