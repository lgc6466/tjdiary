package com.example.todoido;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.example.todoido.Fragment.DayFragment;
import com.example.todoido.Fragment.MonthFragment;
import com.example.todoido.Fragment.SettingFragment;
import com.example.todoido.Fragment.WeekFragment;
import com.example.todoido.Fragment.YearFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    DayFragment dayFragment;
    MonthFragment monthFragment;
    WeekFragment weekFragment;
    YearFragment yearFragment;
    int prevSelectedTab = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
            databaseRef.child("theme").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String theme = dataSnapshot.getValue(String.class);
                    if (theme == null) {  // 테마 정보가 null인 경우 기본 테마를 설정
                        theme = "Theme1";
                    }
                    switch (theme) {
                        case "Theme1":
                            setTheme(R.style.Theme1);  // 테마1 적용
                            break;
                        case "Theme2":
                            setTheme(R.style.Theme2);  // 테마2 적용
                            break;
                        case "Theme3":
                            setTheme(R.style.Theme3);  // 테마3 적용
                            break;
                        default:
                            setTheme(R.style.Theme1);  // 기본 테마 적용
                            break;
                    }

                    setContentView(R.layout.activity_main);

                    dayFragment = new DayFragment();
                    monthFragment = new MonthFragment();
                    weekFragment = new WeekFragment();
                    yearFragment = new YearFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, dayFragment).commit();

                    @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TabLayout tabLayout = findViewById(R.id.tab_layout);
                    tabLayout.addTab(tabLayout.newTab().setText("Day"));
                    tabLayout.addTab(tabLayout.newTab().setText("Week"));
                    tabLayout.addTab(tabLayout.newTab().setText("Month"));
                    tabLayout.addTab(tabLayout.newTab().setText("Year"));
                    tabLayout.addTab(tabLayout.newTab().setText("Set"));

                    tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            if (tab.getPosition() != 4) {
                                prevSelectedTab = tab.getPosition();
                            }

                            switch (tab.getPosition()) {
                                case 0:
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, dayFragment).commit();
                                    break;
                                case 1:
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, weekFragment).commit();
                                    break;
                                case 2:
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, monthFragment).commit();
                                    break;
                                case 3:
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, yearFragment).commit();
                                    break;
                                case 4:
                                    SettingFragment settingFragment = new SettingFragment(tabLayout, prevSelectedTab);
                                    getSupportFragmentManager().beginTransaction().replace(R.id.root_view_id, settingFragment).commit();
                                    break;
                            }
                        }


                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {
                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // 에러 처리
                }
            });
        } else {
            setTheme(R.style.Theme3);  // 기본 테마 적용
            setContentView(R.layout.activity_main);

        }
    }
}