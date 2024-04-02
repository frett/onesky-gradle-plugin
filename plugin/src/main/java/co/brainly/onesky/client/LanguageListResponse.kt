package co.brainly.onesky.client

data class LanguageListResponse(
    val data: List<Language>
)

data class Language(
    val code: String,
    val custom_locale: String?,
    val english_name: String,
    val is_base_language: Boolean,
    val translation_progress: String
) {
    val resolvedLocale get() = custom_locale ?: code
}
