# FileProvider authority and URI perms must survive minify
-keep class androidx.core.content.FileProvider { *; }
-keep public class * extends androidx.core.content.FileProvider
-keep class **.R$xml { *; }

# (Optional) keep our PDF/data classes
-keep class com.example.proofmark.core.pdf.** { *; }
