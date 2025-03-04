package com.bluex.mining.api;

import com.bluex.mining.models.Withdrawal;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WithdrawalAPI {
    private final DatabaseReference mDatabase;
    private final APIKeyManager apiKeyManager;
    private APICallback callback;

    public interface APICallback {
        void onSuccess(List<Withdrawal> withdrawals);
        void onError(String error);
    }

    public WithdrawalAPI() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        apiKeyManager = new APIKeyManager();
    }

    public void getAllWithdrawals(String apiKey, APICallback callback) {
        apiKeyManager.validateAPIKey(apiKey, new APIKeyManager.APIKeyCallback() {
            @Override
            public void onSuccess(String validatedKey) {
                mDatabase.child("withdrawals")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            List<Withdrawal> withdrawals = new ArrayList<>();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Withdrawal withdrawal = snapshot.getValue(Withdrawal.class);
                                if (withdrawal != null) {
                                    withdrawals.add(withdrawal);
                                }
                            }
                            callback.onSuccess(withdrawals);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            callback.onError(databaseError.getMessage());
                        }
                    });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getPendingWithdrawals(String apiKey, APICallback callback) {
        apiKeyManager.validateAPIKey(apiKey, new APIKeyManager.APIKeyCallback() {
            @Override
            public void onSuccess(String validatedKey) {
                mDatabase.child("withdrawals")
                    .orderByChild("status")
                    .equalTo("pending")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            List<Withdrawal> withdrawals = new ArrayList<>();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Withdrawal withdrawal = snapshot.getValue(Withdrawal.class);
                                if (withdrawal != null) {
                                    withdrawals.add(withdrawal);
                                }
                            }
                            callback.onSuccess(withdrawals);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            callback.onError(databaseError.getMessage());
                        }
                    });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void updateWithdrawalStatus(String apiKey, String withdrawalId, 
                                     String newStatus, String reason, APICallback callback) {
        apiKeyManager.validateAPIKey(apiKey, new APIKeyManager.APIKeyCallback() {
            @Override
            public void onSuccess(String validatedKey) {
                DatabaseReference withdrawalRef = mDatabase.child("withdrawals").child(withdrawalId);
                withdrawalRef.child("status").setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        if (reason != null && !reason.isEmpty()) {
                            withdrawalRef.child("rejectionReason").setValue(reason);
                        }
                        List<Withdrawal> result = new ArrayList<>();
                        callback.onSuccess(result); // Empty list for success confirmation
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}