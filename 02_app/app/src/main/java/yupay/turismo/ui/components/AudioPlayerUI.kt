package yupay.turismo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import yupay.turismo.utils.UiTranslations

@Composable
fun AudioPlayerUI(
    currentTime: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    language: String = "Español"
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!compact) {
            Slider(
                value = if (totalDuration > 0) currentTime.toFloat() / totalDuration else 0f,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentTime), fontSize = 12.sp)
                Text(text = formatTime(totalDuration), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onRewind) {
                Icon(Icons.Default.Replay10, contentDescription = UiTranslations.getString(context, "audio_cd_rewind", language))
            }
            
            FloatingActionButton(
                onClick = onPlayPauseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(if (compact) 48.dp else 56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = UiTranslations.getString(context, if (isPlaying) "audio_cd_pause" else "audio_cd_play", language)
                )
            }

            IconButton(onClick = onFastForward) {
                Icon(Icons.Default.Forward10, contentDescription = UiTranslations.getString(context, "audio_cd_forward", language))
            }
        }
        
        if (compact) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (totalDuration > 0) currentTime.toFloat() / totalDuration else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentTime), fontSize = 10.sp)
                Text(text = formatTime(totalDuration), fontSize = 10.sp)
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
