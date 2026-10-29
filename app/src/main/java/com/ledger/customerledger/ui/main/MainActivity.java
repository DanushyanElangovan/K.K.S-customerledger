package com.ledger.customerledger.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.ActivityMainBinding;
import com.ledger.customerledger.reminder.ReminderScheduler;

/** Hosts the four tabs: Home, Customers, Cheques, Billing. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) binding.bottomNav.setSelectedItemId(R.id.nav_home);

        requestNotificationPermission();
        ReminderScheduler.scheduleDaily(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBadge();
    }

    private void showTab(int menuId) {
        Fragment fragment;
        if (menuId == R.id.nav_customers) fragment = new CustomersFragment();
        else if (menuId == R.id.nav_cheques) fragment = new ChequesFragment();
        else if (menuId == R.id.nav_billing) fragment = new BillingFragment();
        else fragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    /** Red number on the Cheques tab: cheques due within 2 days or overdue. */
    public void refreshBadge() {
        int count = LedgerRepository.get(this).getDueSoon().size();
        BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.nav_cheques);
        if (count > 0) {
            badge.setNumber(count);
            badge.setVisible(true);
        } else {
            binding.bottomNav.removeBadge(R.id.nav_cheques);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }
}