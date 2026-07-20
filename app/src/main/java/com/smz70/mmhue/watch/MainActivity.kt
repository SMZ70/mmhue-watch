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
                onRetry = model::refresh,
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
                onPreviewBrightness = { pct -> model.previewBrightness(lightId, pct) },
                onCommitBrightness = { pct -> model.setBrightness(lightId, pct) },
                onOpenWarmth = { navController.navigate("warmth/$lightId") },
                onOpenColor = { navController.navigate("color/$lightId") },
            )
        }

        composable(
            route = "warmth/{lightId}",
            arguments = listOf(navArgument("lightId") { type = NavType.StringType }),
        ) { entry ->
            val lightId = entry.arguments?.getString("lightId").orEmpty()
            WarmthScreen(
                ui = ui,
                lightId = lightId,
                onPreviewTemp = { mirek -> model.previewColorTemp(lightId, mirek) },
                onCommitTemp = { mirek -> model.setColorTemp(lightId, mirek) },
            )
        }

        composable(
            route = "color/{lightId}",
            arguments = listOf(navArgument("lightId") { type = NavType.StringType }),
        ) { entry ->
            val lightId = entry.arguments?.getString("lightId").orEmpty()
            ColorScreen(
                ui = ui,
                lightId = lightId,
                onPreviewHue = { hue -> model.previewHue(lightId, hue) },
                onCommitHue = { hue -> model.setColor(lightId, hue, Hue.SATURATION) },
            )
        }
    }
}
