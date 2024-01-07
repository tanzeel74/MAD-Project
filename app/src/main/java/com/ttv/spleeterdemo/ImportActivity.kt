package com.ttv.spleeterdemo

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class ImportActivity : AppCompatActivity() {

    private var mLastFile: File? = null
    private val STORAGE_PERMISSION_CODE = 100
    private var wavpath: String? = null
    private var outpath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        val btnImport = findViewById<Button>(R.id.btnImport)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        if (!checkPermission()) {
            this.requestPermission()
        }

        btnImport.setOnClickListener {
            val fileChooser = FileChooser(
                this@ImportActivity,
                "Select wav file",
                FileChooser.DialogType.SELECT_FILE,
                mLastFile
            )
            val callback: FileChooser.FileSelectionCallback =
                FileChooser.FileSelectionCallback { file ->
                    mLastFile = file
                    try {
                        val wavPath = file.path
                        val outPath = file.parent + "/out"
                        val f = File(file.parent, "out")
                        if (!f.exists()) {
                            f.mkdirs()
                        }
                        val file1 = File(file.parent, "out")
                        val myFiles: Array<String> = file1.list()
                        for (i in myFiles.indices) {
                            val myFile = File(file1, myFiles[i])
                            myFile.delete()
                        }

                        this.wavpath = wavPath
                        this.outpath = outPath

                        runOnUiThread {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("wavPath", wavPath)
                            intent.putExtra("outPath", outPath)
                            startActivity(intent)
                        }


                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            fileChooser.show(callback)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            try {
                Log.d(ContentValues.TAG, "requestPermission: try")
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "requestPermission: ", e)
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Android is below 11(R)
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), STORAGE_PERMISSION_CODE
            )
        }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data.let {
                Log.e(ContentValues.TAG, "Uri : $it")
                val filePath = getFilePathFromUri(it!!)
                Log.e(ContentValues.TAG, "❌File Path::: $filePath:")

            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        var filePath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                filePath = it.getString(columnIndex)
            }
            it.close()
        }
        return filePath
    }

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(ContentValues.TAG, "storageActivityResultLauncher: ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //Android is 11(R) or above
                if (Environment.isExternalStorageManager()) {
                    Log.d(
                        ContentValues.TAG,
                        ": Manage External Storage Permission is granted"
                    )
                } else {
                    //Manage External Storage Permission is denied....
                    Log.d(
                        ContentValues.TAG,
                        ": Manage External Storage Permission is denied...."
                    )
                    toast("Manage External Storage Permission is denied....")
                }
            } else {
                //Android is below 11(R)
            }
        }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            Environment.isExternalStorageManager()
        } else {
            //Android is below 11(R)
            val write =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty()) {
                //check each permission if granted or not
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (write && read) {
                    //External Storage Permission granted
                    Log.d(ContentValues.TAG, ": External Storage Permission granted")

                } else {
                    //External Storage Permission denied...
                    Log.d(ContentValues.TAG, ": External Storage Permission denied...")
                    toast("External Storage Permission denied...")
                }
            }
        }
    }


    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}