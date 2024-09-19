package com.example.contactapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.contactapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech

    private val CONTACTS_READ_PERMISSION_CODE = 101
    private val TTS_REQUEST_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this, this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_READ_PERMISSION_CODE
            )
        } else {
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_READ_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initialization failed!")
        }
    }

    private fun loadContacts() {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            var count = 0
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                    val name =
                        it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    convertTextToSpeech(id, name)
                    count++
                } while (it.moveToNext() && count < 11) // Limit to the first 11 contacts
            }
        }
    }

    private fun convertTextToSpeech(id: String, text: String) {
        val folder = File(getExternalFilesDir(null), "ContactsSoundFiles")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "$id.mp3")

        // Create a temporary file to save the TTS output
        val tempFile = File.createTempFile("tts", ".mp3")
        val outputStream = FileOutputStream(tempFile)

        // Synthesize the text to speech and save to the temporary file
        textToSpeech.synthesizeToFile(text, null, tempFile, null)

        // Move the temporary file to the desired location
        tempFile.copyTo(file)

        // Delete the temporary file
        tempFile.delete()
    }

    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
