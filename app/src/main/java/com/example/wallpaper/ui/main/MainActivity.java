package com.example.wallpaper.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.wallpaper.R;
import com.example.wallpaper.ui.gallery.GalleryFragment;
import com.example.wallpaper.ui.mine.MineFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupBottomNavigation();
        
        if (savedInstanceState == null) {
            loadFragment(new GalleryFragment());
        }
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_gallery) {
                loadFragment(new GalleryFragment());
                return true;
            } else if (itemId == R.id.navigation_mine) {
                loadFragment(new MineFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }
}
