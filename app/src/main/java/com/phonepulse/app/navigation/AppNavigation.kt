package com.phonepulse.app.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phonepulse.core.common.ShareUtils
import com.phonepulse.core.database.dao.DiagnosticDao
import com.phonepulse.core.model.Certificate
import com.phonepulse.feature.history.CompareScreen
import com.phonepulse.app.ui.SplashScreen
import com.phonepulse.feature.certificate.CertificateScreen
import com.phonepulse.feature.certificate.PdfExporter
import com.phonepulse.feature.certificate.ResultsScreen
import com.phonepulse.feature.diagnostic.ui.DiagnosticScreen
import com.phonepulse.feature.diagnostic.ui.PermissionGateScreen
import com.phonepulse.feature.history.HistoryScreen
import com.phonepulse.feature.home.HomeScreen
import com.phonepulse.feature.onboarding.OnboardingScreen
import com.phonepulse.feature.scanner.ScannerScreen
import kotlinx.serialization.json.Json

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PERMISSIONS = "permissions"
    const val DIAGNOSTIC = "diagnostic"
    const val RESULTS = "results/{certId}"
    const val CERTIFICATE = "certificate/{certId}"
    const val SCANNER = "scanner"
    const val HISTORY = "history"
    const val COMPARE = "compare/{certId1}/{certId2}"
}

@Composable
fun AppNavigation(
    context: Context,
    dao: DiagnosticDao,
    navController: NavHostController = rememberNavController()
) {
    val prefs = remember {
        context.getSharedPreferences("phonepulse_prefs", Context.MODE_PRIVATE)
    }
    val isFirstLaunch = remember { prefs.getBoolean("first_launch", true) }
    val startDestination = Routes.SPLASH

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val destination = if (isFirstLaunch) Routes.ONBOARDING else Routes.HOME
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean("first_launch", false).apply()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onStartDiagnostic = { navController.navigate(Routes.PERMISSIONS) },
                onScanQR = { navController.navigate(Routes.SCANNER) },
                onHistory = { navController.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionGateScreen(
                onAllGranted = {
                    navController.navigate(Routes.DIAGNOSTIC) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DIAGNOSTIC) {
            DiagnosticScreen(
                onComplete = { certId ->
                    navController.navigate("results/$certId") {
                        popUpTo(Routes.DIAGNOSTIC) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RESULTS) { backStackEntry ->
            val certId = backStackEntry.arguments?.getString("certId") ?: return@composable
            var certificate by remember { mutableStateOf<Certificate?>(null) }

            LaunchedEffect(certId) {
                val session = dao.getSession(certId)
                session?.let {
                    certificate = try {
                        Json.decodeFromString<Certificate>(it.certificateJson)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            certificate?.let { cert ->
                ResultsScreen(
                    certificate = cert,
                    onViewCertificate = { navController.navigate("certificate/$certId") }
                )
            }
        }

        composable(Routes.CERTIFICATE) { backStackEntry ->
            val certId = backStackEntry.arguments?.getString("certId") ?: return@composable
            var certificate by remember { mutableStateOf<Certificate?>(null) }

            LaunchedEffect(certId) {
                val session = dao.getSession(certId)
                session?.let {
                    certificate = try {
                        Json.decodeFromString<Certificate>(it.certificateJson)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            certificate?.let { cert ->
                CertificateScreen(
                    certificate = cert,
                    onShare = {
                        ShareUtils.shareCertificate(
                            context = context,
                            certId = cert.certId,
                            deviceModel = "${cert.device.manufacturer} ${cert.device.model}",
                            score = cert.overallScore,
                            grade = cert.grade
                        )
                    },
                    onSavePdf = {
                        PdfExporter.generateAndShare(context, cert)
                    }
                )
            }
        }

        composable(Routes.SCANNER) {
            ScannerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onSessionClick = { certId -> navController.navigate("results/$certId") },
                onCompareSelected = { certId1, certId2 ->
                    navController.navigate("compare/$certId1/$certId2")
                }
            )
        }

        composable(Routes.COMPARE) { backStackEntry ->
            val id1 = backStackEntry.arguments?.getString("certId1") ?: return@composable
            val id2 = backStackEntry.arguments?.getString("certId2") ?: return@composable
            var cert1 by remember { mutableStateOf<Certificate?>(null) }
            var cert2 by remember { mutableStateOf<Certificate?>(null) }

            LaunchedEffect(id1, id2) {
                dao.getSession(id1)?.let { session ->
                    cert1 = try {
                        Json.decodeFromString<Certificate>(session.certificateJson)
                    } catch (_: Exception) {
                        null
                    }
                }
                dao.getSession(id2)?.let { session ->
                    cert2 = try {
                        Json.decodeFromString<Certificate>(session.certificateJson)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            if (cert1 != null && cert2 != null) {
                CompareScreen(cert1 = cert1!!, cert2 = cert2!!)
            }
        }
    }
}
