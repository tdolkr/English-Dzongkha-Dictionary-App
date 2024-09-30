package com.example.dictionary_prototype

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dictionary_prototype.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var dbHelper: SearchHistoryDatabaseHelper
    private var searchHistory: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "History"

        // Initialize the database helper
        dbHelper = SearchHistoryDatabaseHelper(this)

        // Initialize RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyAdapter = HistoryAdapter(searchHistory, ::onDeleteWord)
        binding.historyRecyclerView.adapter = historyAdapter

        // Load search history from SQLite database
        loadSearchHistory()

        // Clear all button click listener
        binding.clearAllBtn.setOnClickListener {
            clearAllHistory()
        }
    }

    // Handle back button click (use finish() for normal back behavior)
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Simply call finish() to go back to the previous activity without any slide animation
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Load search history from SQLite database
    private fun loadSearchHistory() {
        searchHistory = dbHelper.getAllWords().toMutableList()
        if (searchHistory.isEmpty()) {
            Toast.makeText(this, "No history to load", Toast.LENGTH_SHORT).show()
        } else {
            historyAdapter.updateData(searchHistory)
        }
    }

    // Clear all search history from the database
    private fun clearAllHistory() {
        dbHelper.clearHistory()
        searchHistory.clear()
        historyAdapter.updateData(searchHistory)
    }

    // Delete individual word from history
    private fun onDeleteWord(word: String) {
        dbHelper.deleteWord(word)
        searchHistory.remove(word)
        historyAdapter.updateData(searchHistory)
    }
}
