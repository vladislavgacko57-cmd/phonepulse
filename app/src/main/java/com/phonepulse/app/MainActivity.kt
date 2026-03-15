package com.phonepulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.phonepulse.app.navigation.AppNavigation
import com.phonepulse.core.database.dao.DiagnosticDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dao: DiagnosticDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide legacy ActionBar if it is present in this activity context.
        actionBar?.hide()

        enableEdgeToEdge()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0F1923)
            ) {
                AppNavigation(
                    context = this@MainActivity,
                    dao = dao
                )
            }
        }
    }
}
