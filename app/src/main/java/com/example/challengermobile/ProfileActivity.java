package com.example.challengermobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth     mAuth;
    private FirebaseFirestore db;
    private ImageView        ivAvatar;
    private TextView         tvName, tvEmail;
    private Button           btnChangePwd;
    private LinearLayout     teamsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        ivAvatar       = findViewById(R.id.ivAvatar);
        tvName         = findViewById(R.id.tvName);
        tvEmail        = findViewById(R.id.tvEmail);
        btnChangePwd   = findViewById(R.id.btnChangePwd);
        teamsContainer = findViewById(R.id.teamsContainer);

        loadUserInfo();
        loadUserTeams();

        btnChangePwd.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            name = user.getEmail().split("@")[0];
        }
        tvName.setText(name);
        tvEmail.setText(user.getEmail());

        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_change_password, null, false);

        EditText etCurrent = dialogView.findViewById(R.id.etCurrentPwd);
        EditText etNew     = dialogView.findViewById(R.id.etNewPwd);
        EditText etConfirm = dialogView.findViewById(R.id.etConfirmPwd);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            ok.setOnClickListener(v2 -> {
                String current = etCurrent.getText().toString();
                String neu     = etNew.getText().toString();
                String confirm = etConfirm.getText().toString();

                if (TextUtils.isEmpty(current) || TextUtils.isEmpty(neu) || TextUtils.isEmpty(confirm)) {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!neu.equals(confirm)) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) return;

                AuthCredential cred = EmailAuthProvider
                        .getCredential(user.getEmail(), current);
                user.reauthenticate(cred)
                        .addOnSuccessListener(a -> user.updatePassword(neu)
                                .addOnSuccessListener(b -> {
                                    Toast.makeText(this,
                                            "Password changed", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Update failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()
                                )
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        "Current password incorrect",
                                        Toast.LENGTH_SHORT).show()
                        );
            });
        });

        dialog.show();
    }

    private void loadUserTeams() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .collection("teams")
                .get()
                .addOnSuccessListener(qs -> {
                    for (var doc : qs.getDocuments()) {
                        String teamId   = doc.getId();
                        String teamName = doc.getString("teamName");
                        addTeamItem(teamId, teamName, "Owner");
                    }
                });
    }

    private void addTeamItem(String teamId, String name, String role) {
        View item = getLayoutInflater()
                .inflate(R.layout.item_profile_team, teamsContainer, false);
        TextView tvTeam = item.findViewById(R.id.tvTeamName);
        TextView tvRole = item.findViewById(R.id.tvTeamRole);
        tvTeam.setText(name);
        tvRole.setText(role);
        item.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, TeamDashboardActivity.class);
            i.putExtra(TeamDashboardActivity.EXTRA_TEAM_ID, teamId);
            startActivity(i);
        });
        teamsContainer.addView(item);
    }
}