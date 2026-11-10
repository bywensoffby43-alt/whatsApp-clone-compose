

package io.getstream.whatsappclone.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.getstream.whatsappclone.R
import io.getstream.whatsappclone.designsystem.icon.WhatsAppIcons
import io.getstream.whatsappclone.designsystem.theme.WhatsAppCloneComposeTheme

private const val APP_SHARE_URL = "ADD_APP_URL_HERE"

private data class MenuEntry(
val title: String,
val emoji: String
)

private val menuEntries = listOf(
MenuEntry("Profile", "👤"),
MenuEntry("Privacy", "🔒"),
MenuEntry("Notifications", "🔔"),
MenuEntry("Chat Settings", "💬"),
MenuEntry("Security", "🛡️"),
MenuEntry("Change Number", "📱"),
MenuEntry("Help & Support", "🆘"),
MenuEntry("Report a Problem", "⚠️"),
MenuEntry("FAQs", "❓"),
MenuEntry("Invite Friends", "👥"),
MenuEntry("About", "ℹ️"),
MenuEntry("Log Out", "🚪")
)

@Composable
fun WhatsAppTopBar(onSupportClick: () -> Unit = {}) {

var menuExpanded by remember { mutableStateOf(false) }
var selected by remember { mutableStateOf<String?>(null) }

val context = LocalContext.current

fun shareApp() {
    val shareText =
        if (APP_SHARE_URL == "ADD_APP_URL_HERE") {
            "Join me on WensChat! The download link will be available soon."
        } else {
            "Join me on WensChat! Download the app: $APP_SHARE_URL"
        }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join me on WensChat")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(sendIntent, "Invite Friends")
    )
}

TopAppBar(
    modifier = Modifier.fillMaxWidth(),
    title = {
        Text(
            text = stringResource(id = R.string.app_name),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.titleLarge
        )
    },
    actions = {
        Icon(
            modifier = Modifier.size(26.dp),
            imageVector = WhatsAppIcons.Search,
            tint = MaterialTheme.colorScheme.tertiary,
            contentDescription = null
        )

        Spacer(modifier = Modifier.size(16.dp))

        Box {
            IconButton(
                onClick = { menuExpanded = true }
            ) {
                Icon(
                    modifier = Modifier.size(26.dp),
                    imageVector = WhatsAppIcons.MoreVert,
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Open all settings") },
                    onClick = {
                        menuExpanded = false
                        selected = "__all__"
                    }
                )

                menuEntries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.title) },
                        onClick = {
                            menuExpanded = false

                            when (entry.title) {
                                "Invite Friends" -> shareApp()
                                "Help & Support" -> onSupportClick()
                                else -> selected = entry.title
                            }
                        }
                    )
                }
            }
        }
    },
    backgroundColor = MaterialTheme.colorScheme.primary
)

if (selected == "__all__") {
    Dialog(
        onDismissRequest = {
            selected = null
        }
    ) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "WensChat",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 440.dp)
                ) {
                    items(menuEntries) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (entry.title) {
                                        "Invite Friends" -> {
                                            selected = null
                                            shareApp()
                                        }

                                        "Help & Support" -> {
                                            selected = null
                                            onSupportClick()
                                        }

                                        else -> selected = entry.title
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = entry.emoji,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Text(
                                text = entry.title,
                                fontSize = 9.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        selected = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

selected?.takeIf { it != "__all__" }?.let { title ->

    var enabled by remember(title) {
        mutableStateOf(true)
    }

    var details by remember(title) {
        mutableStateOf("")
    }

    val body = when (title) {
        "Profile" ->
            "Profile settings. Profile editing and photo upload require the WensChat account backend to be connected."

        "Privacy" ->
            "Privacy controls. These settings need server-side privacy enforcement before they can protect account data."

        "Notifications" ->
            "Notification preference: ${if (enabled) "Enabled" else "Disabled"}. This switch is a local preview; push notification delivery must be connected to apply it fully."

        "Chat Settings" ->
            "Chat preferences. Local appearance and chat behavior can be added here; cloud synchronization is not connected yet."

        "Security" ->
            "Security settings. Enable account verification and secure session management through the authentication backend."

        "Change Number" ->
            "Changing your phone number requires verification of the new number with an OTP. No number has been changed."

        "Report a Problem" ->
            "Describe the problem in the field below, then use Send to open your email app."

        "FAQs" ->
            """Frequently Asked Questions

• How do I create an account? Sign in with your verified phone number.
• How do I contact support? Choose Help & Support.
• How do I invite friends? Choose Invite Friends and share the app link.
• How do I change my number? Use Change Number and complete OTP verification when available."""

        "About" ->
            "WensChat\nMessaging application\nPowered by WENS DEV."

        "Log Out" ->
            "Sign-out is not connected to an authentication provider in this project yet. Your account will not be signed out from this screen."

        else -> "Settings"
    }

    AlertDialog(
        onDismissRequest = {
            selected = null
        },
        title = {
            Text(title)
        },
        text = {
            Column {
                Text(body)

                if (
                    title == "Notifications" ||
                    title == "Privacy" ||
                    title == "Chat Settings" ||
                    title == "Security"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                            }
                        )

                        Text(
                            if (enabled) "Enabled" else "Disabled"
                        )
                    }
                }

                if (title == "Report a Problem") {
                    OutlinedTextField(
                        value = details,
                        onValueChange = {
                            details = it
                        },
                        label = {
                            Text("Describe the problem")
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (
                        title == "Report a Problem" &&
                        details.isNotBlank()
                    ) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "WensChat problem report"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                details
                            )
                        }

                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    "Send report"
                                )
                            )
                        }
                    }

                    selected = null
                }
            ) {
                Text(
                    if (title == "Report a Problem") "Send" else "Done"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    selected = null
                }
            ) {
                Text("Close")
            }
        }
    )
}

}

@Preview
@Composable
private fun WhatsAppTopBarPreview() {
WhatsAppCloneComposeTheme {
WhatsAppTopBar()
}
}

@Preview
@Composable
private fun WhatsAppTopBarDarkPreview() {
WhatsAppCloneComposeTheme(darkTheme = true) {
WhatsAppTopBar()
}
}