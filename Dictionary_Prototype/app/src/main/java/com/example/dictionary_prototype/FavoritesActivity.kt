package com.example.dictionary_prototype

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoritesAdapter
    private val favoriteWords: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Initialize Toolbar with a back button
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable the back button in the toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Favorites"


        // Retrieve favorite words passed from MainActivity
        favoriteWords.addAll(intent.getStringArrayListExtra("favoriteWords") ?: emptyList())

        // Set up RecyclerView to display favorite words
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with un-favorite and word click handling
        adapter = FavoritesAdapter(favoriteWords, this::onUnfavoriteClick, this::onWordClick)
        recyclerView.adapter = adapter
    }

    // Handle un-favorite click
    // Handle un-favorite click
    private fun onUnfavoriteClick(word: String) {
        // Remove the word from the favoriteWords list
        favoriteWords.remove(word)
        adapter.notifyDataSetChanged() // Update RecyclerView

        // Update SharedPreferences to remove the word
        val sharedPreferences = getSharedPreferences("FavoriteWords", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val savedFavorites = sharedPreferences.getStringSet("favoriteWords", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        if (savedFavorites.contains(word)) {
            savedFavorites.remove(word)
        }

        editor.putStringSet("favoriteWords", savedFavorites)
        editor.apply()

        // Pass updated favorites back to MainActivity without closing the FavoritesActivity
        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra("updatedFavorites", ArrayList(favoriteWords))
        setResult(RESULT_OK, resultIntent)
    }


    // Handle the word click
    private fun onWordClick(word: String) {
        // Pass the selected word back to MainActivity
        val resultIntent = Intent()
        resultIntent.putExtra("selectedWord", word) // Pass the selected word
        setResult(RESULT_OK, resultIntent)
        finish() // Close the activity and go back to MainActivity
    }

    // Handle the back button click in the toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finishWithResult()  // Explicitly navigate to MainActivity when back button is pressed
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Send the updated list back to MainActivity and finish the activity
    private fun finishWithResult() {
        val resultIntent = Intent(this, MainActivity::class.java)
        resultIntent.putStringArrayListExtra("updatedFavorites", ArrayList(favoriteWords))

        // Use FLAG_ACTIVITY_CLEAR_TOP to clear any activities on top of MainActivity
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        setResult(RESULT_OK, resultIntent)
        finish()  // End the activity and go back to MainActivity
    }
}
