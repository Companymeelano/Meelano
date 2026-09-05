# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------- MeeLano

# The Xray core is reached entirely through reflection (see XrayCore), so R8
# cannot see any reference to these classes and would strip them. Losing them
# turns every connection into "class not found" at runtime.
-keep class libv2ray.** { *; }
-keep interface libv2ray.** { *; }
-dontwarn libv2ray.**

# gomobile generates a Go runtime bridge alongside the core.
-keep class go.** { *; }
-dontwarn go.**

# Config models are serialised to and from JSON by name; renaming their fields
# would silently produce configs the core rejects.
-keep class com.example.core.ProxyEndpoint { *; }
-keep class com.example.core.Protocol { *; }
-keep class com.example.data.model.** { *; }

# Keep line numbers so a user-reported stack trace remains actionable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
