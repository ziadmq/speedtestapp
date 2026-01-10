package com.mobix.speedtest.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobix.speedtest.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(
    viewModel: NetworkToolsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Scaffold(
        containerColor = Color(0xFF060B12),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.network_tools_title),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${stringResource(R.string.updating_data)} ${
                                safeText(
                                    state.lastUpdated,
                                    "--"
                                )
                            }",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadData()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.updating_data)
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF060B12)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NetworkHealthCard(
                    ssid = state.wifi?.ssid ?: context.getString(R.string.unknown),
                    isp = state.ispName,
                    routerIp = state.routerIp,
                    localIp = state.localIp
                )
            }

            item {
                ExpandableInfoCard(
                    title = stringResource(R.string.network_details_title),
                    subtitle = stringResource(R.string.network_details_subtitle),
                    icon = Icons.Default.Wifi,
                    accent = Color(0xFF00D1FF)
                ) {
                    val wifi = state.wifi

                    CopyDetailItem(
                        label = stringResource(R.string.isp_label),
                        value = safeText(state.ispName, context.getString(R.string.unknown)),
                        icon = Icons.Default.Business,
                        context = context
                    ) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.copied_isp)
                            )
                        }
                    }


                    CopyDetailItem(
                        label = stringResource(R.string.ssid_label),
                        value = safeText(wifi?.ssid, context.getString(R.string.unknown)),
                        icon = Icons.Default.Wifi,
                        context = context
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("SSID copied ✅") }
                    }

                    CopyDetailItem(
                        label = stringResource(R.string.router_ip_label),
                        value = safeText(state.routerIp, context.getString(R.string.unknown)),
                        icon = Icons.Default.Router,
                        context = context
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("Router IP copied ✅") }
                    }

                    CopyDetailItem(
                        label = stringResource(R.string.router_name_label),
                        value = safeText(state.routerName, context.getString(R.string.unknown)),
                        icon = Icons.Default.Dns,
                        context = context
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("Router name copied ✅") }
                    }

                    CopyDetailItem(
                        label = stringResource(R.string.wifi_version_label),
                        value = safeText(wifi?.routerType, context.getString(R.string.unknown)),
                        icon = Icons.Default.SettingsInputAntenna,
                        context = context
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("Wi-Fi version copied ✅") }
                    }

                    CopyDetailItem(
                        label = stringResource(R.string.local_ip_label),
                        value = safeText(state.localIp, context.getString(R.string.unknown)),
                        icon = Icons.Default.Language,
                        context = context
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("Local IP copied ✅") }
                    }
                }
            }

            item {
                DevicesHeaderRow(
                    count = state.devices.size,
                    isScanning = state.isScanning,
                    onRescan = {
                        viewModel.loadData()
                        scope.launch { snackbarHostState.showSnackbar("Scanning...") }
                    }
                )
            }

            items(state.devices, key = { it.ip }) { device ->
                val isRouter = device.ip == state.routerIp
                DeviceItemImproved(
                    name = device.name,
                    ip = device.ip,
                    isRouter = isRouter,
                    context = context,
                    onCopied = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------- Cards ----------------------------- */

@Composable
private fun NetworkHealthCard(
    ssid: String,
    isp: String,
    routerIp: String,
    localIp: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A26),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00D1FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF00D1FF))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        safeText(ssid, stringResource(R.string.unknown)),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        safeText(isp, stringResource(R.string.unknown)),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            stringResource(R.string.live),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF00FFC2).copy(alpha = 0.16f),
                        labelColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00FFC2).copy(alpha = 0.25f))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStatBox(
                    stringResource(R.string.router_ip_label),
                    safeText(routerIp, "N/A"),
                    Icons.Default.Router,
                    Modifier.weight(1f)
                )
                MiniStatBox(
                    stringResource(R.string.local_ip_label),
                    safeText(localIp, "N/A"),
                    Icons.Default.Language,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniStatBox(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF00D1FF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/* ----------------------------- Expandable Info ----------------------------- */

@Composable
private fun ExpandableInfoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A26),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { expanded = !expanded },
                        onLongClick = { expanded = !expanded }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f)
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
            }
        }
    }
}

@Composable
private fun CopyDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    context: Context,
    onCopied: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    copyToClipboard(context, label, value)
                    onCopied()
                },
                onLongClick = {
                    copyToClipboard(context, label, value)
                    onCopied()
                }
            )
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
        )
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/* ----------------------------- Devices ----------------------------- */

@Composable
private fun DevicesHeaderRow(
    count: Int,
    isScanning: Boolean,
    onRescan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.connected_devices, count),
                color = Color(0xFF00D1FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRescan) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00D1FF))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.scan),
                    color = Color(0xFF00D1FF),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFF00D1FF),
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun DeviceItemImproved(
    name: String,
    ip: String,
    isRouter: Boolean,
    context: Context,
    onCopied: (String) -> Unit
) {
    val icon = when {
        isRouter -> Icons.Default.Router
        name.contains("android", true) || name.contains("phone", true) -> Icons.Default.PhoneAndroid
        name.contains("tv", true) -> Icons.Default.Tv
        name.contains("laptop", true) || name.contains("pc", true) -> Icons.Default.Computer
        name.contains("iphone", true) || name.contains("ipad", true) -> Icons.Default.PhoneIphone
        else -> Icons.Default.Devices
    }

    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        copyToClipboard(context, "IP", ip)
                        onCopied(context.getString(R.string.copied_device_ip))
                    },
                    onLongClick = {
                        copyToClipboard(context, "Device", "$name • $ip")
                        onCopied(context.getString(R.string.copied_device_data))
                    }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00D1FF).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00D1FF))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        safeText(name, context.getString(R.string.unknown_device)),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRouter) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "ROUTER",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF00FFC2).copy(alpha = 0.16f),
                                labelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFF00FFC2).copy(alpha = 0.25f))
                        )
                    }
                }
                Text("IP: $ip", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

/* ----------------------------- Helpers ----------------------------- */

private fun safeText(value: String?, fallback: String): String {
    val v = value?.trim()
    return if (v.isNullOrEmpty() || v == "null") fallback else v
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
