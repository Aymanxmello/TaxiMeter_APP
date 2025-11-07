package com.cmc.taximeter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout // New import for TextInputLayout

class AuthenticationActivity : AppCompatActivity() {

    // Déclaration des vues
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtName: EditText
    private lateinit var edtAge: EditText
    private lateinit var spinnerLicenseType: Spinner
    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var ibPassword: ImageButton
    private lateinit var txtSignUp: TextView
    private lateinit var txtSignIn: TextView

    // Nouveaux éléments pour le layout moderne
    private lateinit var tilName: TextInputLayout // Correctly initialized in onCreate
    private lateinit var tilAge: TextInputLayout // Correctly initialized in onCreate

    var isPassword = true

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        // Initialisation des vues
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtName = findViewById(R.id.edtName)
        edtAge = findViewById(R.id.edtAge)
        spinnerLicenseType = findViewById(R.id.spinnerLicenseType)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        txtSignUp = findViewById(R.id.txtSignUp)
        txtSignIn = findViewById(R.id.txtSignIn)
        ibPassword = findViewById(R.id.ibPassword)

        // Initialisation des TextInputLayouts (nouvelles vues pour l'inscription)
        // These MUST be initialized for the new modern XML
        tilName = findViewById(R.id.tilName)
        tilAge = findViewById(R.id.tilAge)


        ibPassword.setOnClickListener {
            togglePasswordVisibility()
        }


        // Afficher la vue de connexion au démarrage
        switchToSignIn()

        // Action pour afficher l'inscription
        txtSignUp.setOnClickListener {
            switchToSignUp()
        }

        txtSignIn.setOnClickListener {
            switchToSignIn()
        }

        // Action pour se connecter
        btnSignIn.setOnClickListener {
            signIn()
        }

        // Action pour s'inscrire
        btnSignUp.setOnClickListener {
            signUp()
        }

        // Adapter pour le spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.license_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLicenseType.adapter = adapter
    } // End of onCreate

    private fun togglePasswordVisibility() {

        if (isPassword) {
            // Afficher le texte normal
            edtPassword.inputType = InputType.TYPE_CLASS_TEXT
            ibPassword.setImageResource(R.drawable.visibility)
        } else {
            // Afficher comme mot de passe
            edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ibPassword.setImageResource(R.drawable.visibility_off)
        }

        isPassword = !isPassword
        edtPassword.setSelection(edtPassword.text.length)

    }

    // Fonction pour basculer vers l'écran d'inscription (Mise à jour pour le design moderne)
    private fun switchToSignUp() {
        // Logique pour le contrôle segmenté (Sign Up active)
        txtSignUp.setBackgroundResource(R.drawable.cercle) // Background for active tab
        txtSignUp.setTextColor(ContextCompat.getColor(this, R.color.black))
        txtSignIn.setBackgroundResource(0) // Transparent background for inactive tab
        txtSignIn.setTextColor(ContextCompat.getColor(this, R.color.grisSombre))

        // Afficher les champs d'inscription
        tilName.visibility = View.VISIBLE
        tilAge.visibility = View.VISIBLE
        spinnerLicenseType.visibility = View.VISIBLE

        // Afficher le bouton d'inscription
        btnSignUp.visibility = View.VISIBLE
        btnSignIn.visibility = View.GONE
    }

    private fun switchToSignIn() {
        // Logique pour le contrôle segmenté (Sign In active)
        txtSignIn.setBackgroundResource(R.drawable.cercle) // Background for active tab
        txtSignIn.setTextColor(ContextCompat.getColor(this, R.color.black))
        txtSignUp.setBackgroundResource(0) // Transparent background for inactive tab
        txtSignUp.setTextColor(ContextCompat.getColor(this, R.color.grisSombre))

        // Cacher les champs d'inscription
        tilName.visibility = View.GONE
        tilAge.visibility = View.GONE
        spinnerLicenseType.visibility = View.GONE

        // Afficher le bouton de connexion
        btnSignUp.visibility = View.GONE
        btnSignIn.visibility = View.VISIBLE
    }

    // Fonction de connexion
    private fun signIn() {
        // Récupérer les informations entrées par l'utilisateur
        val email = edtEmail.text.toString().lowercase()
        val password = edtPassword.text.toString()

        // Validation des champs
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val storedPassword = sharedPreferences.getString(email, null)

        if (storedPassword != null && storedPassword == password) {
            // Connexion réussie
            val editor = sharedPreferences.edit()
            editor.putString("loggedInUser", email)
            editor.apply()

            // Démarrer MainActivity après la connexion réussie
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Informations incorrectes
            Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
        }
    }


    // Fonction d'inscription
    private fun signUp() {
        val email = edtEmail.text.toString().lowercase()
        val password = edtPassword.text.toString()
        val name = edtName.text.toString()
        val age = edtAge.text.toString()
        val licenseType = spinnerLicenseType.selectedItem.toString()

        // Validation des champs
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || age.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val existingEmail = sharedPreferences.getString(email, null)

        if (existingEmail != null) {
            // Si l'email existe déjà
            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show()
            return
        }

        val editor = sharedPreferences.edit()
        editor.putString(email, password)
        editor.putString(email + "_name", name)
        editor.putString(email + "_age", age)
        editor.putString(email + "_email", email)
        editor.putString(email + "_licenseType", licenseType)
        editor.apply()

        Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()
        switchToSignIn()
    }
}