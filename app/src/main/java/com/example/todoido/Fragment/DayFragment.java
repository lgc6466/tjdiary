package com.example.todoido.Fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todoido.Adapter.DayTaskAdapter;
import com.example.todoido.AlarmReceiver;
import com.example.todoido.R;
import com.example.todoido.ViewModel.DayTask;
import com.example.todoido.ViewModel.DayViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DayFragment extends Fragment {
    private BottomSheetBehavior bottomSheetBehavior;
    private FrameLayout bottomSheet;
    private FrameLayout bottomSheetCalendar;  // Add this line
    private BottomSheetBehavior bottomSheetBehaviorCalendar;  // Add this line
    private FrameLayout blackBackground;
    private Spinner spinner;
    private boolean isSheetVisible = false;
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    private final DatabaseReference databaseRef = firebaseUser != null ? FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid()).child("day") : null;
    private int selectedTaskPosition = -1;

    private void createBlackBackground() {
        blackBackground.setVisibility(View.VISIBLE);
    }

    private void removeBlackBackground() {
        blackBackground.setVisibility(View.GONE);
    }

   
    // 알림
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);
        bottomSheet = view.findViewById(R.id.sheet_day);
        bottomSheetCalendar = view.findViewById(R.id.sheet_day_calendar);  // Add this line
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehaviorCalendar = BottomSheetBehavior.from(bottomSheetCalendar);  // Add this line
        EditText day_txt = view.findViewById(R.id.day_txt);
        blackBackground = view.findViewById(R.id.blackBackground);
        CheckBox smartNotification = view.findViewById(R.id.smart_notification);

        spinner = view.findViewById(R.id.spinner);

        String[] items = new String[]{"선택 안 함", "5분 전", "10분 전", "30분 전", "1시간 전", "3시간 전"};

        ArrayAdapter<String> spinnerAdapter;
        spinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(spinnerAdapter);

        Button timePickerButton = view.findViewById(R.id.timePickerButton);
        Button timePickerButton2 = view.findViewById(R.id.timePickerButton2);
        MaterialButton addButton = view.findViewById(R.id.addButton);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);

                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                switch(item) {
                    case "5분 전":
                        calendar.add(Calendar.MINUTE, -5);
                        break;
                    case "10분 전":
                        calendar.add(Calendar.MINUTE, -10);
                        break;
                    case "30분 전":
                        calendar.add(Calendar.MINUTE, -30);
                        break;
                    case "1시간 전":
                        calendar.add(Calendar.HOUR_OF_DAY, -1);
                        break;
                    case "3시간 전":
                        calendar.add(Calendar.HOUR_OF_DAY, -3);
                        break;
                    default:
                        // "선택 안 함"의 경우 알림을 취소
                        if(alarmManager != null && pendingIntent != null) {
                            alarmManager.cancel(pendingIntent);
                        }
                        return;
                }

                // 현재 시간보다 알람 시간이 이전이라면 다음날로 설정
                if(calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                // 알람 설정
                if (alarmManager == null) {
                    alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                }
                Intent intent = new Intent(getActivity(), AlarmReceiver.class);
                intent.putExtra("channel_name", "spinnerSelection");
                intent.putExtra("channel_description", "text");

                // System.currentTimeMillis()를 사용하여 요청 코드 생성
                int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않은 경우 처리
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSheetVisible) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    isSheetVisible = false;
                    selectedTaskPosition = -1;  // BottomSheet가 닫힐 때마다 selectedTaskPosition 초기화
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    isSheetVisible = true;

                    // 아이템 선택이 아닌 addButton을 통해 BottomSheet를 열 경우, 기존 내용 초기화
                    if (selectedTaskPosition == -1) {
                        timePickerButton.setText("시작 시간");
                        timePickerButton2.setText("종료 시간");
                        day_txt.setText("");
                        spinner.setSelection(0);
                        smartNotification.setChecked(false);
                    }

                }
            }
        });

        View.OnClickListener timePickerClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button clickedButton = (Button) v;

                MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                        .setTheme(R.style.CustomTimePickerTheme)
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(00)
                        .setMinute(00)
                        .build();

                materialTimePicker.show(getChildFragmentManager(), "TIME_PICKER");

                materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int hour = materialTimePicker.getHour();
                        int minute = materialTimePicker.getMinute();
                        clickedButton.setText(String.format("%02d:%02d", hour, minute));
                        clickedButton.setTextColor(getResources().getColor(R.color.selected_tab_text_color));
                    }
                });
            }
        };

        timePickerButton.setOnClickListener(timePickerClickListener);
        timePickerButton2.setOnClickListener(timePickerClickListener);

        DayViewModel dayViewModel = new ViewModelProvider(this).get(DayViewModel.class);
        RecyclerView recyclerView = view.findViewById(R.id.dayRecyclerView);
        DayTaskAdapter adapter = new DayTaskAdapter(new ArrayList<>(), null);
        adapter.setOnItemClickListener(new DayTaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DayTask task) {
                // 항목 클릭 이벤트를 처리합니다.
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                isSheetVisible = true;
                timePickerButton.setText(task.getStartTime());
                timePickerButton2.setText(task.getEndTime());
                day_txt.setText(task.getText());
                spinner.setSelection(((ArrayAdapter<String>) spinner.getAdapter()).getPosition(task.getSpinnerSelection()));
                smartNotification.setChecked(task.isChecked());
                selectedTaskPosition = adapter.getTaskList().indexOf(task);

                // 토스트 메시지 추가
                Toast.makeText(getContext(), "카드를 길게 누르면 일정을 공유할 수 있습니다", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnHeaderClickListener(new DayTaskAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(DayTask header) {
                View includedLayout = view.findViewById(R.id.sheet_day_calendar);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(includedLayout);
                FrameLayout blackBackground = view.findViewById(R.id.blackBackground);

                if (behavior != null) {
                    if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        blackBackground.setVisibility(View.GONE);
                    } else {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        blackBackground.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        blackBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View includedLayout = view.findViewById(R.id.sheet_day_calendar);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(includedLayout);
                FrameLayout blackBackground = view.findViewById(R.id.blackBackground);

                if (behavior != null) {
                    if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        blackBackground.setVisibility(View.GONE);
                    } else if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        blackBackground.setVisibility(View.GONE);
                    }
                }
            }
        });

        bottomSheetBehaviorCalendar.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // BottomSheet가 열렸을 때, 검은 배경을 표시
                    createBlackBackground();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // BottomSheet가 닫혔을 때, 검은 배경을 감추기
                    removeBlackBackground();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 슬라이드 중에 추가적인 작업이 필요하면 여기에 구현
                if (slideOffset == 0.0) {
                    // 슬라이드가 완전히 닫혔을 때, 검은 배경을 감추기
                    removeBlackBackground();
                }
            }
        });


        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dayViewModel.getTaskList().observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTaskList(tasks);
            adapter.notifyDataSetChanged();
        });

        Button submitButton = view.findViewById(R.id.submit_btn);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String startTime = timePickerButton.getText().toString();
                String endTime = timePickerButton2.getText().toString();
                String text = day_txt.getText().toString();
                String spinnerSelection = spinner.getSelectedItem().toString();
                boolean isChecked = smartNotification.isChecked();

                // 텍스트가 비어 있는지 확인
                if (text.isEmpty()) {
                    // 텍스트가 비어 있다면 토스트 메시지를 표시하고 함수를 종료
                    Toast.makeText(getContext(), "일정을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 현재 날짜 정보를 가져오기
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일");
                String currentDate = sdf.format(new Date());

                DayTask task = new DayTask(currentDate, startTime, endTime, text, spinnerSelection, isChecked);

                if (selectedTaskPosition != -1) {
                    task.setId(adapter.getTaskList().get(selectedTaskPosition).getId());
                    dayViewModel.updateTask(task);

                    selectedTaskPosition = -1;
                } else {
                    // 날짜 정보를 헤더 생성에 전달
                    dayViewModel.addTask(task);
                }
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                isSheetVisible = false;

                timePickerButton.setText("00:00");
                timePickerButton2.setText("00:00");
                day_txt.setText("");
                spinner.setSelection(0);
                smartNotification.setChecked(false);
            }
        });


        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("seasonEffect");
        userRef.child("seasonEffect").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String seasonEffect = dataSnapshot.getValue(String.class);
                if (seasonEffect != null) {
                    switch (seasonEffect) {
                        case "spring":
                            spinner.setSelection(1);
                            break;
                        case "summer":
                            spinner.setSelection(2);
                            break;
                        case "fall":
                            spinner.setSelection(3);
                            break;
                        case "winter":
                            spinner.setSelection(4);
                            break;
                        case "none":
                        default:
                            spinner.setSelection(0);
                            break;
                    }
                } else {
                    spinner.setSelection(0);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 데이터를 불러오는데 실패했을 경우 동작
            }
        });

        return view;
}


}
