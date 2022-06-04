package com.udacity.project4.locationreminders.data

import android.os.Parcelable
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.util.*

/**
 * data class acts as a data mapper between the DB and the UI
 */
@Parcelize
data class ReminderDataItem(
    var title: String?,
    var description: String?,
    var location: String?,
    var latitude: Double?,
    var longitude: Double?,
    val id: String = UUID.randomUUID().toString()
) : Parcelable {

    fun toDTO(): ReminderDTO {
        return ReminderDTO(
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            id = id
        )
    }
}