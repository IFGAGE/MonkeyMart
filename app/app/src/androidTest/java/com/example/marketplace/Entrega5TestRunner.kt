package com.example.marketplace

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Exclusivo dos testes instrumentados: nenhuma requisição usa o Firebase real.
class Entrega5TestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        FirebaseApp.getApps(targetContext).forEach { it.delete() }
        FirebaseApp.initializeApp(targetContext, FirebaseOptions.Builder()
            .setProjectId("demo-monkeymart")
            .setApplicationId("1:123456789:android:entrega5")
            .setApiKey("fake-api-key")
            .build())
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 19099)
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 18080)
        super.onCreate(arguments)
    }
}
