public class WithdrawalClient {
    private static final String API_KEY = "your_secure_api_key_here";
    private WithdrawalAPI api;

    public WithdrawalClient() {
        api = new WithdrawalAPI();
    }

    public void fetchPendingWithdrawals() {
        api.getPendingWithdrawals(API_KEY, new WithdrawalAPI.APICallback() {
            @Override
            public void onSuccess(List<Withdrawal> withdrawals) {
                // Handle withdrawals
                for (Withdrawal withdrawal : withdrawals) {
                    System.out.println("Withdrawal ID: " + withdrawal.getId());
                    System.out.println("Amount: " + withdrawal.getAmount());
                    System.out.println("Status: " + withdrawal.getStatus());
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("Error: " + error);
            }
        });
    }

    public void approveWithdrawal(String withdrawalId) {
        api.updateWithdrawalStatus(API_KEY, withdrawalId, "approved", null, 
            new WithdrawalAPI.APICallback() {
                @Override
                public void onSuccess(List<Withdrawal> withdrawals) {
                    System.out.println("Withdrawal approved successfully");
                }

                @Override
                public void onError(String error) {
                    System.err.println("Error approving withdrawal: " + error);
                }
            });
    }

    public void rejectWithdrawal(String withdrawalId, String reason) {
        api.updateWithdrawalStatus(API_KEY, withdrawalId, "rejected", reason, 
            new WithdrawalAPI.APICallback() {
                @Override
                public void onSuccess(List<Withdrawal> withdrawals) {
                    System.out.println("Withdrawal rejected successfully");
                }

                @Override
                public void onError(String error) {
                    System.err.println("Error rejecting withdrawal: " + error);
                }
            });
    }
} 