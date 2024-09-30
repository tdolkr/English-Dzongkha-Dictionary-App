package com.example.dictionary_prototype

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dictionary_prototype.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.HashMap
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_FAVORITES = 1
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var adapter: MeaningAdapter
    private val favoriteWords: MutableSet<String> = mutableSetOf()
    private lateinit var dictionary: HashMap<String, String>
    private lateinit var favoritesActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var dbHelper: SearchHistoryDatabaseHelper // Declare dbHelper for SQLite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = SearchHistoryDatabaseHelper(this) // Initialize dbHelper


        favoritesActivityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedFavorites = result.data?.getStringArrayListExtra("updatedFavorites")
                val selectedWord = result.data?.getStringExtra("selectedWord")

                if (updatedFavorites != null) {
                    favoriteWords.clear()
                    favoriteWords.addAll(updatedFavorites)

                    // Save updated favorites to SharedPreferences
                    saveFavoritesToSharedPreferences()
                }
                if (selectedWord != null) {
                    // Search for the selected word when a word is tapped in the favorites list
                    searchWord(selectedWord)
                }
            }
        }

        binding.meaningRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MeaningAdapter(mutableListOf())
        binding.meaningRecyclerView.adapter = adapter
        binding.searchInput.showSoftInputOnFocus = true
        binding.searchInput.isLongClickable = false // Prevent long clicks (clipboard popup)
        binding.searchInput.setTextIsSelectable(false) // Disable text selection (clipboard)

        // Initialize Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize DrawerLayout and toggle
        drawerLayout = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set up menu icon click listener to open the navigation drawer
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START) // Opens the drawer from the left side
        }

        // Set up navigation drawer menu actions
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                R.id.nav_favorites -> {
                    // Launch FavoritesActivity when "Favorites" is selected
                    val intent = Intent(this, FavoritesActivity::class.java)
                    intent.putStringArrayListExtra("favoriteWords", ArrayList(favoriteWords)) // Pass favorite words
                    favoritesActivityLauncher.launch(intent)  // Use the new launcher here
                }
                R.id.nav_history -> {
                    // Launch HistoryActivity
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                }

                else -> {
                    // If a favorite word was clicked, search for that word
                    val word = it.title.toString()
                    searchWord(word)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Load dictionary data from the assets
        loadDictionary()

        // Add TextWatcher to listen for input changes (No suggestions)
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up search button click listener
        binding.searchBtn.setOnClickListener {
            val word = binding.searchInput.text.toString().trim().toLowerCase()
            searchWord(word)
        }

        // Load favorites from SharedPreferences
        loadFavoritesFromSharedPreferences()
    }

    // Load dictionary data from JSON file in assets
    private fun loadDictionary() {
        dictionary = HashMap()
        try {
            val inputStream: InputStream = assets.open("en-dz.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, StandardCharsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()

            // Log the number of keys loaded
            var wordCount = 0
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getString(key)
                dictionary[key.toLowerCase()] = value
                wordCount++
            }

            // Log total words loaded
            println("Total words loaded into the dictionary: $wordCount")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Search both local dictionary and the API
    private fun searchWord(word: String) {
        clearUI()

        if (word.isEmpty()) {
            displayNoResultsMessage(word)
            return
        }

        // Add to search history
        addToSearchHistory(word)

        // Search in the local Dzongkha dictionary (en-dz.json)
        val localDefinition = dictionary[word]
        if (localDefinition != null) {
            // Display the local definition
            displayLocalDefinition(localDefinition, word)
        } else {
            // If not found in the local dictionary, show "No results" for local data
            displayNoResultsMessage(word)
        }

        // Regardless of local search, proceed with API search
        getMeaning(word)
    }

    private fun addToSearchHistory(word: String) {
        dbHelper.insertWord(word)  // Add the word to the database
    }

    // Fallback to API call
    private fun getMeaning(word: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<List<WordResult>> = RetrofitInstance.dictionaryApi.getMeaning(word)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()

                        // Check if the API returned any valid result
                        if (responseBody.isNullOrEmpty()) {
                            // If no data was found from the API, display no results message
                            displayNoResultsMessage(word)
                        } else {
                            // Otherwise, display the result from the API
                            responseBody.firstOrNull()?.let { wordResult ->
                                setUI(wordResult)
                            }
                        }
                    } else {
                        // If the response is not successful, display no results
                        displayNoResultsMessage(word)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    displayError("An error occurred while searching for '$word': ${e.localizedMessage}")
                }
            }
        }
    }

    // Display the local definition in the WebView
    private fun displayLocalDefinition(definition: String, word: String) {
        val formattedDefinition = definition.split(" ").let { words ->
            if (words.size > 2) {
                "<span style='opacity:1;'>${words[0]}</span>&nbsp; " +
                        "<span style='opacity:0.4;'>${words[1]}</span> " +
                        "<br>" + words.subList(2, words.size).joinToString(" ")
            } else {
                "<span style='opacity:1;'>${words[0]}</span> " +
                        if (words.size == 2) "<span style='opacity:0.4;'>${words[1]}</span>&nbsp" else ""
            }
        }

        val boldDefinition = "<html><body><b>$formattedDefinition</b></body></html>"
        binding.definitionWebview.visibility = View.VISIBLE
        binding.definitionWebview.loadData(boldDefinition, "text/html", "UTF-8")
        binding.wordTextview.text = word

        // Show the favorite icon
        binding.favoriteIcon.visibility = View.VISIBLE
        updateFavoriteIcon(word)

        // Set up the favorite icon click listener
        binding.favoriteIcon.setOnClickListener {
            if (favoriteWords.contains(word)) {
                // Remove the word from favorites
                favoriteWords.remove(word)
                binding.favoriteIcon.setImageResource(R.drawable.ic_favorite_border)
                Toast.makeText(this, "$word removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                // Add the word to favorites
                favoriteWords.add(word)
                binding.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled)
                Toast.makeText(this, "$word added to favorites", Toast.LENGTH_SHORT).show()
            }

            // Save the updated favorites to SharedPreferences
            saveFavoritesToSharedPreferences()
        }
    }

    private fun displayNoResultsMessage(word: String) {
        binding.wordTextview.text = "No results available for '$word'"
        binding.phoneticTextview.visibility = View.GONE
        binding.definitionWebview.visibility = View.GONE
        binding.favoriteIcon.visibility = View.GONE
    }

    private fun displayError(message: String) {
        binding.wordTextview.text = message
        binding.phoneticTextview.visibility = View.GONE
        binding.definitionWebview.visibility = View.GONE
        binding.favoriteIcon.visibility = View.GONE
    }

    private fun clearUI() {
        // Clear the word and phonetic textviews
        binding.wordTextview.text = ""
        binding.phoneticTextview.visibility = View.GONE

        // Clear the WebView that displays the local definition
        binding.definitionWebview.visibility = View.GONE
        binding.definitionWebview.loadData("", "text/html", "UTF-8")

        // Clear the RecyclerView that displays the meanings from the API
        adapter.updateNewData(emptyList())

        // Hide the favorite icon
        binding.favoriteIcon.visibility = View.GONE
    }

    private fun setUI(response: WordResult) {
        // Update the word and phonetic
        binding.wordTextview.text = response.word

        if (!response.phonetic.isNullOrBlank()) {
            binding.phoneticTextview.text = response.phonetic
            binding.phoneticTextview.visibility = View.VISIBLE
        } else {
            binding.phoneticTextview.text = "Phonetic not available"
            binding.phoneticTextview.visibility = View.VISIBLE  // Make it visible with placeholder
        }

        // Update RecyclerView with meanings
        adapter.updateNewData(response.meanings)
        binding.meaningRecyclerView.visibility =
            View.VISIBLE  // Show the RecyclerView with the results

        // Show the favorite icon when definitions appear
        binding.favoriteIcon.visibility = View.VISIBLE

        // Check if the word is in favorites
        updateFavoriteIcon(response.word)
    }

    // Update favorite icon based on favorite state
    private fun updateFavoriteIcon(word: String) {
        if (favoriteWords.contains(word)) {
            binding.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled) // Filled favorite icon
        } else {
            binding.favoriteIcon.setImageResource(R.drawable.ic_favorite_border) // Unfilled favorite icon
        }
    }

    // Load favorites from SharedPreferences
    private fun loadFavoritesFromSharedPreferences() {
        val savedFavorites = getSharedPreferences("FavoriteWords", MODE_PRIVATE)
            .getStringSet("favoriteWords", emptySet())
        favoriteWords.clear()
        favoriteWords.addAll(savedFavorites ?: emptySet())
    }

    // Save favorites to SharedPreferences
    private fun saveFavoritesToSharedPreferences() {
        val sharedPreferences = getSharedPreferences("FavoriteWords", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet("favoriteWords", favoriteWords)
        editor.apply()
    }
}
