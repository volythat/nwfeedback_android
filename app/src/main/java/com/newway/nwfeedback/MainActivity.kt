//package com.newway.nwfeedback
//
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.RequiresApi
//import androidx.core.app.ActivityCompat
//import com.newway.nwfeedback.databinding.ActivityMainBinding
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityMainBinding
//
//    private var allGrantedCallback: ((Boolean) -> Unit)? = null
//
//    private var permissionLauncher: ActivityResultLauncher<Array<String>> =
//        registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()) { result ->
//            var allAreGranted = true
//            for(b in result.values) {
//                allAreGranted = allAreGranted && b
//            }
//            allGrantedCallback?.let { it(true) }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        FeedbackFragment.setUp(this,
//            zendUrl = "change url",
//            zendAppId = "replace id",
//            zendClientId = "replace client id")
//
//        binding.btnRun.singleClick {
//            val dialog = FeedbackFragment.instance("replace id")
//            dialog.listener = object : FeedbackFragmentInterface {
//                override fun sentFeedbackSuccessfully() {
//                    Toast.makeText(this@MainActivity,"Sent successfully!",Toast.LENGTH_LONG).show()
//                }
//            }
//            requestPermissionsApp(dialog.permissions()){
//                if (it) {
//                    dialog.showDialog(this@MainActivity)
//                }else{
//                    Toast.makeText(this@MainActivity, "Permission denied!", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    private fun requestPermissionsApp(permissions:Array<String>,allGranted:(Boolean) -> Unit){
//        this.allGrantedCallback = allGranted
//        permissionLauncher.launch(permissions)
//    }
//
//}