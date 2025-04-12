package com.bluex.mining;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Privacy Policy");
        }

        TextView privacyText = findViewById(R.id.privacyText);
        privacyText.setText(getPrivacyPolicyText());
    }

    private String getPrivacyPolicyText() {
        return "Privacy Policy for BlueX Mining App\n\n" +
               "Last updated: " + java.time.LocalDate.now() + "\n\n" +
               "1. Information We Collect\n" +
               "We collect the following information:\n" +
               "- Account information (email, phone number)\n" +
               "- Mining activity data\n" +
               "- Transaction history\n" +
               "- Device information\n\n" +
               "2. How We Use Your Information\n" +
               "We use your information to:\n" +
               "- Provide mining services\n" +
               "- Process transactions\n" +
               "- Improve app functionality\n" +
               "- Send important notifications\n\n" +
               "3. Data Storage and Security\n" +
               "- We use Firebase for secure data storage\n" +
               "- Your data is encrypted in transit and at rest\n" +
               "- We implement industry-standard security measures\n\n" +
               "4. Third-Party Services\n" +
               "We use the following third-party services:\n" +
               "- Firebase Authentication\n" +
               "- Firebase Realtime Database\n" +
               "- Google Analytics\n\n" +
               "5. Your Rights\n" +
               "You have the right to:\n" +
               "- Access your personal data\n" +
               "- Request data deletion\n" +
               "- Opt-out of marketing communications\n\n" +
               "6. Contact Us\n" +
               "For privacy-related questions, contact us at:\n" +
               "Email: support@bluexmining.com\n\n" +
               "7. Changes to This Policy\n" +
               "We may update this policy periodically. We will notify you of any changes.";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 