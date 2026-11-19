# PS Gamepad (Android -> PC Windows via Bluetooth)

HP Android jadi gamepad Bluetooth (HID) ala PS3, layar penuh landscape.

## Kontrol
- D-pad (8 arah), tombol aksi X / O / Kotak / Segitiga
- L1, R1, L2, R2 (L2/R2 juga mengisi axis trigger analog saat ditekan)
- Analog stick kiri dan kanan, plus tombol L3 / R3
- Select, Start, PS
- Opsi Getar (haptic) di bar atas, tombol Putus, dan sambung ulang otomatis ke PC terakhir

## Build APK
1. Install Android Studio (versi terbaru), lalu File > Open > pilih folder `PSGamepad`.
2. Tunggu Gradle sync selesai (butuh internet).
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Hasil: `app/build/outputs/apk/debug/app-debug.apk`, kirim ke HP dan install.

## Cara pakai
1. Buka app di HP, izinkan izin Bluetooth.
2. Tekan "Bisa ditemukan".
3. Di Windows: Settings > Bluetooth & devices > Add device > Bluetooth > pilih nama HP > Pair.
4. Jika status belum "Terhubung", tekan "Pilih PC" di app lalu pilih nama PC.
5. Tes di Windows: Win+R > `joy.cpl` > Properties (stick, trigger, tombol harus bergerak).

## Mapping HID (Windows)
- Tombol: 1=X, 2=O, 3=Kotak, 4=Segitiga, 5=L1, 6=R1, 7=L2, 8=R2, 9=Select, 10=Start, 11=L3, 12=R3, 13=PS
- Stick kiri = X/Y, stick kanan = Z/Rz (Z = kanan horizontal), trigger = Rx (L2) / Ry (R2), D-pad = POV hat

## Catatan
- Butuh Android 9 (API 28) ke atas. Sebagian HP (beberapa Samsung/Xiaomi/Huawei) tidak mendukung HID Device profile, jadi tidak bisa.
- Terdeteksi sebagai gamepad HID generik (DirectInput). Game yang hanya mendukung XInput: pakai Steam Input atau x360ce.
- Zona mati stick bisa diubah di `GamepadState.deadZone` (default 0.10).
- Belum ada: rumble dari game ke HP dan gyro (Sixaxis). Windows tidak mengirim rumble ke gamepad HID generik tanpa driver.

## Build lewat GitHub (tanpa PC / Android Studio)
1. Buat repo baru di GitHub, upload seluruh isi project ini (termasuk folder `.github`).
2. Buka tab Actions > "Build APK" > Run workflow (atau otomatis jalan saat push).
3. Tunggu sekitar 3-6 menit, buka run yang selesai, download artifact `PSGamepad-debug-apk`.
4. Ekstrak zip-nya, install `app-debug.apk` di HP (izinkan install dari sumber tidak dikenal).
