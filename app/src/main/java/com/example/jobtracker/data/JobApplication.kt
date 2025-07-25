package com.example.jobtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "job_table")
data class JobApplication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobTitle: String,
    val company: String,
    val dateApplied: String
)