# ProGuard rules for Compose Call Guard

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep data entities
-keep class com.example.phoneblocker.data.local.entity.** { *; }

# Keep CallScreeningService
-keep public class com.example.phoneblocker.service.CallScreeningServiceImpl { *; }
