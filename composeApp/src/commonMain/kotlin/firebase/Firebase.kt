package firebase
import com.google.android.gms.tasks.Task

expect fun initializeFirebase()
expect suspend fun createUserWithEmailAndPassword(email: String, password: String): Task<com.google.firebase.auth.AuthResult>

expect suspend fun storeDirectoryInFirebaseStorage(sourcePath: String, destinationPath: String)

internal expect suspend fun loadDirectoryFromFirebaseStorage(sourcePath: String, destinationPath: String)

expect suspend fun uploadMap(useruid: String, mapName: String)
expect suspend fun downloadMap(useruid: String, mapName: String)
expect object FirebaseResourceManager

expect suspend fun listMaps():List<String>