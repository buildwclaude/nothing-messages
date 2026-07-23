# Vendored AOSP MMS PDU library is reflection-free but keep it intact to be safe:
-keep class com.android.messaging.mmslib.** { *; }
-dontwarn com.android.messaging.**

# Keep Room entities and DAOs (KSP generates code referencing them)
-keep class com.buildwclaude.messages.data.db.** { *; }
