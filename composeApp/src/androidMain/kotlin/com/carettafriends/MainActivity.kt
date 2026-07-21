package com.carettafriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carettafriends.data.AndroidApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        AndroidApp.context = applicationContext
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
