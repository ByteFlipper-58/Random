# --- Основные атрибуты (нужны для аннотаций/линий стека) ---
-keepattributes *Annotation*,InnerClasses,Signature
# (если хотите сохранять номера строк для удобного анализа крашей — раскомментируйте)
#-keepattributes SourceFile,LineNumberTable

# --- Kotlin metadata (нужно для корректной работы некоторых библиотек и рефлексии) ---
-keep class kotlin.Metadata { *; }

# --- Keep для Room: сущности, DAO и TypeConverters ---
# Мы держим пакеты с базой данных и сущностями чтобы KSP/Room-сгенерированные классы корректно работали после обфускации.
-keep class com.byteflipper.random.data.db.** { *; }
-keep class com.byteflipper.random.data.preset.** { *; }

# Сохраняем методы помеченные @TypeConverter (используются Room при преобразовании типов)
-keepclassmembers class * {
    @androidx.room.TypeConverter public *;
}

# --- Firebase & Google Play (анализ/крашлитикс/messaging/in-app, биллинг) ---
# Play Billing клиент использует reflection/интерфейсы — держим.
-keep class com.android.billingclient.** { *; }

# Firebase / Google Play сервисы — держим публичные API (предотвратить потенциальные проблемы при обфускации).
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Если у вас есть классы, наследующие FirebaseMessagingService / FirebaseMessagingReceiver — держим их сигнатуры
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }

# --- Сторонние небольшие библиотеки, используемые в проекте ---
# Splashscreen lib (net.kibotu) — держим, т.к. используется в MainActivity через API.
-keep class net.kibotu.splashscreen.** { *; }

# --- Утилиты/классы, используемые из XML/интентов (например, CustomTabs helper) ---
# Если у вас есть классы, которые вызываются из манифеста / через reflection, перечислите/держите их.
-keep class com.byteflipper.random.utils.** { *; }

# --- Общие правила безопасности для библиотек, использующих reflection ---
-dontwarn com.google.firebase.**
-dontwarn com.android.billingclient.**
-dontwarn com.google.android.gms.**
# (оставьте эти dontwarn, чтобы R8 не шумел по сторонним библиотекам)

# --- Опционально: оставить имена для удобства отладки (если нужно) ---
#-keepnames class com.byteflipper.random.**

# --- Примечание по Crashlytics (важно) ---
# При включённом minify/shrink: обязательно загружайте mapping.txt в Firebase Crashlytics для возможности корректной деобфускации отчетов об ошибках.
# (см. firebase setup / gradle task: uploadCrashlyticsMappingFile)