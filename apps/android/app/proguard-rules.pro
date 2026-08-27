# Add project specific ProGuard rules here.

# Os DTOs Retrofit são materializados por reflexão pelo Gson. Nenhum deles contém segredo.
-keep class com.workcontrol.app.data.remote.dto.** { *; }
