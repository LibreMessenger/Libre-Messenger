-dontobfuscate

-keep class de.pixart.messenger.**
-keep class org.whispersystems.**
-keep class com.kyleduo.switchbutton.Configuration
-keep class com.soundcloud.android.crop.**
-keep class com.google.android.gms.**
-keep class org.openintents.openpgp.*
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn org.bouncycastle.mail.**
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.cert.dane.**
-dontwarn rocks.xmpp.addr.**
-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform