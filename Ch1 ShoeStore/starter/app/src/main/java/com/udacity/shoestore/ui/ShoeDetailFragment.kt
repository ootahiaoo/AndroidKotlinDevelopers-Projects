package com.udacity.shoestore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import com.udacity.shoestore.R
import com.udacity.shoestore.databinding.FragmentShoeDetailBinding

class ShoeDetailFragment : Fragment() {

    private val viewModel: ShoeListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentShoeDetailBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_shoe_detail, container, false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.shoeName.observe(viewLifecycleOwner, Observer {
            binding.nameEdittext.text
        })

        // Used by both the Cancel and Save buttons
        viewModel.eventReturnToList.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                NavHostFragment.findNavController(this).navigate(
                    ShoeDetailFragmentDirections.actionShoeDetailFragmentToShoeListFragment()
                )
                viewModel.onReturnToListComplete()
            }
        })

        viewModel.eventShowErrorToast.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                Toast.makeText(context, getString(R.string.error_empty_name), Toast.LENGTH_SHORT)
                    .show()
                viewModel.onShowErrorToastComplete()
            }
        })
        return binding.root
    }
}