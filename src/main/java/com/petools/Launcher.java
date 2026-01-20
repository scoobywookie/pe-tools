package com.petools;

public class Launcher {
    public static void main(String[] args) {
        // --- FIX: Force Software Rendering to prevent WebView Crashes ---
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        // This tricks the computer into loading the app correctly
        App.main(args);
    }
}