package com.example.dictionary_prototype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary_prototype.databinding.MeaningRecyclerRowBinding

class MeaningAdapter(private var meaningList: MutableList<Meaning>) : RecyclerView.Adapter<MeaningAdapter.MeaningViewHolder>() {

    class MeaningViewHolder(private val binding: MeaningRecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meaning: Meaning) {
            // Bind Part of Speech
            binding.partOfSpeechTextview.text = meaning.partOfSpeech

            // Bind Definitions
            binding.definitionsTextview.text = meaning.definitions.joinToString("\n\n") { definition ->
                val currentIndex = meaning.definitions.indexOf(definition)
                "${currentIndex + 1}. ${definition.definition}"
            }

            // Bind Synonyms (Hide if empty)
            if (meaning.synonyms.isEmpty()) {
                binding.synonymsTitleTextview.visibility = View.GONE
                binding.synonymsTextview.visibility = View.GONE
            } else {
                binding.synonymsTitleTextview.visibility = View.VISIBLE
                binding.synonymsTextview.visibility = View.VISIBLE
                binding.synonymsTextview.text = meaning.synonyms.joinToString(", ")
            }

            // Bind Antonyms (Hide if empty)
            if (meaning.antonyms.isEmpty()) {
                binding.antonymsTitleTextview.visibility = View.GONE
                binding.antonymsTextview.visibility = View.GONE
            } else {
                binding.antonymsTitleTextview.visibility = View.VISIBLE
                binding.antonymsTextview.visibility = View.VISIBLE
                binding.antonymsTextview.text = meaning.antonyms.joinToString(", ")
            }
        }
    }

    fun updateNewData(newMeaningList: List<Meaning>) {
        meaningList.clear()
        meaningList.addAll(newMeaningList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeaningViewHolder {
        val binding = MeaningRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MeaningViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return meaningList.size
    }

    override fun onBindViewHolder(holder: MeaningViewHolder, position: Int) {
        holder.bind(meaningList[position])
    }
}
