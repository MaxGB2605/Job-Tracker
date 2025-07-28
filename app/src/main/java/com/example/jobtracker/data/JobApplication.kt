package com.example.jobtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class ApplicationMethod{
    //Added July 28, 2025 to support drop down for the application method
    EASY_APPLY,
    COMPANY_WEBSITE,
    EXTERNAL_LINK,
    OTHER
}


@Entity(tableName = "job_table")
data class JobApplication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobTitle: String,
    val company: String,
    //val dateApplied: String

    //Added July 28, 2025 to automatically get and attach the date from the device
    val dateApplied: String = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date()),
    val applicationMethod: ApplicationMethod = ApplicationMethod.COMPANY_WEBSITE
)