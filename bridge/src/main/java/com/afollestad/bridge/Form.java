package com.afollestad.bridge;

import android.support.annotation.NonNull;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Form {

    public static class Entry {

        public final String name;
        public final Object value;

        public Entry(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public Form() {
        mEntries = new ArrayList<>();
        mEncoding = "UTF-8";
    }

    public Form(@NonNull String encoding) {
        mEntries = new ArrayList<>();
        mEncoding = encoding;
    }

    private final String mEncoding;
    private final List<Entry> mEntries;

    public Form add(String name, Object value) {
        mEntries.add(new Entry(name, value));
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < mEntries.size(); i++) {
            if (i > 0) result.append("&");
            final Entry entry = mEntries.get(i);
            try {
                result.append(URLEncoder.encode(entry.name, mEncoding));
                result.append("=");
                result.append(URLEncoder.encode(entry.value + "", mEncoding));
            } catch (Exception e) {
                // This should never happen
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }
}
