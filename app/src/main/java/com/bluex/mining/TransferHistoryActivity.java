package com.bluex.mining;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TransferHistoryActivity extends AppCompatActivity {
    private ListView transferHistoryListView;
    private DatabaseReference mDatabase;
    private List<String> transferHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_history);

        transferHistoryListView = findViewById(R.id.transferHistoryListView);
        mDatabase = FirebaseDatabase.getInstance().getReference("transfers");
        transferHistory = new ArrayList<>();

        fetchTransferHistory();
    }

    private void fetchTransferHistory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                transferHistory.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String transferDetails = snapshot.getValue(String.class); // Adjust based on your data structure
                    transferHistory.add(transferDetails);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TransferHistoryActivity.this, android.R.layout.simple_list_item_1, transferHistory);
                transferHistoryListView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TransferHistoryActivity.this, "Failed to load transfer history.", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 