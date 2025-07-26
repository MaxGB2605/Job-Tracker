package com.example.jobtracker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var db: JobDatabase
    private lateinit var jobRecyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var companyInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText

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
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val job = jobAdapter.getItemAt(position)
                // deleteJob(job, position)
                showDeleteConfirmation(job, position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
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

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(jobRecyclerView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        jobRecyclerView = findViewById(R.id.jobRecyclerView)
        searchInput = findViewById(R.id.searchInput)
        jobTitleInput = findViewById(R.id.jobTitleInput)
        companyInput = findViewById(R.id.companyInput)
        dateInput = findViewById(R.id.dateInput)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)

        // Set up RecyclerView
        jobRecyclerView.layoutManager = LinearLayoutManager(this)
        jobRecyclerView.setHasFixedSize(true)
        jobAdapter = JobAdapter(jobList)
        jobRecyclerView.adapter = jobAdapter

        // Initialize database
        db = JobDatabase.getDatabase(this)

        // Set up swipe to delete
        setUpSwipeToDelete()

        // Set up search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jobAdapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle search button on keyboard
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide the keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Load jobs from database
        loadJobs()

        // Set up save button click listener
        saveButton.setOnClickListener {
            val title = jobTitleInput.text.toString().trim()
            val company = companyInput.text.toString().trim()
            val date = dateInput.text.toString().trim()

            // Validate input
            if (title.isEmpty() || company.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val job = JobApplication(jobTitle = title, company = company, dateApplied = date)
            saveJob(job)

            // Clear input fields
            jobTitleInput.text?.clear()
            companyInput.text?.clear()
            dateInput.text?.clear()
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

    //Added July 26, 2025 delete confirmation
    private fun showDeleteConfirmation(job: JobApplication, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Job Application")
            .setMessage("Are you sure you want to delete this job application?")
            .setPositiveButton("Yes") { _, _ ->
                deleteJob(job, position)
            }
            .setNegativeButton("No!") { dialog, _ ->
                dialog.dismiss()
                //Refresh the list to make sure item is in the list
                jobAdapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun deleteJob(job: JobApplication, position: Int) {
        lifecycleScope.launch {
            try {
                //remove from database
                db.jobDao().delete(job)

                //update UI
                withContext(Dispatchers.Main) {
                    jobList.removeAt(position)
                    jobAdapter.notifyItemRemoved(position)
                    Toast.makeText(applicationContext, "Job Deleted!", Toast.LENGTH_SHORT).show()
                    loadJobs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Error Deleting Job: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadJobs()
                }
            }
        }
    }

    private fun loadJobs() {
        lifecycleScope.launch {
            val jobs = withContext(Dispatchers.IO) {
                db.jobDao().getAllJobs()
            }
            jobList.clear()
            jobList.addAll(jobs)
            jobAdapter.setItems(jobList)
        }
    }
}
