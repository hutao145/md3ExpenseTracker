# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep data classes used in JSON parsing
-keep class com.example.expensetracker.data.remote.AutoAccountingService$SyncBill { *; }

# AutoTrack accessibility module
-keep class com.autotrack.** { *; }
-keep class com.example.expensetracker.autotrack.** { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-dontwarn com.autotrack.**
