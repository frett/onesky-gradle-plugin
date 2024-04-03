package co.brainly.onesky.client

import co.brainly.onesky.client.util.enqueueResponseWithFilesContent
import co.brainly.onesky.util.TimeProvider
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class OneSkyApiClientTest {

    private val server = MockWebServer()

    private val projectId = 41994
    private val client = OneSkyApiClient(
        "my-api-key",
        "my-api-secret",
        apiUrl = server.url("/").toString(),
        timeProvider = object : TimeProvider {
            override fun currentTimeMillis(): Long {
                return 12345L
            }
        }
    )

    @Test
    fun `downloads list of project's languages`() {
        server.enqueueResponseWithFilesContent("project_languages_response.json")

        val response = client.fetchProjectLanguages(projectId)

        val request = server.takeRequest()
        assertEquals(
            "/projects/41994/languages?api_key=my-api-key&timestamp=12&dev_hash=28dac32cc9ee8ab264d35087653be23e",
            request.path
        )

        val expected = listOf(
            Language("en", null, "English", true, "100.0%"),
            Language("fr", null, "French", false, "100.0%"),
            Language("ru", null, "Russian", false, "98.1%"),
            Language("tr", null, "Turkish", false, "100.0%"),
            Language("pl", null, "Polish", false, "100.0%"),
            Language("ro", null, "Romanian", false, "100.0%"),
            Language("kn-IN", null, "Kannada (India)", false, "0.0%"),
            Language("hi-IN", null, "Hindi (India)", false, "98.1%"),
            Language("ta-IN", null, "Tamil (India)", false, "0.0%"),
            Language("uk", null, "Ukrainian", false, "99.2%"),
            Language("te-IN", null, "Telugu (India)", false, "0.0%"),
            Language("id", null, "Indonesian", false, "99.8%"),
            Language("ms", null, "Malay", false, "0.0%"),
            Language("pa", null, "Punjabi", false, "0.1%"),
            Language("bn-IN", null, "Bengali (India)", false, "0.0%"),
            Language("ml-IN", null, "Malayalam (India)", false, "98.1%"),
            Language("pt-BR", null, "Portuguese (Brazil)", false, "99.9%"),
            Language("en-IN", null, "English (India)", false, "0.0%"),
            Language("mr", null, "Marathi", false, "96.9%"),
            Language("gu-IN", null, "Gujarati (India)", false, "0.0%"),
            Language("or-IN", null, "Oriya (India)", false, "100.0%"),
            Language("bn", null, "Bengali", false, "100.0%"),
            Language("ta", null, "Tamil", false, "100.0%"),
            Language("ur-IN", null, "Urdu (India)", false, "0.0%"),
            Language("en-US", null, "English (United States)", false, "0.0%"),
            Language("pa-IN", null, "Punjabi (India)", false, "93.8%"),
            Language("es-ES", null, "Spanish (Spain)", false, "100.0%"),
            Language("hi", "Hinglish LAT-IN", "Hindi", false, "98.1%"),
            Language("gu", null, "Gujarati", false, "96.6%"),
            Language("kn", null, "Kannada", false, "90.9%"),
            Language("ml", null, "Malayalam", false, "0.0%"),
            Language("or", null, "Oriya", false, "0.0%"),
            Language("te", null, "Telugu", false, "98.1%")
        )

        assertEquals(expected, response.getOrNull()?.data)
    }

    @Test
    fun `downloads a translation file`() {
        server.enqueueResponseWithFilesContent("example_translation_file.xml")

        val language = Language("fr", null, "French", false, "100.0%")
        val translationResult = client.fetchTranslation(projectId, "strings.xml", language)

        assertEquals(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <resources>
                    <!-- Name of the application -->
                    <string name="app_name">Nosdevoirs.fr</string>
                    <!-- Name of the shortcut for memory leaks info -->
                    <!-- error informing the user that he/she doesnt have ticket -->
                    <string name="error_task_view_no_ticket">Désolé, une erreur est survenue. Réessaye plus tard.
                    </string>
                    <!-- error informing the user that the question he opened was deleted -->
                    <string name="error_task_view_missing_task">La question a été supprimée</string>
                </resources>

            """.trimIndent(),
            translationResult.getOrNull()
        )
    }

    @Test
    fun `uploads a translation file`() {
        val file = File.createTempFile("onesky", ".tmp")
        file.writeText("Hello OneSky Gradle Plugin")

        client.uploadTranslation(projectId, file, deprecateStrings = false)

        val request = server.takeRequest()

        assertEquals(
            "/projects/41994/files?api_key=my-api-key&timestamp=12" +
                "&dev_hash=28dac32cc9ee8ab264d35087653be23e&file_format=ANDROID_XML&is_keeping_all_strings=true",
            request.path
        )

        assertEquals(
            "POST",
            request.method
        )

        assertEquals(
            """
            --onesky-gradle-plugin-file
            Content-Disposition: form-data; name="file"; filename="${file.name}"
            Content-Type: application/octet-stream
            Content-Length: 26

            Hello OneSky Gradle Plugin
            --onesky-gradle-plugin-file--

            """.trimIndent().replace(
                "\n",
                "\r\n"
            ), // to avoid conflicts with OkHttp
            request.body.readByteString().utf8()
        )
    }

    @Test
    fun `uploads a translation file and deprecate old strings`() {
        val file = File.createTempFile("onesky", ".tmp")
        file.writeText("Hello OneSky Gradle Plugin")

        client.uploadTranslation(projectId, file, deprecateStrings = true)

        val request = server.takeRequest()

        assertEquals(
            "/projects/41994/files?api_key=my-api-key&timestamp=12" +
                "&dev_hash=28dac32cc9ee8ab264d35087653be23e&file_format=ANDROID_XML&is_keeping_all_strings=false",
            request.path
        )

        assertEquals(
            "POST",
            request.method
        )

        assertEquals(
            """
            --onesky-gradle-plugin-file
            Content-Disposition: form-data; name="file"; filename="${file.name}"
            Content-Type: application/octet-stream
            Content-Length: 26

            Hello OneSky Gradle Plugin
            --onesky-gradle-plugin-file--

            """.trimIndent().replace(
                "\n",
                "\r\n"
            ), // to avoid conflicts with OkHttp
            request.body.readByteString().utf8()
        )
    }

    @Test
    fun `uploads a translation file with appended prefix`() {
        val file = File.createTempFile("onesky", ".tmp")
        file.writeText("Hello OneSky Gradle Plugin")

        client.uploadTranslation(projectId, file, deprecateStrings = false, fileNamePrefix = "my-feature")

        val request = server.takeRequest()

        assertEquals(
            "/projects/41994/files?api_key=my-api-key&timestamp=12" +
                "&dev_hash=28dac32cc9ee8ab264d35087653be23e&file_format=ANDROID_XML&is_keeping_all_strings=true",
            request.path
        )

        assertEquals(
            "POST",
            request.method
        )

        assertEquals(
            """
            --onesky-gradle-plugin-file
            Content-Disposition: form-data; name="file"; filename="my-feature-${file.name}"
            Content-Type: application/octet-stream
            Content-Length: 26

            Hello OneSky Gradle Plugin
            --onesky-gradle-plugin-file--

            """.trimIndent().replace(
                "\n",
                "\r\n"
            ), // to avoid conflicts with OkHttp
            request.body.readByteString().utf8()
        )
    }
}
