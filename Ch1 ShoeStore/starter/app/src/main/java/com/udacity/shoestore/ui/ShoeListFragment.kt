package com.udacity.shoestore.ui

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.udacity.shoestore.R
import com.udacity.shoestore.databinding.FragmentShoeListBinding
import com.udacity.shoestore.models.Shoe

class ShoeListFragment : Fragment() {

    private val viewModel: ShoeListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentShoeListBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_shoe_list, container, false
        )

        // Requirement: list screen uses ScrollView and LinearLayout for showing a list of items
        // "Add a new layout item into the scrollview for each shoe"
        viewModel.shoeList.observe(viewLifecycleOwner, Observer { list ->
            binding.shoeList.removeAllViews()
            list.forEach { shoe ->
                binding.shoeList.addView(createShoeView(shoe))
            }
        })

        binding.addShoeFab.setOnClickListener { view ->
            view.findNavController().navigate(
                ShoeListFragmentDirections.actionShoeListFragmentToShoeDetailFragment()
            )
        }

        return binding.root
    }

    // Requirement: create a layout for the shoe item
    private fun createShoeView(data: Shoe): View {
        val shoeItem = TextView(ContextThemeWrapper(requireContext(), R.style.TextViewStyle))
        shoeItem.text = data.name
        // Convert padding from dp to pixel for the setPadding method
        val padding = resources.getDimensionPixelOffset(R.dimen.medium_padding)
        shoeItem.setPadding(padding)
        return shoeItem
    }
}