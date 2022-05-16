package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel.AuthenticationState
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val SIGN_IN_RESULT_CODE = 1001
    }

    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityAuthenticationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        viewModel.authenticationState.observe(this, Observer { authenticationState ->
            if (authenticationState == AuthenticationState.AUTHENTICATED) {
                // When opening the activity for the second time, automatically redirect the user
                // to the screen with the list of saved locations
                // https://knowledge.udacity.com/questions/443481
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
            } else {
                // For the first time or if the user is not logged in, display the login button
                // and welcome message
                binding.loginButton.setOnClickListener {
                    launchSignInFlow()
                }
                binding.welcomeTextview.visibility = View.VISIBLE
                binding.loginButton.visibility = View.VISIBLE
            }
        })
    }

    /**
     * Use Firebase Authentication to prompt for registration/log in
     */
    private fun launchSignInFlow() {
        // Can use either an email address with a password, or a Google account
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            // Since the output image sample only changed the background and not the buttons,
            // instead of using AuthMethodPickerLayout, just customize the theme and logo
            .setTheme(R.style.LoginCustomTheme)
            .setLogo(R.drawable.map)
            .build()

        startActivityForResult(intent, SIGN_IN_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Only care about Firebase Authentication sign in operation
        if (requestCode != SIGN_IN_RESULT_CODE) return

        if (resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.error_happened), Toast.LENGTH_SHORT).show()
        }
    }
}
