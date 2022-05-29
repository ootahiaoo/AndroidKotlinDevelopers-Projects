package com.udacity.project4.locationreminders.reminderdescription

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataItem
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.utils.SingleLiveEvent
import kotlinx.coroutines.launch

class ReminderDescriptionViewModel(
    app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {

    val reminder = MutableLiveData<ReminderDataItem>()

    fun setReminder(reminder: ReminderDataItem) {
        this.reminder.postValue(reminder)
    }

    fun deleteReminder(reminderId: String) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.deleteReminder(reminderId)
            showLoading.postValue(false)
            finishActivity.postValue(true)
        }
    }

    val finishActivity: SingleLiveEvent<Boolean> = SingleLiveEvent()
}