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
