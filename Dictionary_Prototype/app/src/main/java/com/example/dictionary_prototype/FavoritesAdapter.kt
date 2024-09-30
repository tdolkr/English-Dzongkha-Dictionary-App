package com.example.dictionary_prototype
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary_prototype.R

class FavoritesAdapter(
    private val favoriteWords: MutableList<String>,
    private val onUnfavoriteClick: (String) -> Unit,
    private val onWordClick: (String) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.favorite_word_item, parent, false)
        return FavoritesViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val word = favoriteWords[position]
        holder.wordTextView.text = word

        // Handle un-favorite button click
        holder.unfavoriteIcon.setOnClickListener {
            onUnfavoriteClick(word)  // Trigger removal of the word from favorites
        }

        // Handle word click to get the meaning
        holder.wordTextView.setOnClickListener {
            onWordClick(word)
        }
    }

    override fun getItemCount(): Int = favoriteWords.size

    class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wordTextView: TextView = itemView.findViewById(R.id.favorite_word_text)
        val unfavoriteIcon: ImageView = itemView.findViewById(R.id.unfavorite_icon)
    }
}

