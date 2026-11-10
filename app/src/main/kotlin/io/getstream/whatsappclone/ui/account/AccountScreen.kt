package io.getstream.whatsappclone.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    backend: AccountBackend = BackendNotConfigured(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { AccountSettingsStore(context) }
    var page by remember { mutableStateOf("Account") }
    var name by remember { mutableStateOf(store.profileName()) }
    var about by remember { mutableStateOf(store.profileAbout()) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(store.theme()) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = { if (page == "Account") onBack() else page = "Account" }) { Text("← Back") }
        Text(page, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when (page) {
            "Account" -> listOf("Profile", "Privacy", "Change Number", "Delete Account", "Security", "App Theme").forEach { item ->
                ListItem(headlineContent = { Text(item) }, modifier = Modifier.fillMaxWidth().clickable { page = item })
                HorizontalDivider()
            }
            "Profile" -> {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(about, { about = it }, label = { Text("About") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { store.saveProfile(name, about); message = "Profile saved on this device." }) { Text("Save profile") }
            }
            "Privacy" -> listOf("last_seen" to "Last seen", "profile_photo" to "Profile photo", "about" to "About", "read_receipts" to "Read receipts", "groups" to "Group invitations").forEach { (key, label) ->
                var checked by remember { mutableStateOf(store.privacy(key)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, modifier = Modifier.weight(1f).padding(top = 14.dp))
                    Switch(checked, { checked = it; store.setPrivacy(key, it) })
                }
            }
            "Change Number" -> {
                Text("Enter your new number in international format, for example +509XXXXXXXX.")
                OutlinedTextField(phone, { phone = it }, label = { Text("New phone number") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { scope.launch { message = backend.requestPhoneNumberChange(phone).fold({ "Verification request sent." }, { it.message ?: "Backend not configured." }) } }) { Text("Request verification") }
                OutlinedTextField(otp, { otp = it }, label = { Text("OTP code") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { scope.launch { message = backend.confirmPhoneNumberChange(phone, otp).fold({ "Number updated." }, { it.message ?: "Backend not configured." }) } }) { Text("Confirm number") }
            }
            "Delete Account" -> {
                Text("This action permanently deletes the account and associated server data. It requires a configured backend and re-authentication.")
                Button(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete my account") }
                if (showDeleteConfirm) AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Confirm account deletion") }, text = { Text("This cannot be undone. Continue only after the backend is configured.") }, confirmButton = { TextButton(onClick = { showDeleteConfirm = false; scope.launch { message = backend.deleteMyAccount().fold({ "Account deleted." }, { it.message ?: "Backend not configured." }) } }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
            }
            "Security" -> {
                Text("Security actions are handled by your authentication provider when connected.")
                Button(onClick = { scope.launch { message = backend.signOutEverywhere().fold({ "Other sessions signed out." }, { it.message ?: "Backend not configured." }) } }) { Text("Sign out other sessions") }
            }
            "App Theme" -> {
                listOf("SYSTEM", "LIGHT", "DARK").forEach { value ->
                    Row(Modifier.fillMaxWidth().clickable { theme = value; store.setTheme(value) }.padding(vertical = 12.dp)) {
                        RadioButton(selected = theme == value, onClick = { theme = value; store.setTheme(value) })
                        Text(value.lowercase().replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
                    }
                }
                Text("Theme choice is saved locally. Apply it to the app-wide theme wrapper when wiring the theme provider.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (message.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(message, color = MaterialTheme.colorScheme.primary) }
    }
}
