package com.example.jobtracker

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobtracker.data.JobApplication
import com.example.jobtracker.data.JobDatabase
import com.example.jobtracker.ui.JobAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var db: JobDatabase
    private lateinit var jobRecyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
    private var jobList: List<JobApplication> = listOf()


    private val TAG = "JobTracker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jobRecyclerView = findViewById(R.id.jobRecyclerView)
        jobRecyclerView.layoutManager = LinearLayoutManager(this)

        val jobTitleInput = findViewById<EditText>(R.id.jobTitleInput)
        val companInput = findViewById<EditText>(R.id.companyInput)
        val dateInput = findViewById<EditText>(R.id.dateInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        db = JobDatabase.getDatabase(this)

        loadJobs()

        // val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val title = findViewById<EditText>(R.id.jobTitleInput).text.toString()
            val company = findViewById<EditText>(R.id.companyInput).text.toString()
            val date = findViewById<EditText>(R.id.dateInput).text.toString()

            val job = JobApplication(jobTitle = title, company = company, dateApplied = date)

            lifecycleScope.launch {
                db.jobDao().insert(job)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Saved!", Toast.LENGTH_SHORT).show()
                    jobTitleInput.text.clear()
                    companInput.text.clear()
                    dateInput.text.clear()
                }
            }
        }
    }

    private fun loadJobs() {
        lifecycleScope.launch {
            jobList = db.jobDao().getAlJobs()
            withContext(Dispatchers.Main) {
                jobAdapter = JobAdapter(jobList)
                jobRecyclerView.adapter = jobAdapter
            }
        }
    }


}




