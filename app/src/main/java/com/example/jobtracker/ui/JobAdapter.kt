package com.example.jobtracker.ui

import android.annotation.SuppressLint
import com.example.jobtracker.R

//noinspection SuspiciousImport
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
//import androidx.compose.ui.text.intl.Locale
import androidx.recyclerview.widget.RecyclerView
import com.example.jobtracker.data.JobApplication
import java.util.Locale

class JobAdapter(
    var jobs: MutableList<JobApplication>,
    private var filteredJobs: MutableList<JobApplication> = jobs.toMutableList(),
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {


    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        val companyText: TextView = itemView.findViewById(R.id.companyText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: JobViewHolder,
        position: Int,
    ) {
        // val job = jobs[position]

        //Added to use filter/search field
        val job = filteredJobs[position]
        holder.jobTitleText.text = job.jobTitle
        holder.companyText.text = job.company
        holder.dateText.text = job.dateApplied
    }

//Added to return filtered list from search
    override fun getItemCount() = filteredJobs.size

    //override fun getItemCount() = jobs.size


    fun getItemAt(position: Int): JobApplication {
        //return jobs[position]

        //added to use filter/search field
        return filteredJobs[position]
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun removeItem(newItems: List<JobApplication>) {
//        jobs = newItems.toMutableList()
//        notifyDataSetChanged()
//    }


    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<JobApplication>) {
        jobs = newItems.toMutableList()

        ////added to use filter/search field
        filteredJobs = newItems.toMutableList()
        notifyDataSetChanged()
    }


    //added to use filter/search field
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


    //added to use filter/search field
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