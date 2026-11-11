package io.getstream.whatsappclone.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

private enum class AuthStep { WELCOME, PHONE, OTP, PROFILE }
private data class CountryChoice(val name: String, val iso: String, val callingCode: Int) {
  val dialCode: String get() = "+$callingCode"
  val flag: String get() = iso.uppercase(Locale.ROOT).map { c ->
    String(Character.toChars(0x1F1E6 + (c.code - 'A'.code)))
  }.joinToString("")
}

private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }
private val countryChoices: List<CountryChoice> by lazy {
  Locale.getISOCountries().mapNotNull { iso ->
    val code = phoneUtil.getCountryCodeForRegion(iso)
    if (code <= 0) null else CountryChoice(
      name = Locale("", iso).getDisplayCountry(Locale.ENGLISH),
      iso = iso,
      callingCode = code
    )
  }.distinctBy { it.iso }.sortedBy { it.name }
}

/**
 * English onboarding UI. Backend actions are exposed as callbacks so Supabase Auth can be
 * connected without changing this screen's design.
 *
 * onSendCode receives the complete E.164 number, e.g. +509XXXXXXXX.
 * onVerifyCode receives that same number and the OTP entered by the user.
 */
@Composable
fun WelcomeAuthFlow(
  onSendCode: (phoneE164: String) -> Unit,
  onVerifyCode: (phoneE164: String, otp: String) -> Unit,
  onProfileContinue: (displayName: String) -> Unit,
  onLoginClick: () -> Unit = {},
) {
  var step by rememberSaveable { mutableStateOf(AuthStep.WELCOME) }
  var selectedIso by rememberSaveable { mutableStateOf("US") }
  var phone by rememberSaveable { mutableStateOf("") }
  var otp by rememberSaveable { mutableStateOf("") }
  var displayName by rememberSaveable { mutableStateOf("") }
  var countryMenuOpen by remember { mutableStateOf(false) }
  var error by rememberSaveable { mutableStateOf("") }

  val selectedCountry = countryChoices.firstOrNull { it.iso == selectedIso }
    ?: countryChoices.first { it.iso == "US" }
  val phoneE164 = selectedCountry.dialCode + phone.filter(Char::isDigit)

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("WensChat", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(12.dp))

    when (step) {
      AuthStep.WELCOME -> {
        Text("Connect. Chat. Share.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { error = ""; step = AuthStep.PHONE }, modifier = Modifier.fillMaxWidth()) {
          Text("Create Account")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
          Text("Log In")
        }
      }

      AuthStep.PHONE -> {
        Text("Select your country", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Column {
          OutlinedButton(onClick = { countryMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("${selectedCountry.flag}  ${selectedCountry.name}  (${selectedCountry.dialCode})  ▾")
          }
          DropdownMenu(expanded = countryMenuOpen, onDismissRequest = { countryMenuOpen = false }) {
            countryChoices.forEach { country ->
              DropdownMenuItem(
                text = { Text("${country.flag}  ${country.name}  (${country.dialCode})") },
                onClick = { selectedIso = country.iso; countryMenuOpen = false }
              )
            }
          }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch.isWhitespace() || ch == '-' }.take(20) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Phone Number") },
          placeholder = { Text("Enter your number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true
        )
        Spacer(Modifier.height(18.dp))
        Button(
          onClick = {
            val digits = phone.filter(Char::isDigit)
            val valid = try { phoneUtil.isValidNumber(phoneUtil.parse(phoneE164, null)) } catch (_: Exception) { false }
            if (digits.isEmpty() || !valid) error = "Enter a valid phone number for the selected country."
            else { error = ""; onSendCode(phoneE164); step = AuthStep.OTP }
          },
          modifier = Modifier.fillMaxWidth()
        ) { Text("Send Verification Code") }
        TextButton(onClick = { step = AuthStep.WELCOME }) { Text("Back") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
      }

      AuthStep.OTP -> {
        Text("Enter the code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Enter the 6-digit verification code sent to $phoneE164")
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
          value = otp,
          onValueChange = { otp = it.filter(Char::isDigit).take(6) },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Verification Code") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true
        )
        Spacer(Modifier.height(18.dp))
        Button(
          onClick = {
            if (otp.length != 6) error = "Enter the 6-digit code."
            else { error = ""; onVerifyCode(phoneE164, otp) }
          },
          modifier = Modifier.fillMaxWidth()
        ) { Text("Verify Code") }
        TextButton(onClick = { otp = ""; error = ""; step = AuthStep.PHONE }) { Text("Change Number") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
      }

      AuthStep.PROFILE -> {
        Text("Set Up Your Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Your Name") },
          placeholder = { Text("Enter your name") },
          singleLine = true
        )
        Spacer(Modifier.height(18.dp))
        Button(
          onClick = { onProfileContinue(displayName.trim()) },
          enabled = displayName.isNotBlank(),
          modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
      }
    }
  }
}

/** Call this only after Supabase Auth confirms the OTP successfully. */
@Composable
fun AuthProfileStepEntry(onVerified: () -> Unit) {
  // Kept as a tiny integration hook for the app's existing navigation if needed.
  TextButton(onClick = onVerified) { Text("Continue") }
}
