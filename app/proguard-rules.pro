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
# Mantem as data classes usadas pelo Firestore com toObject()/toObjects()
-keep class com.imobiliario.aluno.ui.notificacoes.NotificacaoNota { *; }
-keep class com.imobiliario.aluno.ui.notificacoes.TipoNotificacao { *; }

# Regra geral: protege qualquer modelo anotado com @PropertyName no
# futuro, alem de preservar as anotacoes necessarias em runtime
-keepclassmembers class com.imobiliario.aluno.** {
  @com.google.firebase.firestore.PropertyName <fields>;
  @com.google.firebase.firestore.PropertyName <methods>;
}
-keepattributes *Annotation*
