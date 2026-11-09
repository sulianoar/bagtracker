package com.stansa.bagtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.stansa.bagtracker.databinding.ActivityMainBinding
import com.stansa.bagtracker.util.NotificationUtil
import com.stansa.bagtracker.worker.ExpiryNotificationWorker
import com.stansa.bagtracker.worker.ReminderNotificationWorker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        NotificationUtil.createChannels(this)
        ExpiryNotificationWorker.schedule(this)
        ReminderNotificationWorker.schedule(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        // Deep link bagtracker://item/{id}/{name}
        intent?.data?.let { uri ->
            if (uri.scheme == "bagtracker" && uri.host == "item") {
                val itemId = uri.pathSegments.getOrNull(0)?.toLongOrNull()
                if (itemId != null) {
                    val bundle = Bundle().apply { putLong("itemId", itemId) }
                    navController.navigate(R.id.itemDetailFragment, bundle)
                }
            }
        }
    }
}