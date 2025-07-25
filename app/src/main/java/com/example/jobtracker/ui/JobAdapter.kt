package com.example.jobtracker.ui

import android.annotation.SuppressLint
import com.example.jobtracker.R

//noinspection SuspiciousImport
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jobtracker.data.JobApplication

class JobAdapter(var jobs: MutableList<JobApplication>) :
    RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

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
        position: Int
    ) {
        val job = jobs[position]
        holder.jobTitleText.text = job.jobTitle
        holder.companyText.text = job.company
        holder.dateText.text = job.dateApplied
    }

    override fun getItemCount() = jobs.size

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(newItems: List<JobApplication>) {
        jobs = newItems.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<JobApplication>) {
        jobs = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): JobApplication {
        return jobs[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(position: Int, item: JobApplication) {
        jobs[position] = item
        notifyItemChanged(position)
    }
}