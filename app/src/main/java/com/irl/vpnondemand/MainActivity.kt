@file:SuppressLint("NewApi")

package com.irl.vpnondemand

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      VpnOnDemandTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            TopAppBar(
              title = { Text(stringResource(R.string.app_name)) }
            )
          }
        ) { innerPadding ->
          Content(innerPadding)
        }
      }
    }

    startService(Intent(this, StartupService::class.java))
  }

  @Composable
  private fun Content(padding: PaddingValues) {
    val ssidHelper: SsidHelper = remember {
      SsidHelper.instance(this@MainActivity)
    }

    val enabled: Boolean by remember {
      ssidHelper.enabled
    }.collectAsState(initial = false)

    val currentSsid: String? by remember {
      ssidHelper.currentSsid
    }.collectAsState(initial = null)

    val homeSsids: List<String> by remember {
      ssidHelper.homeSsids
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SwitchWithLabel(
        label = "Enable Tailscale on demand",
        state = enabled,
        onStateChange = {
          scope.launch {
            ssidHelper.setEnabled(it)
          }
        }
      )

      var text: String by remember { mutableStateOf("") }
      if (enabled) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Home SSID") },
            modifier = Modifier.weight(1f),
          )

          TextButton(
            onClick = {
              scope.launch {
                ssidHelper.addHomeSsid(text)
                text = ""
              }
            },
            enabled = text.isNotEmpty() && text !in homeSsids,
          ) {
            Text("Add")
          }
        }

        Text(
          text = "Disabling Tailscale for the following networks:",
        )

        LazyColumn(
          modifier = Modifier.weight(1f)
        ) {
          items(homeSsids) { ssid ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = ssid,
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.padding(start = 8.dp))
              IconButton(
                onClick = {
                  scope.launch {
                    ssidHelper.removeHomeSsid(ssid)
                  }
                }
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
              }
            }
          }
        }

        if (currentSsid != null && currentSsid !in homeSsids) {
          Button(
            onClick = {
              scope.launch {
                ssidHelper.addHomeSsid(currentSsid!!)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
          ) {
            Text("Add $currentSsid as home network")
          }
        }
      }
    }
  }
}

@Composable
private fun SwitchWithLabel(
  label: String,
  state: Boolean,
  onStateChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier = modifier
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Switch,
        onClick = { onStateChange(!state) }
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(label)
    Switch(
      checked = state,
      onCheckedChange = { onStateChange(it) }
    )
  }
}

@Composable
fun VpnOnDemandTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

val Typography = Typography(
  bodyLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.5.sp
  )
)