package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.model.GameMode
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MultiplayerLobbyScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.MainViewModel

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopTabs = listOf(
    NavTab("command", R.string.nav_command, Icons.Filled.GpsFixed, Icons.Outlined.GpsFixed),
    NavTab("armory", R.string.nav_armory, Icons.Filled.MilitaryTech, Icons.Outlined.MilitaryTech),
    NavTab("ranks", R.string.nav_ranks, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    NavTab("lan", R.string.nav_multiplayer, Icons.Filled.Lan, Icons.Outlined.Lan)
)

@Composable
fun IronfrontNavApp(viewModel: MainViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val loadouts by viewModel.loadouts.collectAsStateWithLifecycle()
    val activeLoadout by viewModel.activeLoadout.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedMap by viewModel.selectedMap.collectAsStateWithLifecycle()
    val lanLobbyState by viewModel.lanLobbyState.collectAsStateWithLifecycle()
    val activeSimulation by viewModel.activeSimulation.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    val sim = activeSimulation
    if (sim != null) {
        BattleScreen(
            simulation = sim,
            hud = hudState,
            highQualityGraphics = settings.highQualityGraphics,
            hapticsEnabled = settings.hapticsEnabled,
            onHudUpdate = viewModel::updateHudFromRenderer,
            onTogglePause = viewModel::togglePause,
            onRematch = { viewModel.launchMatch(sim.gameMode, sim.mapId) },
            onExitBattle = viewModel::endActiveMatch
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "command"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = GunmetalSurface) {
                        TopTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = GunmetalSurface) {
                        TopTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "command",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("command") {
                        HomeScreen(
                            profile = profile,
                            loadouts = loadouts,
                            activeLoadout = activeLoadout,
                            selectedMap = selectedMap,
                            selectedMode = selectedMode,
                            onSelectLoadoutSlot = viewModel::selectLoadoutSlot,
                            onOpenArmory = {
                                navController.navigate("armory") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSelectMap = viewModel::selectMap,
                            onSelectMode = viewModel::selectMode,
                            onDeploy = { viewModel.launchMatch(selectedMode, selectedMap) },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    composable("armory") {
                        ArmoryScreen(
                            profile = profile,
                            loadouts = loadouts,
                            activeLoadout = activeLoadout,
                            onSelectSlot = viewModel::selectLoadoutSlot,
                            onRenameSlot = viewModel::renameLoadoutSlot,
                            onSetPrimaryWeapon = viewModel::setPrimaryWeapon,
                            onSetSecondaryWeapon = viewModel::setSecondaryWeapon,
                            onSetOptic = viewModel::setPrimaryOptic,
                            onSetMuzzle = viewModel::setPrimaryMuzzle,
                            onSetPrimaryCamo = viewModel::setPrimaryCamo,
                            onSetSecondaryCamo = viewModel::setSecondaryCamo,
                            onSetPerk = viewModel::setLoadoutPerk,
                            onSaveCallsign = viewModel::updateCallsign
                        )
                    }
                    composable("ranks") {
                        LeaderboardScreen(
                            leaderboard = leaderboard,
                            history = matchHistory
                        )
                    }
                    composable("lan") {
                        MultiplayerLobbyScreen(
                            lobbyState = lanLobbyState,
                            selectedMap = selectedMap,
                            onSelectMap = viewModel::selectMap,
                            onHostMatch = viewModel::startHostLanMatch,
                            onJoinMatch = viewModel::joinLanMatch,
                            onStopSession = viewModel::stopLanSession,
                            onLaunchLanMatch = {
                                viewModel.launchMatch(GameMode.LAN_ARENA, lanLobbyState.selectedMap)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onUpdateSettings = viewModel::updateSettings,
            onDismiss = { showSettings = false }
        )
    }
}
