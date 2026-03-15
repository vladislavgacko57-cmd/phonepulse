package com.phonepulse.feature.diagnostic

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonepulse.core.common.HapticManager
import com.phonepulse.core.common.SoundManager
import com.phonepulse.core.database.dao.DiagnosticDao
import com.phonepulse.core.database.entity.DiagnosticSessionEntity
import com.phonepulse.core.model.Certificate
import com.phonepulse.core.model.DiagnosticWeights
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.jvm.JvmSuppressWildcards

enum class DiagnosticPhase {
    IDLE,
    RUNNING_AUTO,
    INTERACTIVE_PIXELS,
    INTERACTIVE_BURNIN,
    INTERACTIVE_TOUCH,
    INTERACTIVE_VOLUME,
    GENERATING_CERT,
    COMPLETED
}

data class DiagnosticState(
    val phase: DiagnosticPhase = DiagnosticPhase.IDLE,
    val currentModuleIndex: Int = 0,
    val currentModuleName: String = "",
    val totalModules: Int = 0,
    val progress: Float = 0f,
    val completedResults: List<TestResult> = emptyList(),
    val certificate: Certificate? = null,
    val error: String? = null
)

@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modules: List<@JvmSuppressWildcards DiagnosticModule>,
    private val dao: DiagnosticDao,
    private val priceEstimator: PriceEstimator
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticState())
    val state: StateFlow<DiagnosticState> = _state.asStateFlow()

    private val results = mutableListOf<TestResult>()

    fun startDiagnostic() {
        if (_state.value.phase != DiagnosticPhase.IDLE) return
        if (modules.isEmpty()) {
            _state.value = _state.value.copy(error = "Нет доступных модулей диагностики")
            return
        }

        viewModelScope.launch {
            results.clear()
            val totalSteps = modules.size + 4

            _state.value = DiagnosticState(
                phase = DiagnosticPhase.RUNNING_AUTO,
                totalModules = totalSteps
            )

            for ((index, module) in modules.withIndex()) {
                _state.value = _state.value.copy(
                    currentModuleIndex = index,
                    currentModuleName = module.moduleName,
                    progress = index.toFloat() / totalSteps
                )

                try {
                    val result = module.runAutomatic(context)
                    results.add(result)
                    HapticManager.tick(context)
                    launch { SoundManager.playModuleComplete() }
                } catch (e: Exception) {
                    results.add(
                        TestResult(
                            moduleName = module.moduleName,
                            score = 0,
                            status = TestStatus.FAILED,
                            details = mapOf("error" to (e.message ?: "unknown"))
                        )
                    )
                    HapticManager.error(context)
                    launch { SoundManager.playError() }
                }

                _state.value = _state.value.copy(completedResults = results.toList())
            }

            _state.value = _state.value.copy(
                phase = DiagnosticPhase.INTERACTIVE_PIXELS,
                progress = modules.size.toFloat() / totalSteps
            )
        }
    }

    fun onPixelTestResult(hasBadPixels: Boolean) {
        val displayIndex = results.indexOfFirst { it.moduleName == "display" }
        if (displayIndex >= 0) {
            val old = results[displayIndex]
            val newScore = if (hasBadPixels) (old.score - 40).coerceAtLeast(0) else old.score.coerceAtLeast(90)
            val newDetails = old.details.toMutableMap().apply {
                put("dead_pixels", "$hasBadPixels")
            }
            results[displayIndex] = old.copy(
                score = newScore,
                status = if (hasBadPixels) TestStatus.WARNING else TestStatus.PASSED,
                details = newDetails
            )
        }

        val totalSteps = modules.size + 4
        _state.value = _state.value.copy(
            phase = DiagnosticPhase.INTERACTIVE_BURNIN,
            completedResults = results.toList(),
            progress = (modules.size + 1).toFloat() / totalSteps
        )
    }

    fun onBurnInResult(hasBurnIn: Boolean) {
        val displayIndex = results.indexOfFirst { it.moduleName == "display" }
        if (displayIndex >= 0) {
            val old = results[displayIndex]
            val penalty = if (hasBurnIn) 25 else 0
            val newScore = (old.score - penalty).coerceIn(0, 100)
            val newDetails = old.details.toMutableMap().apply {
                put("burn_in", "$hasBurnIn")
            }
            results[displayIndex] = old.copy(
                score = newScore,
                details = newDetails,
                status = if (hasBurnIn) TestStatus.WARNING else old.status
            )
        }

        val totalSteps = modules.size + 4
        _state.value = _state.value.copy(
            phase = DiagnosticPhase.INTERACTIVE_TOUCH,
            completedResults = results.toList(),
            progress = (modules.size + 2).toFloat() / totalSteps
        )
    }

    fun onTouchTestResult(coveragePct: Int) {
        val displayIndex = results.indexOfFirst { it.moduleName == "display" }
        if (displayIndex >= 0) {
            val old = results[displayIndex]
            val touchScore = when {
                coveragePct >= 95 -> 100
                coveragePct >= 85 -> 85
                coveragePct >= 70 -> 65
                coveragePct >= 50 -> 40
                else -> 20
            }
            val newScore = ((old.score + touchScore) / 2).coerceIn(0, 100)
            val newDetails = old.details.toMutableMap().apply {
                put("touch_coverage_pct", "$coveragePct")
            }
            results[displayIndex] = old.copy(
                score = newScore,
                details = newDetails,
                status = when {
                    newScore >= 80 -> TestStatus.PASSED
                    newScore >= 50 -> TestStatus.WARNING
                    else -> TestStatus.FAILED
                }
            )
        }

        val totalSteps = modules.size + 4
        _state.value = _state.value.copy(
            phase = DiagnosticPhase.INTERACTIVE_VOLUME,
            completedResults = results.toList(),
            progress = (modules.size + 3).toFloat() / totalSteps
        )
    }

    fun onVolumeKeysResult(upOk: Boolean, downOk: Boolean) {
        val controlsIndex = results.indexOfFirst { it.moduleName == "controls" }
        if (controlsIndex >= 0) {
            val old = results[controlsIndex]
            var adjustment = 0
            if (!upOk) adjustment -= 20
            if (!downOk) adjustment -= 20
            val newScore = (old.score + adjustment).coerceIn(0, 100)
            val newDetails = old.details.toMutableMap().apply {
                put("volume_up", "$upOk")
                put("volume_down", "$downOk")
                remove("volume_keys")
            }
            results[controlsIndex] = old.copy(
                score = newScore,
                details = newDetails,
                status = when {
                    newScore >= 80 -> TestStatus.PASSED
                    newScore >= 50 -> TestStatus.WARNING
                    else -> TestStatus.FAILED
                }
            )
        }

        generateCertificate()
    }

    fun skipInteractiveTest() {
        when (_state.value.phase) {
            DiagnosticPhase.INTERACTIVE_PIXELS -> {
                val totalSteps = modules.size + 4
                _state.value = _state.value.copy(
                    phase = DiagnosticPhase.INTERACTIVE_BURNIN,
                    progress = (modules.size + 1).toFloat() / totalSteps
                )
            }

            DiagnosticPhase.INTERACTIVE_BURNIN -> {
                val totalSteps = modules.size + 4
                _state.value = _state.value.copy(
                    phase = DiagnosticPhase.INTERACTIVE_TOUCH,
                    progress = (modules.size + 2).toFloat() / totalSteps
                )
            }

            DiagnosticPhase.INTERACTIVE_TOUCH -> {
                val totalSteps = modules.size + 4
                _state.value = _state.value.copy(
                    phase = DiagnosticPhase.INTERACTIVE_VOLUME,
                    progress = (modules.size + 3).toFloat() / totalSteps
                )
            }

            DiagnosticPhase.INTERACTIVE_VOLUME -> generateCertificate()
            else -> Unit
        }
    }

    private fun generateCertificate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(phase = DiagnosticPhase.GENERATING_CERT, progress = 0.95f)

            // Обязательно загружаем цены здесь перед estimate.
            try {
                priceEstimator.loadPrices(context)
                Log.d("DiagVM", "Prices loaded, models: ${priceEstimator.getModelsCount()}")
            } catch (e: Exception) {
                Log.e("DiagVM", "Failed to load prices", e)
            }

            val overallScore = DiagnosticWeights.calculateOverall(results)
            val grade = DiagnosticWeights.gradeFromScore(overallScore)
            val deviceInfo = DeviceInfoCollector.collect(context)
            Log.d("DiagVM", "Device: ${deviceInfo.manufacturer} ${deviceInfo.model}")
            Log.d("DiagVM", "RAM: ${deviceInfo.ramGb}GB, Storage: ${deviceInfo.storageGb}GB")
            Log.d("DiagVM", "Score: $overallScore, Grade: $grade")

            val certId = "PP-${Year.now().value}-${UUID.randomUUID().toString().take(8).uppercase()}"
            val estimation = priceEstimator.estimate(deviceInfo, overallScore)
            Log.d("DiagVM", "Price: ${estimation.minPrice}-${estimation.maxPrice}")
            Log.d("DiagVM", "Match: ${estimation.matchedModel ?: "none"} (${estimation.source})")

            val certificate = Certificate(
                certId = certId,
                timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                device = deviceInfo,
                results = results.toList(),
                overallScore = overallScore,
                grade = grade,
                recommendedPriceMin = estimation.minPrice,
                recommendedPriceMax = estimation.maxPrice,
                matchedModel = estimation.matchedModel,
                priceSource = estimation.source,
                priceDbUpdated = estimation.dbUpdated
            )

            val json = Json.encodeToString(certificate)
            dao.insertSession(
                DiagnosticSessionEntity(
                    certId = certId,
                    timestamp = System.currentTimeMillis(),
                    deviceModel = "${deviceInfo.manufacturer} ${deviceInfo.model}",
                    overallScore = overallScore,
                    grade = grade,
                    certificateJson = json
                )
            )

            HapticManager.success(context)
            launch { SoundManager.playSuccess() }

            _state.value = _state.value.copy(
                phase = DiagnosticPhase.COMPLETED,
                progress = 1f,
                certificate = certificate,
                completedResults = results.toList()
            )
        }
    }
}
