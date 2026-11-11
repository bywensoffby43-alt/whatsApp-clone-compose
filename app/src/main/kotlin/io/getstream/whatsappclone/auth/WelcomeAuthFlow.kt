package io.getstream.whatsappclone.aut

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

private val Blue = Color(0xFF1769E8)
private val Ink = Color(0xFF172033)
private val Muted = Color(0xFF718096)
private val Line = Color(0xFFDCE3EF)
private val Page = Color(0xFFF7F9FD)

private enum class Step { WELCOME, PHONE, OTP, PROFILE, SUCCESS }
private data class Country(val name: String, val iso: String, val code: Int) {
    val dial = "+$code"
    val flag: String get() = iso.map { String(Character.toChars(0x1F1E6 + it.code - 'A'.code)) }.joinToString("")
}
private val phoneUtil by lazy { PhoneNumberUtil.getInstance() }
private val countries by lazy {
    Locale.getISOCountries().mapNotNull { iso ->
        val code = phoneUtil.getCountryCodeForRegion(iso)
        if (code <= 0) null else Country(Locale("", iso).getDisplayCountry(Locale.ENGLISH), iso, code)
    }.distinctBy { it.iso }.sortedBy { it.name }
}

@Composable
fun WelcomeAuthFlow(
    onSendCode: (phoneE164: String) -> Unit,
    onVerifyCode: (phoneE164: String, otp: String) -> Unit,
    onProfileContinue: (displayName: String) -> Unit,
    onLoginClick: () -> Unit = {},
    onStartChatting: () -> Unit = {},
) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var countryIso by rememberSaveable { mutableStateOf("HT") }
    var phone by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val context = LocalContext.current
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
        photoBitmap = uri?.let { selected ->
            runCatching { context.contentResolver.openInputStream(selected)?.use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    val country = countries.firstOrNull { it.iso == countryIso } ?: countries.first { it.iso == "HT" }
    val fullNumber = country.dial + phone.filter(Char::isDigit)

    Surface(Modifier.fillMaxSize(), color = Page) {
        when (step) {
            Step.WELCOME -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(112.dp).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) {
                    Text("☎", color = Color.White, fontSize = 62.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("WensChat", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                Text("Connect WensChat ²⁰²⁶.", color = Muted, fontSize = 17.sp)
                Spacer(Modifier.height(44.dp))
                Text("💬     👤     💬", fontSize = 38.sp)
                Spacer(Modifier.height(42.dp))
                MainButton("Create Account") { error = ""; step = Step.PHONE }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(30.dp)) {
                    Text("connecté", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Text("Connect with the people who matter.", color = Muted, textAlign = TextAlign.Center)
            }
            Step.PHONE -> AuthPage("Create Account", { step = Step.WELCOME }) {
                Text("Select your country", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(country.flag)
                        Spacer(Modifier.width(10.dp))
                        Text(country.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, color = Ink)
                        Text(country.dial, color = Blue, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                        countries.forEach { item -> DropdownMenuItem(
                            text = { Text("${item.flag}  ${item.name} (${item.dial})") },
                            onClick = { countryIso = item.iso; menu = false }
                        ) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Phone Number", color = Ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == ' ' || c == '-' }.take(20); error = "" },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter your phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Spacer(Modifier.height(22.dp))
                MainButton("Send Verification Code", enabled = phone.isNotBlank()) {
                    val valid = runCatching { phoneUtil.isValidNumber(phoneUtil.parse(fullNumber, null)) }.getOrDefault(false)
                    if (!valid) error = "Enter a valid phone number for the selected country."
                    else { error = ""; onSendCode(fullNumber); step = Step.OTP }
                }
                if (error.isNotBlank()) Error(error)
            }
            Step.OTP -> AuthPage("Verify Your Number", { step = Step.PHONE }) {
                Text("▣", color = Blue, fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text("Enter the code", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("We sent a 6-digit code to\n$fullNumber", color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(6) { index ->
                        Box(Modifier.weight(1f).height(54.dp).border(1.dp, if (otp.length == index) Blue else Line, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Text(otp.getOrNull(index)?.toString() ?: "", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                BasicTextField(value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(6); error = "" },
                    modifier = Modifier.fillMaxWidth().height(1.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(24.dp))
                MainButton("Verify Code", enabled = otp.length == 6) {
                    error = ""
                    onVerifyCode(fullNumber, otp)
                    // The callback must perform real server verification. Wire its success result to navigation in the host.
                    step = Step.PROFILE
                }
                TextButton(onClick = { error = ""; onSendCode(fullNumber); otp = "" }) { Text("Resend Code", color = Blue) }
                if (error.isNotBlank()) Error(error)
            }
            Step.PROFILE -> AuthPage("Set Up Your Profile", { step = Step.OTP }) {
                Box(Modifier.size(136.dp).clip(CircleShape).background(Color(0xFFE6EEFC)).clickable { gallery.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (photoBitmap != null) Image(photoBitmap!!.asImageBitmap(), contentDescription = "Selected profile photo", modifier = Modifier.fillMaxSize().clip(CircleShape))
                    else Text("Add Photo", color = Blue, fontWeight = FontWeight.Bold)
                    Box(Modifier.align(Alignment.BottomEnd).size(38.dp).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) { Text("📷", fontSize = 18.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Tap the photo to open your gallery", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
                Text("Your Name", modifier = Modifier.fillMaxWidth(), color = Ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it; error = "" }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter your name") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Spacer(Modifier.height(24.dp))
                MainButton("Continue", enabled = name.isNotBlank()) { onProfileContinue(name.trim()); step = Step.SUCCESS }
                if (error.isNotBlank()) Error(error)
            }
            Step.SUCCESS -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(140.dp).clip(CircleShape).background(Color(0xFFDCEAFF)), contentAlignment = Alignment.Center) { Text("✓", color = Blue, fontSize = 82.sp) }
                Spacer(Modifier.height(26.dp))
                Text("Welcome to\nWensChat!", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("Your profile is ready.", color = Muted, fontSize = 16.sp)
                Spacer(Modifier.height(28.dp))
                MainButton("Start Chatting", onClick = onStartChatting)
            }
        }
    }
}

@Composable
private fun AuthPage(title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Blue)) {
        Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.clickable(onClick = back).padding(end = 22.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun MainButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Error(message: String) { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp), fontSize = 13.sp) }
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue, unfocusedBorderColor = Line, cursorColor = Blue)
