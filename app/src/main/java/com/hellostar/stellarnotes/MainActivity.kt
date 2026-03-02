package com.hellostar.stellarnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.hellostar.stellarnotes.data.NoteDatabase
import com.hellostar.stellarnotes.data.NoteRepository
import com.hellostar.stellarnotes.ui.AppPreferences
import com.hellostar.stellarnotes.ui.StellarApp
import com.hellostar.stellarnotes.ui.StellarViewModel
import com.hellostar.stellarnotes.ui.StellarViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val appPrefs = AppPreferences(applicationContext)
        
        setContent { AppContent(appPrefs) }
    }

    @Composable
    private fun AppContent(appPrefs: AppPreferences) {
        val db = remember {
            Room.databaseBuilder(
                applicationContext,
                NoteDatabase::class.java,
                "stellar-notes.db"
            ).fallbackToDestructiveMigration().build()
        }
        val repo = remember { NoteRepository(db.noteDao()) }
        val vm: StellarViewModel = viewModel(
            factory = StellarViewModelFactory(repo)
        )
        StellarApp(viewModel = vm, appPrefs = appPrefs)
    }
}
