package com.example.cnam_go.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnam_go.R;
import com.example.cnam_go.models.AuditeurCaptured;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditeurAdapter extends RecyclerView.Adapter<AuditeurAdapter.ViewHolder> {
    private List<AuditeurCaptured> auditeurs;
    private Context context;

    public AuditeurAdapter(Context context, List<AuditeurCaptured> auditeurs) {
        this.context = context;
        this.auditeurs = auditeurs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.auditeur_captured, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AuditeurCaptured a = auditeurs.get(position);
        holder.textViewName.setText(a.getFullName());
        holder.textViewDetails.setText("Capturé le : " + a.CatchDate);
        holder.textAtk.setText("ATK: " + a.Atk);
        holder.textDef.setText("DEF: " + a.Def);
        holder.textHp.setText("HP: " + a.Hp);
        holder.textSpd.setText("SPD: " + a.Spd);

        String imageName = a.getImageName();
        int resourceId = context.getResources().getIdentifier(
                imageName,
                "drawable",
                context.getPackageName()
        );

        if (resourceId != 0) {
            holder.imageViewAuditeur.setImageResource(resourceId);
        } else {
            holder.imageViewAuditeur.setImageResource(R.drawable.logo);
        }

        holder.btnRename.setOnClickListener(v -> {
            Context activityContext = holder.itemView.getContext();

            AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
            builder.setTitle("Donner un surnom");

            EditText input = new EditText(activityContext);
            input.setHint("Surnom (max 12)");
            input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(12) });
            input.setTextSize(14);

            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String nickname = input.getText().toString().trim();
                a.Nickname = nickname;

                updateNicknameInDB(a.Id, nickname);

                notifyItemChanged(position);
            });

            builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());

            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return auditeurs.size();
    }

    public void updateNicknameInDB(String id, String nickname) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("Nickname", nickname);

        db.collection("Listener")
                .document(id)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Base de données mise à jour !");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Erreur : " + e.getMessage());
                });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;

        TextView textViewDetails;

        ImageView imageViewAuditeur;

        TextView textAtk, textDef, textHp, textSpd;

        ImageButton btnRename;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
            imageViewAuditeur = itemView.findViewById(R.id.imageViewAuditeur);
            textAtk = itemView.findViewById(R.id.textAtk);
            textDef = itemView.findViewById(R.id.textDef);
            textHp  = itemView.findViewById(R.id.textHp);
            textSpd = itemView.findViewById(R.id.textSpd);
            btnRename = itemView.findViewById(R.id.btnRename);
        }
    }
}
