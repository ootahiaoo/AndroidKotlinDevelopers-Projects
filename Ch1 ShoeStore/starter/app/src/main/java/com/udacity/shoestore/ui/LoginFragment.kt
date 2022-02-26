package com.udacity.shoestore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.udacity.shoestore.R
import com.udacity.shoestore.databinding.FragmentLoginBinding

/**
 * Login/register screen, which does not actually login or register
 */
class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentLoginBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_login, container, false
        )

        binding.registerButton.setOnClickListener { view ->
            view.findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
            )
        }
        binding.loginButton.setOnClickListener { view ->
            view.findNavController().navigate(
                LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
            )
        }

        return binding.root
    }
}