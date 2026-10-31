package com.maty.rocola.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.maty.rocola.data.MusicRepository
import com.maty.rocola.ui.components.PillButton
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType

@Composable
fun PermissionScreen(onResult: () -> Unit) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val audioPermission = MusicRepository.audioPermission()
    var blocked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult()
        if (result[audioPermission] != true) {
            blocked = activity?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, audioPermission) } ?: true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Rocola.Vino)
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text("Rocola", style = RocolaType.wordmark.copy(fontSize = 58.sp, lineHeight = 60.sp), color = Rocola.Marfil)
        Spacer(Modifier.height(20.dp))
        Text(
            "Para mostrar y reproducir las canciones guardadas en este teléfono, Rocola necesita permiso para leer tus archivos de audio.",
            style = RocolaType.body.copy(fontSize = 18.sp, lineHeight = 27.sp),
            color = Rocola.Arena,
        )
        Spacer(Modifier.weight(1.2f))
        if (blocked) {
            Text(
                "Android bloqueó el pedido. Activá el permiso en Ajustes, en Permisos > Música y audio.",
                style = RocolaType.body,
                color = Rocola.Marfil,
            )
            Spacer(Modifier.height(16.dp))
            PillButton(
                text = "Abrir ajustes",
                icon = null,
                filled = true,
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PillButton(
                text = "Permitir acceso a la música",
                icon = null,
                filled = true,
                onClick = {
                    val permissions = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    launcher.launch(permissions.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
