package com.example.amtpi

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import java.util.Locale
import com.google.android.material.switchmaterial.SwitchMaterial

class PersonalProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etUsername: EditText

    private lateinit var btnEditProfile: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var btnLogout: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var switchDarkMode: SwitchMaterial

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)


        auth = FirebaseAuth.getInstance()

        etUsername = findViewById(R.id.et_username)

        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnLogout = findViewById(R.id.btn_logout)
        languageSpinner = findViewById(R.id.spinner_language)
        switchDarkMode = findViewById(R.id.switch_dark_mode)

        loadUserData()

        setupLanguageSpinner()
        setupDarkModeSwitch()
        btnEditProfile.setOnClickListener { enableEditing(true) }
        btnSaveChanges.setOnClickListener { saveProfileChanges() }
        btnLogout.setOnClickListener { logoutUser() }
    }
    private fun setupDarkModeSwitch() {

        switchDarkMode.isChecked = sharedPreferences.getBoolean("dark_mode", false)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }


    private fun setupLanguageSpinner() {
        val languageOptions = resources.getStringArray(R.array.language_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter



        val languageCodes = resources.getStringArray(R.array.language_codes)
        val currentLangCode = sharedPreferences.getString("language_code", "es") ?: "es"
        val currentLangIndex = languageCodes.indexOf(currentLangCode)
        if (currentLangIndex != -1) {
            languageSpinner.setSelection(currentLangIndex)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLangCode = languageCodes[position]

                if (currentLangCode != selectedLangCode) {
                    setAppLocale(selectedLangCode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setAppLocale(languageCode: String) {

        sharedPreferences.edit().putString("language_code", languageCode).apply()
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)


        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }


    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            etUsername.setText(user.displayName)

        } else {
            Toast.makeText(this, "No se pudo cargar el usuario", Toast.LENGTH_SHORT).show()
            goToMainActivity()
        }
    }

    private fun enableEditing(isEditing: Boolean) {
        etUsername.isEnabled = isEditing
        if (isEditing) {
            btnEditProfile.visibility = View.GONE
            btnSaveChanges.visibility = View.VISIBLE
            etUsername.requestFocus()
        } else {
            btnEditProfile.visibility = View.VISIBLE
            btnSaveChanges.visibility = View.GONE
        }
    }

    private fun saveProfileChanges() {
        val newUsername = etUsername.text.toString().trim()
        if (newUsername.isEmpty()) {
            etUsername.error = "El nombre no puede estar vacío"
            return
        }
        val user = auth.currentUser
        val profileUpdates = userProfileChangeRequest { displayName = newUsername }
        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                enableEditing(false)
            } else {
                Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        goToMainActivity()
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
