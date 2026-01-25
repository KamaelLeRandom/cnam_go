package com.example.cnam_go;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ShopActivity extends AppCompatActivity {
    private SoundPool soundPool;

    private int soundClick, soundSuccess;

    private long money;

    private TextView tvMoney;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        soundClick = soundPool.load(this, R.raw.shop_click, 1);
        soundSuccess = soundPool.load(this, R.raw.shop_sucess, 1);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        boolean isLogged = prefs.getBoolean("isLogged", false);

        if (isLogged) {
            String login = prefs.getString("login", "");
            money = prefs.getLong("money", 0);

            System.out.println("Utilisateur connecté : " + login);
        }

        tvMoney = findViewById(R.id.tvMoney);
        updateMoney();

        View monstahball = findViewById(R.id.monstahball);
        View redball = findViewById(R.id.redball);
        View perrierBall = findViewById(R.id.perrierBall);
        // View encens = findViewById(R.id.encens);
        // View chadJuice = findViewById(R.id.chadJuice);
        Button btnBack = findViewById(R.id.btnBack);

        setupCard(
                monstahball,
                "Monstahball",
                "Une canette énergisante ultra sucrée. Rien de plus efficace qu’une dose de sucre pour convaincre un auditeur fatigué. Même puissance que les autres, mais avec un goût explosif.",
                R.drawable.monstahball,
                v -> showBuyDialog("SIT-01","Monstahball", 50)
        );

        setupCard(
                redball,
                "RedBall",
                "Une boisson rouge vif au goût fruité. Classique, fiable, toujours prête à rafraîchir un auditeur récalcitrant. Aucun bonus caché, juste du style.",
                R.drawable.redball,
                v -> showBuyDialog("SIT-02", "RedBall", 75)
        );

        setupCard(
                perrierBall,
                "PerrierBall",
                "Une eau pétillante élégante. Les bulles chatouillent l’ego des auditeurs les plus distingués. Même efficacité, mais avec classe.",
                R.drawable.perrierball,
                v -> showBuyDialog("SIT-03","PerrierBall", 60)
        );

//        setupCard(
//                encens,
//                "Encens",
//                R.drawable.outline_android_wifi_3_bar_24,
//                v -> showBuyDialog("Encens", 100)
//        );
//
//        setupCard(
//                chadJuice,
//                "ChadJuice",
//                R.drawable.outline_air_freshener_24,
//                v -> showBuyDialog("ChadJuice", 120)
//        );

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupCard(View card, String title, String description, int iconRes, View.OnClickListener listener) {
        TextView titleView = card.findViewById(R.id.title);
        TextView descriptionView = card.findViewById(R.id.description);
        ImageView iconView = card.findViewById(R.id.icon);

        titleView.setText(title);
        iconView.setImageResource(iconRes);
        descriptionView.setText(description);

        card.setOnClickListener(listener);
    }

    private void updateMoney() {
        tvMoney.setText(money + " €");
    }

    private void showBuyDialog(String code, String itemName, int price) {
        soundPool.play(soundClick, 1, 1, 0, 0, 1);

        View view = getLayoutInflater().inflate(R.layout.dialog_buy_item, null);

        TextView tvItemName = view.findViewById(R.id.tvItemName);
        TextView tvPrice = view.findViewById(R.id.tvPrice);
        NumberPicker picker = view.findViewById(R.id.numberPicker);

        tvItemName.setText(itemName);
        tvPrice.setText("Prix unitaire : " + price + " ₽");

        picker.setMinValue(1);
        picker.setMaxValue(99);
        picker.setValue(1);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Acheter", (d, which) -> {
                    int quantity = picker.getValue();
                    int totalPrice = quantity * price;

                    if (money >= totalPrice) {
                        money -= totalPrice;

                        AddItem(code, quantity, money);

                        Toast.makeText(
                                this,
                                "Acheté : " + quantity + " × " + itemName,
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "Fonds insuffisants",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .create();

        dialog.show();
    }

    private void AddItem(String code, int quantity, Long money) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);

        boolean isLogged = prefs.getBoolean("isLogged", false);

        if (isLogged) {
            String userId = prefs.getString("userId", "");

            db.collection("Inventory")
                    .whereEqualTo("UserId", userId)
                    .whereEqualTo("Code", code)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                Log.d("Firebase", "A trouvé l'objet");

                                Map<String, Object> item = new HashMap<>();
                                item.put("UserId", userId);
                                item.put("Code", code);
                                item.put("Quantity", quantity);

                                db.collection("Inventory")
                                        .add(item)
                                        .addOnSuccessListener(documentReference -> {
                                            Log.d("Firebase", "Document ajouté avec ID : " + documentReference.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firebase", "Erreur lors de l'ajout", e);
                                        });
                            } else {
                                String docId = task.getResult().getDocuments().get(0).getId();

                                db.collection("Inventory").document(docId)
                                        .update("Quantity", FieldValue.increment(quantity))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firebase", "Quantité mise à jour");
                                        });
                            }

                            RemoveMoney(money);
                        } else {
                            Log.e("Firebase", "Erreur de vérification : ", task.getException());
                        }
                    });
        }
    }

    private void RemoveMoney(Long money) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        boolean isLogged = prefs.getBoolean("isLogged", false);

        if (isLogged) {
            String userId = prefs.getString("userId", "");

            Map<String, Object> updates = new HashMap<>();
            updates.put("Money", money);

            db.collection("User")
                    .document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        tvMoney.setText(money + " €");
                        editor.putLong("money", money != null ? money : 0);

                        soundPool.play(soundSuccess, 1, 1, 0, 0, 1);

                        Log.d("Firebase", "Base de données mise à jour !");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Erreur : " + e.getMessage());
                    });
        }
    }
}
