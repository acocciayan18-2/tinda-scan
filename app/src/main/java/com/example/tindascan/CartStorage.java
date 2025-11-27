package com.example.tindascan;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartStorage {
    private static final String PREF_NAME = "cart_storage";
    private static final String KEY_CART_ITEMS = "cart_items";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public CartStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ✅ CHANGED to save List<CartItem>
    public void saveCart(List<CartItem> cartItems) {
        String json = gson.toJson(cartItems);
        prefs.edit().putString(KEY_CART_ITEMS, json).apply();
    }

    // ✅ CHANGED to return List<CartItem>
    public List<CartItem> loadCart() {
        String json = prefs.getString(KEY_CART_ITEMS, null);
        if (json == null) return new ArrayList<>();

        // ✅ CHANGED to reflect List<CartItem>
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void clearCart() {
        prefs.edit().remove(KEY_CART_ITEMS).apply();
    }
}