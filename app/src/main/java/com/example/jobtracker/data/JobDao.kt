package com.example.jobtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

//DAO - Data Access Object

@Dao
interface JobDao {
    @Insert
    suspend fun insert(job: JobApplication)

    @Query("SELECT * FROM job_table ORDER BY id DESC")
    suspend fun getAllJobs(): List<JobApplication>

    @Delete
    suspend fun delete(job: JobApplication)
}