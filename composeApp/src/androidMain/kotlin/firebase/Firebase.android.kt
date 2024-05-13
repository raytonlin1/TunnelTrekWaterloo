package firebase
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File

actual fun initializeFirebase(){}
actual suspend fun createUserWithEmailAndPassword(
    email: String,
    password: String
): Task<AuthResult> {

    return FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
}

actual object FirebaseResourceManager {
    var filesDir: File? = null
}

actual suspend fun storeDirectoryInFirebaseStorage(sourcePath: String, destinationPath: String) {
    val storage = FirebaseStorage.getInstance().reference
        val files = FirebaseResourceManager.filesDir!!.resolve(sourcePath).list()
        if (!files.isNullOrEmpty()) {
            for (file in files) {
                storeDirectoryInFirebaseStorage("$sourcePath/$file", "$destinationPath/$file")
            }
        } else {
            val inputStream = FirebaseResourceManager.filesDir!!.resolve(sourcePath).inputStream()
            val fileRef = storage.child(destinationPath)
            fileRef.putStream(inputStream).await()
            println("Uploaded ${sourcePath} to Firebase Storage successfully.")
        }
}

actual suspend fun loadDirectoryFromFirebaseStorage(sourcePath: String, destinationPath: String){
    val storage = FirebaseStorage.getInstance().reference
    val directory = storage.child(sourcePath)
    val destinationFile = FirebaseResourceManager.filesDir?.resolve(destinationPath)!!
    destinationFile.mkdir()
    downloadPath(directory, destinationFile)
}

private suspend fun downloadPathImpl(source: com.google.firebase.storage.StorageReference, destination: File, toWaitFor: MutableList<FileDownloadTask>) {
    source.listAll().await().let { listResult ->
        listResult.items.map { fileRef ->
            val localFile = destination.resolve(fileRef.name)
            localFile.createNewFile()
            toWaitFor.add(fileRef.getFile(localFile))
        }

        listResult.prefixes.forEach { subDirRef ->
            val subLocalDirectory = destination.resolve(subDirRef.name)
            if (!subLocalDirectory.exists()) {
                subLocalDirectory.mkdir()
            }

            downloadPathImpl(subDirRef, subLocalDirectory, toWaitFor)
        }
    }
}

suspend fun downloadPath(source: com.google.firebase.storage.StorageReference, destination: File) {
    val toWaitFor = mutableListOf<FileDownloadTask>()
    downloadPathImpl(source, destination, toWaitFor)
    toWaitFor.map { it.await() }
}

suspend fun listMaps(source: com.google.firebase.storage.StorageReference): List<String>{
    val mapNames = mutableListOf<String>()
    source.listAll().await().let { listResult ->
        listResult.prefixes.forEach { ref ->
            mapNames.add(ref.name)
        }
    }
    return mapNames.toList()
}

actual suspend fun uploadMap(useruid: String, mapName: String) {
    val source = "maps/$mapName"
    if (!FirebaseResourceManager.filesDir!!.resolve(source).exists()) return
    storeDirectoryInFirebaseStorage(source, "shared/maps/$mapName")
}

actual suspend fun downloadMap(useruid: String, mapName: String) {
    FirebaseResourceManager.filesDir?.resolve("maps")!!.mkdir()
    loadDirectoryFromFirebaseStorage("shared/maps/$mapName",
        "maps/$mapName")
}

actual suspend fun listMaps(): List<String> {
    return listMaps(FirebaseStorage.getInstance().reference.child("shared/maps"))
}