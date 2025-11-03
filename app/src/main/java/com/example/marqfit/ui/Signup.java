package com.example.marqfit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marqfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText confirmPassword = findViewById(R.id.confirmPassword);
        EditText nameInput = findViewById(R.id.name);
        Button signupButton = findViewById(R.id.signupButton);
        Button backToLoginButton = findViewById(R.id.backToLoginButton);

        signupButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String passwordConfirmation = confirmPassword.getText().toString().trim();
            String name = nameInput.getText().toString().trim();



            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(passwordConfirmation)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }


            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = result.getUser();
                        if (user == null) {
                            Toast.makeText(this, "Signup succeeded but user is null.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        user.updateProfile(new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build());
                        Map<String, Object> data = new HashMap<>();
                        data.put("name", name);
                        data.put("email", email);
                        data.put("createdAt", System.currentTimeMillis());

                        assert auth.getCurrentUser() != null;
                        db.collection("users").document(auth.getCurrentUser().getUid())
                                .set(data).addOnSuccessListener(vv -> {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, Login.class));
                            finish();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }).addOnFailureListener(e -> {
                Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });


        backToLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }
}
