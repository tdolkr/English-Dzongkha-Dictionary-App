package com.example.dictionary_prototype

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary_prototype.databinding.HistoryWordItemBinding

class HistoryAdapter(
    private var historyList: List<String>,
    private val onDeleteWord: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: HistoryWordItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.wordTextView.text = item

            // Handle delete button click
            binding.deleteIcon.setOnClickListener {
                onDeleteWord(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HistoryWordItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    // Update the history list when needed
    fun updateData(newHistoryList: List<String>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }
}
