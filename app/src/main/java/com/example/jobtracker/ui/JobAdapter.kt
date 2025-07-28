package com.example.jobtracker.ui

import android.annotation.SuppressLint
import com.example.jobtracker.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jobtracker.data.JobApplication

class JobAdapter(
    var jobs: MutableList<JobApplication>,
    private var filteredJobs: MutableList<JobApplication> = jobs.toMutableList(),
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        val companyText: TextView = itemView.findViewById(R.id.companyText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val applicationMethodText: TextView = itemView.findViewById(R.id.applicationMethodText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = filteredJobs[position]
        holder.jobTitleText.text = job.jobTitle
        holder.companyText.text = job.company
        holder.dateText.text = job.dateApplied
        
        // Format the application method for display
        val appMethod = job.applicationMethod.name
            .lowercase()
            .split("_")
            .joinToString(" ") { it.capitalize() }
        holder.applicationMethodText.text = "• $appMethod"
    }

    override fun getItemCount() = filteredJobs.size

    fun getItemAt(position: Int): JobApplication {
        return filteredJobs[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<JobApplication>) {
        jobs = newItems.toMutableList()
        filteredJobs = newItems.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredJobs = if (query.isEmpty()) {
            jobs.toMutableList()
        } else {
            val lowerCaseQuery = query.lowercase()
            jobs.filter { job ->
                job.company?.lowercase()?.contains(lowerCaseQuery) == true ||
                        job.jobTitle?.lowercase()?.contains(lowerCaseQuery) == true ||
                        job.dateApplied?.lowercase()?.contains(lowerCaseQuery) == true
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        val job = filteredJobs[position]
        jobs.remove(job)
        filteredJobs.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, filteredJobs.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(position: Int, item: JobApplication) {
        jobs[position] = item
        notifyItemChanged(position)
    }
}