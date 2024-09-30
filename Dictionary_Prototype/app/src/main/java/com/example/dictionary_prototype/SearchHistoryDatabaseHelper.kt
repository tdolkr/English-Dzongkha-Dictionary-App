package com.example.dictionary_prototype

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Define constants for the database
private const val DATABASE_NAME = "searchHistory.db"
private const val DATABASE_VERSION = 2  // Updated version
private const val TABLE_NAME = "history"
private const val COLUMN_ID = "id"
private const val COLUMN_WORD = "word"

class SearchHistoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WORD TEXT UNIQUE
            )
        """
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Insert a new word into the history without duplicates
    fun insertWord(word: String) {
        if (word.isNotBlank()) {  // Check if the word is not blank or empty
            val db = writableDatabase
            val contentValues = ContentValues().apply {
                put(COLUMN_WORD, word.trim())  // Trim leading/trailing spaces before inserting
            }
            db.insertWithOnConflict(
                TABLE_NAME,
                null,
                contentValues,
                SQLiteDatabase.CONFLICT_IGNORE  // Ignores the insert if duplicate
            )
            db.close()
        }
    }

    // Get all search history words
    fun getAllWords(): List<String> {
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        val words = mutableListOf<String>()

        if (cursor.moveToFirst()) {
            do {
                val word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD))
                words.add(word)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return words
    }

    // Delete a word from the history
    fun deleteWord(word: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_WORD=?", arrayOf(word))
        db.close()
    }

    // Clear all search history
    fun clearHistory() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}
