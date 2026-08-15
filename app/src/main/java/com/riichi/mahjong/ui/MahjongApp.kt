package com.riichi.mahjong.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riichi.mahjong.ui.theme.RiichiMahjongTheme

private enum class Screen { INPUT, RESULT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahjongApp(vm: ScorerViewModel = viewModel()) {
    RiichiMahjongTheme {
        var screen by remember { mutableStateOf(Screen.INPUT) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (screen == Screen.INPUT) "立直麻将点数计算" else "计算结果",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        if (screen == Screen.RESULT) {
                            IconButton(onClick = { screen = Screen.INPUT }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (screen) {
                    Screen.INPUT -> InputScreen(
                        vm = vm,
                        onCalculate = {
                            vm.calculate()
                            if (vm.result != null) screen = Screen.RESULT
                        },
                    )
                    Screen.RESULT -> ResultScreen(
                        outcome = vm.result ?: return@Box,
                        onBack = { screen = Screen.INPUT },
                    )
                }
            }
        }
    }
}
