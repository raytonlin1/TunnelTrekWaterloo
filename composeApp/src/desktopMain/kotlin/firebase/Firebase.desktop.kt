package firebase

import android.app.Application
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebasePlatform
import com.google.firebase.auth.AuthResult
import com.google.firebase.storage.StorageReference
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.auth.auth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists

private val config = FirebaseOptions(
    applicationId = "1:803202435163:android:e677d6e2e660e0ece1177e",
    apiKey = "AIzaSyBfQGeLqdcvflwMuAWJh3NK3tLyYerA5UU",
    databaseUrl = null,
    storageBucket = "tunneltrek-e3c70.appspot.com",
    projectId = "tunneltrek-e3c70",
    gcmSenderId = "803202435163",
    authDomain = "tunneltrek-e3c70.firebaseapp.com"
)

actual fun initializeFirebase() {
    FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
        val storage = mutableMapOf<String, String>()
        override fun store(key: String, value: String) = storage.set(key, value)
        override fun retrieve(key: String) = storage[key]
        override fun clear(key: String) { storage.remove(key) }
        override fun log(msg: String) = println(msg)
    })

    Firebase.initialize( Application(), config)
}

actual suspend fun createUserWithEmailAndPassword(
    email: String,
    password: String
): Task<AuthResult> {
    return com.google.firebase.auth.FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
}

actual object FirebaseResourceManager{

}

actual suspend fun storeDirectoryInFirebaseStorage(sourcePath: String, destinationPath: String) {
    val uploader = FirebaseStorageClient( authToken = Firebase.auth.currentUser!!.getIdToken(false)!!)
    uploader.uploadDirectory(File(sourcePath), destinationPath)
}
class FirebaseStorageClient(
    private val authToken: String,
    bucketName: String = "tunneltrek-e3c70.appspot.com"
) {
    private val httpClient = OkHttpClient()
    private val baseUrl = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o"

    fun uploadDirectory(directory: File, firebasePath: String = "") {
        if (!directory.isDirectory) {
            throw IllegalArgumentException("The provided path is not a directory!")
        }

        directory.walk().forEach { file ->
            if (file.isFile) {
                val relativePath = file.relativeTo(directory).path.replace(File.separator, "/")
                val fullPath = if (firebasePath.isNotEmpty()) "$firebasePath/$relativePath" else relativePath
                uploadFile(file, fullPath)
            }
        }
    }

    private fun uploadFile(file: File, firebasePath: String) {
        val fileUrl = "$baseUrl?name=${firebasePath.urlEncode()}&uploadType=media"
        val request = Request.Builder()
            .url(fileUrl)
            .post(file.asRequestBody("application/octet-stream".toMediaType()))
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                println("Uploaded $file to Firebase Storage successfully.")
            }
        }
    }

    fun downloadDirectory(firebasePath: String, localDir: File) {
        val relativePath = firebasePath.substringAfter("/")
        val dir = localDir.resolve(relativePath)
        dir.mkdir()
        val encodedPath = firebasePath.urlEncode().replace("+", "%20")
        val url = "${baseUrl}?prefix=$encodedPath/&delimiter=/"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            val responseDecoded = parseItems(responseBody)

            responseDecoded.items.forEach { item ->
                downloadFile(item.name, localDir)
            }

            // recurse
            responseDecoded.prefixes.forEach {pfx ->
                downloadDirectory(pfx.dropLast(1), localDir)
            }
        }
    }

    private fun downloadFile(firebasePath: String, localDir: File) {
        val relativePath = firebasePath.substringAfter("/")
        val localFile = localDir.resolve(relativePath)
        localFile.createNewFile()
        val encodedPath = firebasePath.urlEncode().replace("+", "%20")
        val url = "$baseUrl/$encodedPath?alt=media"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val inputStream = response.body?.byteStream()
            inputStream?.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private fun parseItems(json: String?): DownloadResponse {
        return jsonParser.decodeFromString<DownloadResponse>(json!!)
    }

    suspend fun listMaps(firebasePath: String): List<String> {
        val encodedPath = firebasePath.urlEncode().replace("+", "%20")
        val url = "${baseUrl}?prefix=$encodedPath/&delimiter=/"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        val mapNames = mutableListOf<String>()
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            val responseDecoded = parseItems(responseBody)
            responseDecoded.prefixes.forEach { pfx ->
                mapNames.add(pfx.split("/").dropLast(1).last())
            }
        }
        return mapNames.toList()
    }

    @Serializable
    data class Item(val name: String)

    @Serializable
    data class DownloadResponse(val items: List<Item>, val prefixes: List<String>)

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}

actual suspend fun loadDirectoryFromFirebaseStorage(sourcePath: String, destinationPath: String) {
    val downloader = FirebaseStorageClient( authToken = Firebase.auth.currentUser!!.getIdToken(false)!!)
    downloader.downloadDirectory(sourcePath, File(destinationPath))
}

fun getBasePath(): String{
    val system = System.getProperty("os.name")
    return  when (system) {
        "Mac OS X" -> "${System.getProperty("user.home")}/Library/Application Support/org.example.project"
        "Linux" -> "${System.getProperty("user.home")}/.config/org.example.project"
        else -> {
            when {
                system.matches(Regex("Windows.*")) -> "${System.getenv("LOCALAPPDATA")}\\org.example.project"
                else -> {
                    throw UnsupportedOperationException("$system is not supported")
                }
            }
        }
    }
}

actual suspend fun downloadMap(useruid: String, mapName: String) {
    val basePath = getBasePath()
    loadDirectoryFromFirebaseStorage("shared/maps/$mapName",
        basePath)
}

actual suspend fun uploadMap(useruid: String, mapName: String) {
    val mapPath = Paths.get(getBasePath(), "maps", mapName)
    if (!mapPath.exists()) return
    storeDirectoryInFirebaseStorage(mapPath.toString(),
        "shared/maps/$mapName")
}

actual suspend fun listMaps(): List<String>{
    val client = FirebaseStorageClient( authToken = Firebase.auth.currentUser!!.getIdToken(false)!!)
    return client.listMaps("shared/maps")
}