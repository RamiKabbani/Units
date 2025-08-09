package com.example.puzzledrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puzzledrop.game.GameViewModel
import com.example.puzzledrop.ui.GameScreen
import com.example.puzzledrop.ui.theme.PuzzleDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuzzleDropTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val vm: GameViewModel = viewModel()
                    GameScreen(viewModel = vm)
                }
            }
        }
    }
}