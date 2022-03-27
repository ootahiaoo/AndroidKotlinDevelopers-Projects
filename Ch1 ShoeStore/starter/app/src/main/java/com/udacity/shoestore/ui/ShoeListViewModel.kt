package com.udacity.shoestore.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.udacity.shoestore.models.Shoe

// Requirement: use an activity level ViewModel
// shared by all the fragments including the ShoeDetailFragment
class ShoeListViewModel : ViewModel() {

    // Requirement: all entries will be saved only as long as the app is running.
    // So no need to use a database
    private val _shoeList = MutableLiveData<List<Shoe>>()
    val shoeList: LiveData<List<Shoe>>
        get() = _shoeList

    // Use MutableLiveData for two-way binding with EditText
    // https://stackoverflow.com/questions/61424074/cannot-find-symbol-class-data-binding-impl
    val shoeName = MutableLiveData<String>("")

    val shoeCompany = MutableLiveData<String>("")

    val shoeSize = MutableLiveData<String>("")

    val shoeDescription = MutableLiveData<String>("")
    //TODO: implement form validation with Flow
    // https://levelup.gitconnected.com/using-flows-for-form-validation-in-android-79016b00c079
    // https://proandroiddev.com/should-we-choose-kotlins-stateflow-or-sharedflow-to-substitute-for-android-s-livedata-2d69f2bd6fa5
    // https://blog.mindorks.com/a-complete-guide-to-learn-kotlin-for-android-development-b1e5d23cc2d8

    private val _eventReturnToList = MutableLiveData<Boolean>(false)
    val eventReturnToList: LiveData<Boolean>
        get() = _eventReturnToList

    private val _eventShowErrorToast = MutableLiveData<Boolean>(false)
    val eventShowErrorToast: LiveData<Boolean>
        get() = _eventShowErrorToast

    init {
        makeDefaultList()
    }

    private fun makeDefaultList() {
        val list = mutableListOf<Shoe>()
        repeat(5) {
            val shoe = Shoe(
                name = "Shoe $it",
                size = 0.00,
                company = "Shoe Company",
                description = "This is a nice pair of shoes.",
                images = listOf<String>()
            )
            list.add(shoe)
        }

        _shoeList.value = list
    }

    fun cancelShoeCreation() {
        clearShoePlaceholder()
        _eventReturnToList.value = true
    }

    fun addShoeToList() {
        // Don't add a new shoe if name is empty
        // (Don't check for others because the list only displays the shoe name)
        if (shoeName.value.isNullOrBlank()) {
            _eventShowErrorToast.value = true
            return
        }

        val shoe = Shoe(
            name = shoeName.value!!,
            // The edit text input type only allows numbers
            size = if (shoeSize.value.isNullOrBlank()) 0.00 else shoeSize.value!!.toDouble(),
            company = shoeCompany.value ?: "",
            description = shoeDescription.value ?: "",
            images = listOf<String>()
        )

        // Add shoe to the list, or create a list with the shoes if the list is empty
        _shoeList.value = _shoeList.value?.plus(shoe) ?: listOf(shoe)

        clearShoePlaceholder()

        _eventReturnToList.value = true
    }

    // Since fragments share the same ViewModel, clear the values after closing the detail screen
    private fun clearShoePlaceholder() {
        shoeName.value = ""
        shoeSize.value = ""
        shoeCompany.value = ""
        shoeDescription.value = ""
    }

    fun onReturnToListComplete() {
        _eventReturnToList.value = false
    }

    fun onShowErrorToastComplete() {
        _eventShowErrorToast.value = false
    }
}