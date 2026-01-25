package com.example.cnam_go;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ShopActivity extends AppCompatActivity {
    private long money;

    private TextView tvMoney;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

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
        View encens = findViewById(R.id.encens);
        View chadJuice = findViewById(R.id.chadJuice);
        Button btnBack = findViewById(R.id.btnBack);

        setupCard(
                monstahball,
                "Monstahball",
                R.drawable.monstahball,
                v -> showBuyDialog("Monstahball", 50)
        );

        setupCard(
                redball,
                "RedBall",
                R.drawable.redball,
                v -> showBuyDialog("RedBall", 75)
        );

        setupCard(
                perrierBall,
                "PerrierBall",
                R.drawable.perrierball,
                v -> showBuyDialog("PerrierBall", 60)
        );

        setupCard(
                encens,
                "Encens",
                R.drawable.outline_android_wifi_3_bar_24,
                v -> showBuyDialog("Encens", 100)
        );

        setupCard(
                chadJuice,
                "ChadJuice",
                R.drawable.outline_air_freshener_24,
                v -> showBuyDialog("ChadJuice", 120)
        );

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupCard(View card, String title, int iconRes, View.OnClickListener listener) {
        TextView titleView = card.findViewById(R.id.title);
        ImageView iconView = card.findViewById(R.id.icon);

        titleView.setText(title);
        iconView.setImageResource(iconRes);

        card.setOnClickListener(listener);
    }
    private void updateMoney() {
        tvMoney.setText(money + " €");
    }

    private void showBuyDialog(String itemName, int price) {
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
                        updateMoney();

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


}
