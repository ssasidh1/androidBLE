package com.example.maxim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maxim.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class CharacteristicsActivity extends AppCompatActivity {

    private ListView characteristicsListView;
    private ArrayList<HashMap<String, String>> displayList;
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characterisitics);

        // Initialize ListView and adapter
        characteristicsListView = findViewById(R.id.characteristics_list_view);
        displayList = new ArrayList<>();
        adapter = new SimpleAdapter(
                this,
                displayList,
                android.R.layout.simple_list_item_2,
                new String[]{"UUID", "Data"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        characteristicsListView.setAdapter(adapter);

        // Retrieve the initial characteristics list from the intent
        Intent intent = getIntent();
        ArrayList<HashMap<String, String>> initialList = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("CHARACTERISTICS_LIST");
        if (initialList != null) {
            displayList.addAll(initialList);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("ACTION_DATA_AVAILABLE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(dataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataUpdateReceiver);
    }

    private final BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateCharacteristicData(intent);
        }
    };

    private void updateCharacteristicData(Intent intent) {
        String uuid = intent.getStringExtra("UUID");
        String dataType = intent.getStringExtra("DATA_TYPE");

        if (uuid == null || dataType == null) return;

        // Search for the characteristic in the displayList
        for (HashMap<String, String> entry : displayList) {
            if (entry.get("UUID").equals(uuid)) {
                entry.put("Data", getDataString(intent, dataType));
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private String getDataString(Intent intent, String dataType) {
        switch (dataType) {
            case "BPM":
                return String.valueOf(intent.getIntExtra("DATA_SINGLE", 0));

            case "TEMP":
                return String.valueOf(intent.getIntExtra("DATA_SINGLE", 0));

            case "ECG":
                short[] ecgData = (short[]) intent.getSerializableExtra("DATA_ARRAY_BOXED");
                return ecgData != null ? Arrays.toString(ecgData) : "No Data";

            case "HRV":
                byte[] hrvData = intent.getByteArrayExtra("DATA_ARRAY_BYTE");
                return hrvData != null ? Arrays.toString(hrvData) : "No Data";

            case "QRS":
                short[] qrsData = (short[]) intent.getSerializableExtra("DATA_ARRAY_BOXED");
                return qrsData != null ? Arrays.toString(qrsData) : "No Data";

            default:
                return "N/A";
        }
    }
}
