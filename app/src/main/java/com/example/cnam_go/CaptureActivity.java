package com.example.cnam_go;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cnam_go.models.AuditeurEntity;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CaptureActivity extends AppCompatActivity {
    private AuditeurEntity auditeur = null;
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private PreviewView cameraPreview;
    private boolean cameraEnabled = false;
    private TextView creatureNameView;
    private ImageView creatureImageView;
    private ImageView ballImageView;
    private boolean isAnimating = false;
    private boolean isCaptureSequenceRunning = false;
    private boolean captured = false;
    private static final float ALLOWED_TOUCH_ZONE_FROM_BOTTOM = 0.25f;
    private float minTouchY = 0f;
    private int captureAttempts = 0;
    private boolean auditeurFled = false;
    private String selectedItemCode = "SIT-01";
    private RelativeLayout btnCanetteMonstah, btnCanetteReball, btnCanPerrier;
    private ImageButton btnFlee;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        creatureNameView = findViewById(R.id.creatureNameText);
        creatureImageView = findViewById(R.id.creatureImageView);
        ballImageView = findViewById(R.id.ballImageView);
        btnFlee = findViewById(R.id.btnFlee);
        btnCanetteMonstah = findViewById(R.id.btnCanetteMonstah);
        btnCanetteReball = findViewById(R.id.btnCanetteReball);
        btnCanPerrier = findViewById(R.id.btnCanPerrier);

        auditeur = (AuditeurEntity) getIntent().getSerializableExtra("auditeur_entity");
        if (auditeur != null) {
            Log.d("CAPTURE", "Auditeur reçu : " + auditeur.Name);

            String imgName = auditeur.Name.toLowerCase();

            int resId = getResources().getIdentifier(imgName, "drawable", getPackageName());
            if (resId != 0) {
                Drawable drawable = getResources().getDrawable(resId, null);
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 160, 160, true);
                Drawable auditeurIcon = new BitmapDrawable(getResources(), scaled);
                creatureImageView.setImageDrawable(auditeurIcon);
            } else {
                Log.w("CAPTURE", "Drawable non trouvé pour : " + imgName);
            }

            creatureNameView.setText(auditeur.Name);
        }

        ConstraintLayout root = findViewById(R.id.captureRoot);
        root.post(() -> {
            minTouchY = root.getHeight() * (1 - ALLOWED_TOUCH_ZONE_FROM_BOTTOM);
        });

        startCreatureAnimation();

        root.setOnTouchListener((v, event) -> {
            if (isAnimating || isCaptureSequenceRunning) return true;

            float rawY = event.getRawY();
            if (rawY < minTouchY) {
                return false; // trop haut
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ballImageView.setVisibility(View.VISIBLE);
                    updateCanettePosition(event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateCanettePosition(event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                    launchCanette(event);
                    break;
            }
            return true;
        });

        cameraPreview = findViewById(R.id.cameraPreview);
        Button cameraButton = findViewById(R.id.cameraButton);

        cameraButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST
                );
            } else {
                toggleCamera();
            }
        });

        btnCanetteMonstah.setOnClickListener(v -> selectCanette("SIT-01", R.drawable.monstahball));
        btnCanetteReball.setOnClickListener(v -> selectCanette("SIT-02", R.drawable.redball));
        btnCanPerrier.setOnClickListener(v -> selectCanette("SIT-03", R.drawable.perrierball));
        btnFlee.setOnClickListener(v -> {
            Toast.makeText(this, "Tu as pris la fuite !", Toast.LENGTH_SHORT).show();

            setResult(RESULT_CANCELED);
            finish();
        });

        loadUserCanette();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleCamera();
            } else {
                Toast.makeText(this,
                        "Permission caméra refusée",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectCanette(String code, int imageResId) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        long stock = prefs.getLong(code, 0);

        if (stock > 0) {
            selectedItemCode = code;
            ballImageView.setImageResource(imageResId); // Change la balle au centre de l'écran

            Toast.makeText(this, "Sélectionné : " + code, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Vous n'avez plus de cannettes de ce type !", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadUserCanette() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = prefs.getString("userId", "");

        if (!userId.isEmpty()) {
            db.collection("Inventory")
                    .whereEqualTo("UserId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                                String itemCode = doc.getString("Code");
                                Long quantity = doc.getLong("Quantity");

                                if (quantity == null) quantity = 0L;

                                updateCanetteUI(itemCode, quantity);

                                prefs.edit().putLong(itemCode, quantity).apply();

                                Log.d("Firebase", "Objet "+ itemCode + " en " + quantity + "x.");
                            }

                            Log.d("Firebase", "Inventaire complet chargé");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firebase", "Erreur", e));
        }
    }

    private void updateCanetteUI(String code, long qty) {
        if (code == null) return;

        // Si on a aucune canette de ce type, on l'a grise.
        float alphaValue = (qty > 0) ? 1.0f : 0.4f;

        switch (code) {
            case "SIT-01":
                TextView txtClassic = findViewById(R.id.txtCountClassic);
                ImageView imgClassic = findViewById(R.id.imgCanClassic); // ID de l'ImageView dans le XML
                txtClassic.setText("x" + qty);
                imgClassic.setAlpha(alphaValue);
                break;

            case "SIT-02":
                TextView txtSuper = findViewById(R.id.txtCountSuper);
                ImageView imgSuper = findViewById(R.id.imgCanSuper);
                txtSuper.setText("x" + qty);
                imgSuper.setAlpha(alphaValue);
                break;

            case "SIT-03":
                TextView txtUltra = findViewById(R.id.txtCountUltra);
                ImageView imgUltra = findViewById(R.id.imgCanUltra);
                txtUltra.setText("x" + qty);
                imgUltra.setAlpha(alphaValue);
                break;
        }
    }

    private void updateCanettePosition(float rawX, float rawY) {
        ballImageView.setX(rawX - ballImageView.getWidth() / 2f);
        ballImageView.setY(rawY - ballImageView.getHeight() / 2f);
    }

    private void resetCanette() {
        isAnimating = false;
        ballImageView.setVisibility(View.INVISIBLE);

        ConstraintLayout root = findViewById(R.id.captureRoot);

        ballImageView.setX(
                root.getWidth()/2f - ballImageView.getWidth()/2f
        );

        ballImageView.setY(
                root.getHeight() - ballImageView.getHeight() - 80f
        );
    }

    private void launchCanette(MotionEvent event) {
        if (selectedItemCode == null || selectedItemCode.isEmpty()) {
            Toast.makeText(this, "Sélectionnez une cannette !", Toast.LENGTH_SHORT).show();
            return;
        }

        useCanette(selectedItemCode);

        isAnimating = true;
        captured = false;

        float startX = ballImageView.getX();
        float startY = ballImageView.getY();

        float endX = event.getRawX();
        float endY = event.getRawY();

        float deltaX = endX - startX - ballImageView.getWidth() / 2f;
        float deltaY = endY - startY - ballImageView.getHeight() / 2f;

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance < 10) {
            // Lancer vers le haut si pas de mouvement
            deltaX = 0;
            deltaY = -300;
        } else {
            // Amplifier la direction
            float force = 5.0f;
            deltaX *= force;
            deltaY *= force;
        }

        // Cible éloignée dans la direction du geste
        ConstraintLayout root = findViewById(R.id.captureRoot);
        float screenWidth = root.getWidth();
        float screenHeight = root.getHeight();

        // Normaliser le vecteur du geste
        float length = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        deltaX /= length;
        deltaY /= length;

        // Multiplier par une grande distance (2x hauteur écran)
        float distance2 = screenHeight * 2f;

        float targetX = startX + deltaX * distance2;
        float targetY = startY + deltaY * distance2;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);

        animator.addUpdateListener(animation -> {
            if (captured) return;

            float progress = (float) animation.getAnimatedValue();
            float currentX = startX + (targetX - startX) * progress;
            float currentY = startY + (targetY - startY) * progress;

            ballImageView.setX(currentX);
            ballImageView.setY(currentY);

            if (checkCollisionAt(currentX, currentY)) {
                captured = true;
                animator.cancel();

                // Bloque tout input
                isAnimating = false;
                isCaptureSequenceRunning = true;

                // Fige la balle exactement sur place
                ballImageView.setX(currentX);
                ballImageView.setY(currentY);
                ballImageView.setVisibility(View.VISIBLE);

                startCanetteCaptureSequence();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetCanette();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (!captured) resetCanette();
            }
        });

        animator.start();
    }

    private void startCanetteCaptureSequence() {
        creatureImageView.setVisibility(View.INVISIBLE);

        ballImageView.animate()
                .y(ballImageView.getY() + 180f)
                .setDuration(250)
                .withEndAction(() -> playCanetteShake(0))
                .start();
    }

    private void playCanetteShake(int shakeCount) {
        if (shakeCount >= 3) {
            boolean success = computeCaptureSuccess();

            if (success) {
                Toast.makeText(this, "🎉 Capturé !", Toast.LENGTH_SHORT).show();

                playCaptureAnimation();
                addAuditeurInDatabase();
                showCaptureSummary();

                ballImageView.postDelayed(this::finish, 2000);
            } else {
                captureAttempts++;

                if (computeFlee()) {
                    auditeurFled = true;

                    ballImageView.setVisibility(View.VISIBLE);
                    creatureImageView.setVisibility(View.VISIBLE);

                    Toast.makeText(this, "💨 L'auditeur s'est enfui !", Toast.LENGTH_LONG).show();

                    ballImageView.postDelayed(() -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    }, 1200);

                    return;
                }

                creatureImageView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "💥 Échappé !", Toast.LENGTH_SHORT).show();
                isCaptureSequenceRunning = false;
                resetCanette();
            }
            return;
        }

        float originalX = ballImageView.getX();

        ballImageView.animate()
                .x(originalX - 35f)
                .setDuration(140)
                .withEndAction(() ->
                        ballImageView.animate()
                                .x(originalX + 35f)
                                .setDuration(140)
                                .withEndAction(() ->
                                        ballImageView.animate()
                                                .x(originalX)
                                                .setDuration(140)
                                                .withEndAction(() -> playCanetteShake(shakeCount + 1))
                                                .start()
                                ).start()
                ).start();
    }

    private void useCanette(String itemCode) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        long currentStock = prefs.getLong(itemCode, 0);
        if (currentStock > 0) {
            long newStock = currentStock - 1;
            prefs.edit().putLong(itemCode, newStock).apply();

            updateCanetteUI(itemCode, newStock);

            if (newStock == 0) {
                autoSwitchCanette(prefs);
            }

            if (!userId.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("Inventory")
                        .whereEqualTo("UserId", userId)
                        .whereEqualTo("Code", itemCode)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                String docId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                // Utilisation de FieldValue.increment(-1) pour la soustraction atomique
                                db.collection("Inventory").document(docId)
                                        .update("Quantity", FieldValue.increment(-1))
                                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "Stock réduit sur le serveur"))
                                        .addOnFailureListener(e -> Log.e("Firebase", "Erreur serveur", e));
                            }
                        });
            }
        }
    }

    private void autoSwitchCanette(SharedPreferences prefs) {
        String[] codes = {"SIT-01", "SIT-02", "SIT-03"};

        boolean foundAlternative = false;

        for (String code : codes) {
            if (prefs.getLong(code, 0) > 0) {
                int resId = R.drawable.monstahball;
                if (code.equals("SIT-02")) resId = R.drawable.redball;
                if (code.equals("SIT-03")) resId = R.drawable.perrierball;

                selectCanette(code, resId);
                foundAlternative = true;
                break;
            }
        }

        if (!foundAlternative) {
            selectedItemCode = "";
            ballImageView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Plus de munitions ! Il faut en acheter au magasin.", Toast.LENGTH_LONG).show();
        }
    }


    private void toggleCamera() {
        cameraEnabled = !cameraEnabled;

        if (cameraEnabled) {
            cameraPreview.setVisibility(View.VISIBLE);
            startCamera();
        } else {
            cameraPreview.setVisibility(View.GONE);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }








    private boolean checkCollisionAt(float ballX, float ballY) {
        float ballCenterX = ballX + ballImageView.getWidth() / 2f;
        float ballCenterY = ballY + ballImageView.getHeight() / 2f;

        float creatureCenterX = creatureImageView.getX() + creatureImageView.getWidth() / 2f;
        float creatureCenterY = creatureImageView.getY() + creatureImageView.getHeight() / 2f;

        double dx = ballCenterX - creatureCenterX;
        double dy = ballCenterY - creatureCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        float captureRadius = creatureImageView.getWidth() * 0.4f;
        return distance < captureRadius;
    }

    private void startCreatureAnimation() {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                creatureImageView, "translationX", -150f, 150f);
        animator.setDuration(5000);
        animator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        animator.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void playCaptureAnimation() {
        int starSize = 80;
        ConstraintLayout root = findViewById(R.id.captureRoot);

        float centerX = ballImageView.getX() + ballImageView.getWidth() / 2f - starSize/2f;
        float centerY = ballImageView.getY() + ballImageView.getHeight() / 2f - starSize/2f;

        for (int i = 0; i < 10; i++) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star);
            star.setLayoutParams(new ConstraintLayout.LayoutParams(starSize, starSize));

            star.setX(centerX);
            star.setY(centerY);
            root.addView(star);

            star.animate()
                    .translationXBy((float)(Math.random()*300 - 150))
                    .translationYBy((float)(Math.random()*-300))
                    .scaleX(2.5f)
                    .scaleY(2.5f)
                    .alpha(0f)
                    .setDuration(1400) // un peu plus lent ✨
                    .withEndAction(() -> root.removeView(star))
                    .start();
        }
    }

    private boolean computeCaptureSuccess() {
        int baseChance = 20;

        int bonus = 50;
        if (auditeur.CodeBall.equalsIgnoreCase(selectedItemCode)) {
            baseChance += bonus;
        }

        int finalChance = Math.min(90, Math.max(10, baseChance));
        int roll = new java.util.Random().nextInt(100);

        Log.d("CAPTURE", "Roll=" + roll + " / Chance=" + finalChance);

        return roll < finalChance;
    }

    private boolean computeFlee() {
        if (captureAttempts < 3) return false;

        int fleeChance;
        if (captureAttempts == 3) fleeChance = 20;
        else if (captureAttempts == 4) fleeChance = 35;
        else if (captureAttempts == 5) fleeChance = 50;
        else fleeChance = 70;

        int roll = new java.util.Random().nextInt(100);

        Log.d("CAPTURE", "Fuite roll=" + roll + " chance=" + fleeChance);

        return roll < fleeChance;
    }

    private void showCaptureSummary() {
        creatureNameView.setVisibility(View.INVISIBLE);

        LinearLayout summaryLayout = findViewById(R.id.captureSummaryLayout);
        ImageView summaryImage = findViewById(R.id.summaryCreatureImage);
        TextView summaryName = findViewById(R.id.summaryCreatureName);
        TextView summaryHp = findViewById(R.id.summaryHp);
        TextView summaryAtk = findViewById(R.id.summaryAtk);
        TextView summaryDef = findViewById(R.id.summaryDef);
        TextView summarySpd = findViewById(R.id.summarySpd);

        summaryName.setText(auditeur.Name);
        summaryHp.setText("HP: " + auditeur.BaseHp);
        summaryAtk.setText("ATK: " + auditeur.BaseAtk);
        summaryDef.setText("DEF: " + auditeur.BaseDef);
        summarySpd.setText("SPD: " + auditeur.BaseSpd);

        String imgName = auditeur.Name.toLowerCase();
        int resId = getResources().getIdentifier(imgName, "drawable", getPackageName());
        if (resId != 0) {
            Drawable drawable = getResources().getDrawable(resId, null);
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
            summaryImage.setImageBitmap(scaled);
        }

        summaryLayout.setVisibility(View.VISIBLE);

        summaryLayout.postDelayed(() -> finish(), 5000);
    }

    private void addAuditeurInDatabase() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        boolean isLogged = prefs.getBoolean("isLogged", false);

        if (isLogged) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new java.util.Date());

            String userId = prefs.getString("userId", "");

            Map<String, Object> auditeurData = getAuditeurData(userId, currentDate);

            db.collection("Listener")
                    .add(auditeurData)
                    .addOnSuccessListener(documentReference -> {
                        String newId = documentReference.getId();
                        Log.d("Firebase", "Auditeur ajouté avec l'ID : " + newId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Erreur lors de l'ajout", e);
                    });
        }
    }

    private Map<String, Object> getAuditeurData(String userId, String currentDate) {
        Map<String, Object> auditeurData = new HashMap<>();
        auditeurData.put("UserId", userId);
        auditeurData.put("Code", auditeur.Code);
        auditeurData.put("Name", auditeur.Name);
        auditeurData.put("Nickname", "");
        auditeurData.put("IsChad", auditeur.IsChad);
        auditeurData.put("CatchDate", currentDate);
        auditeurData.put("Atk", auditeur.BaseAtk);
        auditeurData.put("Def", auditeur.BaseDef);
        auditeurData.put("Spd", auditeur.BaseSpd);
        auditeurData.put("Hp", auditeur.BaseHp);
        return auditeurData;
    }
}