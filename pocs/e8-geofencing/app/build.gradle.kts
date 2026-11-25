plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.geofencingpoc"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        applicationId = "com.monkeymart.geofencingpoc"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Fornece FusedLocationProviderClient (distância na tela) e GeofencingClient (entrada/saída da área).
    implementation("com.google.android.gms:play-services-location:21.4.0")
    // Bibliotecas dos testes que executam em um celular ou emulador Android.
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
