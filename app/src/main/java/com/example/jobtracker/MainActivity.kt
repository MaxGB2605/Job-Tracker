package com.example.jobtracker

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobtracker.data.ApplicationMethod
import com.example.jobtracker.data.JobApplication
import com.example.jobtracker.data.JobDatabase
import com.example.jobtracker.databinding.ActivityMainBinding
import com.example.jobtracker.ui.JobAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: JobDatabase
    private lateinit var jobRecyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var fabAddJob: FloatingActionButton
    private lateinit var jobAdapter: JobAdapter
    private var jobList: MutableList<JobApplication> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database
        db = JobDatabase.getDatabase(this)

        // Set up RecyclerView
        jobRecyclerView = binding.jobRecyclerView
        jobRecyclerView.layoutManager = LinearLayoutManager(this)
        jobAdapter = JobAdapter(jobList)
        jobRecyclerView.adapter = jobAdapter

        // Set up swipe to delete
        setUpSwipeToDelete()

        // Set up search
        searchInput = binding.searchInput
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                jobAdapter.filter(s.toString())
            }
        })

        // Set up FAB
        fabAddJob = binding.fabAddJob
        fabAddJob.setOnClickListener {
            showAddJobDialog()
        }

        // Load jobs
        loadJobs()
        
        // Handle shared text intent
        handleSharedText()
    }

    private fun handleSharedText() {
        // Check if activity was started via a share intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            
            // Check if it's a LinkedIn job URL
            if (sharedText.contains("linkedin.com/jobs/view", ignoreCase = true)) {
                // Show loading state
                Toast.makeText(this, "Fetching job details...", Toast.LENGTH_SHORT).show()
                
                // Fetch and parse the LinkedIn job page
                lifecycleScope.launch {
                    try {
                        val (company, title) = fetchLinkedInJobDetails(sharedText)
                        if (company != null && title != null) {
                            runOnUiThread {
                                showAddJobDialog(company, title)
                            }
                        } else {
                            runOnUiThread {
                                // Fallback to basic extraction if parsing fails
                                val (fallbackCompany, fallbackTitle) = extractJobDetails(sharedText)
                                showAddJobDialog(fallbackCompany, fallbackTitle)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error fetching job details", Toast.LENGTH_SHORT).show()
                            val (company, title) = extractJobDetails(sharedText)
                            showAddJobDialog(company, title)
                        }
                    }
                }
            } else {
                // For non-LinkedIn URLs or text, use the existing extraction
                val (company, title) = extractJobDetails(sharedText)
                showAddJobDialog(company, title)
            }
        }
    }
    
    private suspend fun fetchLinkedInJobDetails(url: String): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val doc: Document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get()
                
                // Extract job title
                val titleElement = doc.selectFirst("h1.top-card-layout__title")
                val title = titleElement?.text()?.trim()
                
                // Extract company name
                val companyElement = doc.selectFirst("a.topcard__org-name-link")
                val company = companyElement?.text()?.trim()
                
                company to title
            } catch (e: Exception) {
                e.printStackTrace()
                null to null
            }
        }
    }
    
    private fun extractJobDetails(text: String): Pair<String?, String?> {
        // If it's a LinkedIn job URL, return nulls to trigger the full fetch
        if (text.contains("linkedin.com/jobs/view", ignoreCase = true)) {
            return null to null
        }
        
        // Existing extraction logic for non-URL text
        val patterns = listOf(
            // LinkedIn pattern: "Job Title at Company"
            "(.+?) at (.+?)(?:\\s*\\||\\s*\\n|\\s*\\r|\\s*https?://|\\s*$)".toRegex(),
            // Common pattern: "Company: X, Title: Y"
            "[Cc]ompany[\\s:]+([^\\n,]+)[,\\s]*[Tt]itle[\\s:]+([^\\n,]+)".toRegex(),
            // Common pattern: "Title: X, Company: Y"
            "[Tt]itle[\\s:]+([^\\n,]+)[,\\s]*[Cc]ompany[\\s:]+([^\\n,]+)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 3) {
                return match.groupValues[1].trim() to match.groupValues[2].trim()
            }
        }
        
        // Fallback: Try to extract from common formats
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size >= 2) {
            // If we have multiple lines, assume first line is title, second is company
            return lines[1].trim() to lines[0].trim()
        }
        
        return null to null
    }
    
    private fun showAddJobDialog(company: String? = null, title: String? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_job, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Initialize dialog views
        val companyInput = dialogView.findViewById<TextInputEditText>(R.id.companyInputDialog)
        val jobTitleInput = dialogView.findViewById<TextInputEditText>(R.id.jobTitleInputDialog)
        val applicationMethodDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.applicationMethodDropdown)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButtonDialog)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)
        
        // Pre-fill fields if data was provided
        company?.let { companyInput.setText(it) }
        title?.let { jobTitleInput.setText(it) }

        // Set up application method dropdown
        val applicationMethods = ApplicationMethod.values().map { 
            it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, applicationMethods)
        applicationMethodDropdown.setAdapter(adapter)
        applicationMethodDropdown.setText(applicationMethods[1], false) // Default to COMPANY_WEBSITE

        saveButton.setOnClickListener {
            val company = companyInput.text?.toString()?.trim()
            val jobTitle = jobTitleInput.text?.toString()?.trim()
            val selectedMethod = applicationMethodDropdown.text.toString()
                .uppercase().replace(" ", "_")

            if (company.isNullOrBlank() || jobTitle.isNullOrBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val applicationMethod = ApplicationMethod.valueOf(selectedMethod)
                val job = JobApplication(
                    jobTitle = jobTitle,
                    company = company,
                    applicationMethod = applicationMethod
                )
                saveJob(job)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveJob(job: JobApplication) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.jobDao().insert(job)
            }
            loadJobs()
        }
    }

    private fun loadJobs() {
        lifecycleScope.launch {
            val jobs = withContext(Dispatchers.IO) {
                db.jobDao().getAllJobs()
            }
            jobList.clear()
            jobList.addAll(jobs)
            jobAdapter.setItems(jobs)
        }
    }

    private fun deleteJob(job: JobApplication) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.jobDao().delete(job)
            }

            
            loadJobs()
        }
    }

    private fun setUpSwipeToDelete() {
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

    private fun showDeleteConfirmation(job: JobApplication, position: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Job Application")
            .setMessage("Are you sure you want to delete this job application?")
            .setPositiveButton("Yes") { _, _ ->
                deleteJob(job)
            }
            .setNegativeButton("No!") { dialog, _ ->
                dialog.dismiss()
                //Refresh the list to make sure item is in the list
                jobAdapter.notifyItemChanged(position)
            }
            .show()
    }
}
