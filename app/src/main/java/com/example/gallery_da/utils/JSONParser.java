package com.example.gallery_da.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

public class JSONParser {
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    public static <T> T deserializer(InputStream stream, Type type) {
        T object = null;
        InputStreamReader sReader = null;
        JsonReader reader = null;

        try {
            sReader = new InputStreamReader(stream);
            reader = new JsonReader(sReader);

            object = gson.fromJson(reader, type);
        } finally {
            if (sReader != null) {
                try {
                    sReader.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }

        return object;
    }
}
