package com.oddjobs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.oddjobs.app.ui.OddJobsApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OddJobsApp(vm) }
    }

    override fun onStop() {
        vm.flush()
        super.onStop()
    }
}
