package com.example.ytdownloader

import android.app.Application
import android.util.Log

class YTDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global Exception Handler to prevent system crash dialogs
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("YTDownloaderApp", "FATAL CRASH CAUGHT: ", exception)
            // We caught the crash. We can prevent the system dialog by NOT calling the default handler,
            // or we can cleanly exit the process.
            
            // Clean exit without showing the "App has stopped" dialog
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }
}
