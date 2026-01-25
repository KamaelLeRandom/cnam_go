package com.example.cnam_go;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnam_go.adapter.AuditeurAdapter;
import com.example.cnam_go.models.AuditeurCaptured;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CollectionActivity extends AppCompatActivity {
    private RecyclerView recyclerView;

    private List<AuditeurCaptured> auditeurList;

    private AuditeurAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        auditeurList = new ArrayList<>();
        adapter = new AuditeurAdapter(getApplicationContext(), auditeurList);
        recyclerView = findViewById(R.id.recyclerViewCaptured);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAuditeur();
    }

    private void loadAuditeur() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        boolean isLogged = prefs.getBoolean("isLogged", false);

        if (isLogged) {
            String userId = prefs.getString("userId", "");

            db.collection("Listener")
                    .whereEqualTo("UserId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        auditeurList.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String id = document.getId();
                            String code = document.getString("Code");
                            String name = document.getString("Name");
                            String nickname = document.getString("Nickname");
                            String catchDate = document.getString("CatchDate");
                            Long hp = document.getLong("Hp");
                            Long atk = document.getLong("Atk");
                            Long def = document.getLong("Def");
                            Long spd = document.getLong("Spd");

                            AuditeurCaptured auditeur = new AuditeurCaptured(
                                    id,
                                    code,
                                    name,
                                    hp,
                                    atk,
                                    def,
                                    spd,
                                    nickname,
                                    catchDate
                            );

                            auditeurList.add(auditeur);
                        }

                        adapter.notifyDataSetChanged();
                    });
        }
    }
}
