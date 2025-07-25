package com.example.jobtracker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
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

    private val TAG = "JobTracker"

    private var jobList: MutableList<JobApplication> = mutableListOf()
    private lateinit var jobAdapter: JobAdapter


    fun setUpSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val job = jobAdapter.getItemAt(position)
                deleteJob(job, position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.RED)
                val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2

                if (dX > 0) { // Swiping to the right
                    background.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                    background.draw(c)

                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + deleteIcon.intrinsicWidth
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.draw(c)
                } else if (dX < 0) { // Swiping to the left
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.draw(c)
                } else { // View is unswiped
                    background.setBounds(0, 0, 0, 0)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(jobRecyclerView)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jobRecyclerView = findViewById(R.id.jobRecyclerView)
        jobRecyclerView.layoutManager = LinearLayoutManager(this)
        jobRecyclerView.setHasFixedSize(true)  // Add this line for better performance

        val jobTitleInput = findViewById<EditText>(R.id.jobTitleInput)
        val companyInput = findViewById<EditText>(R.id.companyInput)
        val dateInput = findViewById<EditText>(R.id.dateInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        db = JobDatabase.getDatabase(this)
        jobList = mutableListOf()  // Initialize the list
        jobAdapter = JobAdapter(jobList)
        jobRecyclerView.adapter = jobAdapter
        
        // Set up swipe to delete
        setUpSwipeToDelete()

        loadJobs()

        saveButton.setOnClickListener {
            val title = findViewById<EditText>(R.id.jobTitleInput).text.toString()
            val company = findViewById<EditText>(R.id.companyInput).text.toString()
            val date = findViewById<EditText>(R.id.dateInput).text.toString()

            val job = JobApplication(jobTitle = title, company = company, dateApplied = date)

            lifecycleScope.launch {
                db.jobDao().insert(job)
                loadJobs()
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Saved!", Toast.LENGTH_SHORT).show()
                    jobTitleInput.text.clear()
                    companyInput.text.clear()
                    dateInput.text.clear()
                }
            }
        }



    }

    private fun saveJob(job: JobApplication) {
        lifecycleScope.launch {
            db.jobDao().insert(job)
            loadJobs()
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Job Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun deleteJob(job: JobApplication, position: Int){
        lifecycleScope.launch{
            db.jobDao().delete(job)
            jobList.removeAt(position)
            jobAdapter.notifyItemRemoved(position)
            withContext(Dispatchers.Main){
                Toast.makeText(applicationContext, "Job deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadJobs() {
       lifecycleScope.launch{
           val jobs = withContext(Dispatchers.IO){
               db.jobDao().getAllJobs()
           }
           jobList.clear()
           jobList.addAll(jobs)
           jobAdapter.setItems(jobList)
       }
    }


}
