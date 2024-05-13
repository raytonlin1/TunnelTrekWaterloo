import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.auth
import firebase.storeDirectoryInFirebaseStorage
import kotlinx.coroutines.launch


class HomeScreen: Screen {
    init {
        firebase.initializeFirebase()
    }

    enum class AuthenticationMode {
        SigningIn,
        CreatingAccount
    }
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        if (Firebase.auth.currentUser != null) {
            navigator.push(MapListScreen())
            return
        }

        var authenticationMode by remember { mutableStateOf(AuthenticationMode.SigningIn) }

        var usernameField by remember { mutableStateOf("") }
        var passwordField by remember { mutableStateOf("") }
        var passwordConfirmationField by remember { mutableStateOf("") }

        var errorMessage by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()

        val title = when (authenticationMode) {
            AuthenticationMode.SigningIn -> "Login"
            AuthenticationMode.CreatingAccount -> "Create a New Account"
        }


        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )

            OutlinedTextField(
                value = usernameField,
                isError = errorMessage.isNotEmpty(),
                onValueChange = {
                    usernameField = it
                    errorMessage = ""
                },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val passwordLabel = when (authenticationMode) {
                AuthenticationMode.SigningIn -> "Password"
                AuthenticationMode.CreatingAccount -> "New Password"
            }

            OutlinedTextField(
                value = passwordField,
                isError = errorMessage.isNotEmpty(),
                onValueChange = {
                    passwordField = it
                    errorMessage = ""
                },
                label = { Text(passwordLabel) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible= authenticationMode==AuthenticationMode.CreatingAccount,
                enter = fadeIn(initialAlpha = 0.4f),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordConfirmationField,
                    isError = errorMessage.isNotEmpty(),
                    onValueChange = {
                        passwordConfirmationField = it
                        errorMessage = ""
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // signIn/createAccount produces non-descriptive errors in these cases
                    if (usernameField.isEmpty()){
                        errorMessage = "No Username Provided"
                    }else if (passwordField.isEmpty()){
                        errorMessage = "No Password Provided"
                    }else if ((authenticationMode == AuthenticationMode.CreatingAccount) &&
                        (passwordConfirmationField != passwordField)) {
                        errorMessage = "Passwords do not Match"
                    } else {
                        when (authenticationMode) {
                            AuthenticationMode.SigningIn -> {
                                scope.launch {
                                    try {
                                        Firebase.auth.signInWithEmailAndPassword(usernameField, passwordField)
                                        navigator.push(MapListScreen())
                                    } catch (e: Exception){
                                       errorMessage = e.message ?: ""

                                    }
                                }
                            }

                            AuthenticationMode.CreatingAccount -> {
                                scope.launch {
                                    firebase.createUserWithEmailAndPassword(usernameField, passwordField).addOnSuccessListener {
                                        authenticationMode = AuthenticationMode.SigningIn
                                    }.addOnFailureListener {
                                        errorMessage = it.message ?: ""
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red)
            }

            // toggle between signing in and creating a new account
            ClickableText(
                text = AnnotatedString(when (authenticationMode) {
                    AuthenticationMode.CreatingAccount -> "Return to Login"
                    AuthenticationMode.SigningIn -> "Create a New Account"
                }),
                onClick = {
                    when (authenticationMode) {
                        AuthenticationMode.SigningIn -> {
                            authenticationMode = AuthenticationMode.CreatingAccount
                        }
                        AuthenticationMode.CreatingAccount -> {
                            authenticationMode = AuthenticationMode.SigningIn
                        }
                    }

                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ClickableText(AnnotatedString("Continue as Guest")) {
                navigator.push(MapListScreen())
            }
        }
    }
}