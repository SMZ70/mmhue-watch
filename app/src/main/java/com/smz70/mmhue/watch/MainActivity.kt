package com.smz70.mmhue.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

class MainActivity : ComponentActivity() {

    private val model: HomeViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private val wifi: WifiNetworkManager by lazy { WifiNetworkManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the saved mmhue address before anything makes a request.
        AppConfig.load(this)
        setContent { MmhueApp() }
    }

    override fun onResume() {
        super.onResume()
        // Bring Wi-Fi up before polling: the panel is a LAN host the watch's
        // Bluetooth internet cannot reach, so without this the first requests
        // fail until (if ever) Wi-Fi happens to be on.
        wifi.acquire()
        model.startPolling()
    }

    override fun onPause() {
        super.onPause()
        model.stopPolling()
        wifi.release()
    }
}

@Composable
fun MmhueApp(model: HomeViewModel = viewModel()) {
    val navController = rememberSwipeDismissableNavController()
    val ui by model.ui.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(
                ui = ui,
                onAllOn = model::allOn,
                onAllOff = model::allOff,
                onRoomToggle = model::setRoom,
                onRoomOpen = { roomId -> navController.navigate("room/$roomId") },
                onOpenSettings = { navController.navigate("settings") },
                onRetry = model::refresh,
            )
        }

        composable("settings") {
            SettingsScreen(
                currentUrl = AppConfig.baseUrl,
                onSave = { url ->
                    AppConfig.setBaseUrl(context, url)
                    model.refresh()
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = "room/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
        ) { entry ->
            val roomId = entry.arguments?.getString("roomId").orEmpty()
            RoomScreen(
                ui = ui,
                roomId = roomId,
                onRoomToggle = { on -> model.setRoom(roomId, on) },
                onRoomBrightness = { pct -> model.setRoomBrightness(roomId, pct) },
                onRoomWarmth = { mirek -> model.setRoomWarmth(roomId, mirek) },
                onOpenColor = { navController.navigate("roomcolor/$roomId") },
                onLightToggle = model::toggleLight,
                onLightOpen = { lightId -> navController.navigate("light/$lightId") },
            )
        }

        composable(
            route = "light/{lightId}",
            arguments = listOf(navArgument("lightId") { type = NavType.StringType }),
        ) { entry ->
            val lightId = entry.arguments?.getString("lightId").orEmpty()
            LightScreen(
                ui = ui,
                lightId = lightId,
                onToggle = { model.toggleLight(lightId) },
                onBrightness = { pct -> model.setBrightness(lightId, pct) },
                onWarmth = { mirek -> model.setColorTemp(lightId, mirek) },
                onOpenColor = { navController.navigate("color/$lightId") },
            )
        }

        composable(
            route = "color/{lightId}",
            arguments = listOf(navArgument("lightId") { type = NavType.StringType }),
        ) { entry ->
            val lightId = entry.arguments?.getString("lightId").orEmpty()
            val light = ui.home?.light(lightId)
            ColorWheelScreen(
                initialHue = light?.hue ?: 0f,
                initialSat = light?.saturation ?: 1f,
                onPick = { hue, sat -> model.setColor(lightId, hue, sat) },
            )
        }

        composable(
            route = "roomcolor/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
        ) { entry ->
            val roomId = entry.arguments?.getString("roomId").orEmpty()
            val group = ui.home?.let { RoomAggregate(it.lightsIn(roomId)) }
            ColorWheelScreen(
                initialHue = group?.hue ?: 0f,
                initialSat = group?.lights?.firstOrNull { it.saturation != null }?.saturation ?: 1f,
                onPick = { hue, sat -> model.setRoomColor(roomId, hue, sat) },
            )
        }
    }
}
