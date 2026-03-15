package com.phonepulse.feature.diagnostic.di

import com.phonepulse.feature.diagnostic.DiagnosticModule
import com.phonepulse.feature.diagnostic.modules.AudioDiagnostic
import com.phonepulse.feature.diagnostic.modules.BatteryDiagnostic
import com.phonepulse.feature.diagnostic.modules.CameraDiagnostic
import com.phonepulse.feature.diagnostic.modules.ConnectivityDiagnostic
import com.phonepulse.feature.diagnostic.modules.ControlsDiagnostic
import com.phonepulse.feature.diagnostic.modules.DisplayDiagnostic
import com.phonepulse.feature.diagnostic.modules.SensorsDiagnostic
import com.phonepulse.feature.diagnostic.modules.StorageDiagnostic
import com.phonepulse.feature.diagnostic.modules.WifiSpeedTest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticModulesProvider {

    @Provides
    @Singleton
    fun provideDiagnosticModules(
        battery: BatteryDiagnostic,
        display: DisplayDiagnostic,
        audio: AudioDiagnostic,
        camera: CameraDiagnostic,
        sensors: SensorsDiagnostic,
        connectivity: ConnectivityDiagnostic,
        storage: StorageDiagnostic,
        controls: ControlsDiagnostic,
        wifiSpeed: WifiSpeedTest
    ): List<DiagnosticModule> = listOf(
        battery,
        display,
        audio,
        camera,
        sensors,
        connectivity,
        storage,
        controls,
        wifiSpeed
    )
}
