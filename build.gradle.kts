buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter() // Deprecated but might still be required for some older dependencies
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    }
}
