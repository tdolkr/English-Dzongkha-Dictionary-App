package com.example.dictionary_prototype;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DictionaryParser {

    private Map<String, Long> indexMap;
    private Context context;

    public DictionaryParser(Context context) {
        this.context = context;
        this.indexMap = new HashMap<>();
        try {
            loadIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadIndex() throws IOException {
        // Load the .idx file from assets as a binary stream
        InputStream idxStream = context.getAssets().open("en_dz.idx");

        try {
            while (true) {
                // Read the word (until null byte '\0')
                StringBuilder wordBuilder = new StringBuilder();
                int b;
                while ((b = idxStream.read()) != 0) {
                    if (b == -1) {
                        return;  // End of file
                    }
                    wordBuilder.append((char) b);
                }

                if (wordBuilder.length() == 0) {
                    break; // Break if no word is found (end of file)
                }

                // Read the offset (4 bytes, big-endian)
                long offset = readLong(idxStream);

                // Read the size (4 bytes, big-endian)
                int size = readInt(idxStream);

                // Store the word and its offset
                String word = wordBuilder.toString().trim(); // Trim to remove any extra spaces or newlines
                indexMap.put(word, offset);

                // Debug: Print the word, its offset, and size
                System.out.println("Loaded word: '" + word + "', offset: " + offset + ", size: " + size);
            }
        } finally {
            idxStream.close();
        }
    }

    public String getTranslation(String word) {
        // Check if word exists in the index
        Long offset = indexMap.get(word);  // Ensure the input matches the case and format in the .idx file
        if (offset == null) {
            System.out.println("Word not found in index map.");
            return "Translation not found";
        }

        try {
            // Load the .dict file from assets and seek to the correct offset
            InputStream dictStream = context.getAssets().open("en_dz.dict");
            dictStream.skip(offset);

            // Read the translation until the end of the entry (null byte '\0' indicates end)
            StringBuilder translationBuilder = new StringBuilder();
            int b;
            while ((b = dictStream.read()) != -1 && b != '\0') {
                translationBuilder.append((char) b);
            }

            dictStream.close();

            // Debug: Print the translation found
            System.out.println("Translation found: " + translationBuilder.toString());

            return translationBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading translation";
        }
    }

    // Reads 4 bytes from the stream and converts them to a long value
    private long readLong(InputStream stream) throws IOException {
        byte[] buffer = new byte[4];
        int bytesRead = stream.read(buffer);
        if (bytesRead != 4) {
            System.err.println("Error reading offset. Expected 4 bytes, got " + bytesRead);
        }
        // Convert 4 bytes to a long value (big-endian)
        return ((buffer[0] & 0xffL) << 24) | ((buffer[1] & 0xffL) << 16) | ((buffer[2] & 0xffL) << 8) | (buffer[3] & 0xffL);
    }

    // Reads 4 bytes from the stream and converts them to an int value
    private int readInt(InputStream stream) throws IOException {
        byte[] buffer = new byte[4];
        int bytesRead = stream.read(buffer);
        if (bytesRead != 4) {
            System.err.println("Error reading size. Expected 4 bytes, got " + bytesRead);
        }
        // Convert 4 bytes to an int value (big-endian)
        return ((buffer[0] & 0xff) << 24) | ((buffer[1] & 0xff) << 16) | ((buffer[2] & 0xff) << 8) | (buffer[3] & 0xff);
    }
}
