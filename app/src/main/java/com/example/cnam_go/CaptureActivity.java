package com.example.cnam_go;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
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

public class CaptureActivity extends AppCompatActivity {

    private AuditeurEntity auditeur = null;

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private PreviewView cameraPreview;
    private boolean cameraEnabled = false;

    private ImageView creatureImageView;
    private ImageView ballImageView;
    private boolean isAnimating = false;
    private boolean captured = false;

    // Zone autorisée : 40% du bas
    private static final float ALLOWED_TOUCH_ZONE_FROM_BOTTOM = 0.4f;
    private float minTouchY = 0f;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        creatureImageView = findViewById(R.id.creatureImageView);
        ballImageView = findViewById(R.id.ballImageView);

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
        }

        ConstraintLayout root = findViewById(R.id.captureRoot);
        root.post(() -> {
            minTouchY = root.getHeight() * (1 - ALLOWED_TOUCH_ZONE_FROM_BOTTOM);
        });

        startCreatureAnimation();

        root.setOnTouchListener((v, event) -> {
            if (isAnimating) return true;

            float rawY = event.getRawY();
            if (rawY < minTouchY) {
                return false; // trop haut
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ballImageView.setVisibility(View.VISIBLE);
                    updateBallPosition(event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateBallPosition(event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                    launchBallWithGesture(event);
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
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

    private void updateBallPosition(float rawX, float rawY) {
        ballImageView.setX(rawX - ballImageView.getWidth() / 2f);
        ballImageView.setY(rawY - ballImageView.getHeight() / 2f);
    }

    private void launchBallWithGesture(MotionEvent event) {
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
            if (captured) return; // déjà capturé

            float progress = (float) animation.getAnimatedValue();
            float currentX = startX + (targetX - startX) * progress;
            float currentY = startY + (targetY - startY) * progress;

            ballImageView.setX(currentX);
            ballImageView.setY(currentY);

            // ✅ Vérifier la collision à chaque frame
            if (checkCollisionAt(currentX, currentY)) {
                captured = true;
                playCaptureAnimation(); // <-- Animation des étoiles
                Toast.makeText(CaptureActivity.this, "Capturé ! 🎉", Toast.LENGTH_SHORT).show();

                // Retarder la fin pour voir l’animation
                ballImageView.postDelayed(() -> finish(), 1000);
                animator.cancel();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetBall();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (!captured) resetBall();
            }
        });

        animator.start();
    }

    private void resetBall() {
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

    private boolean checkCollisionAt(float ballX, float ballY) {
        float ballCenterX = ballX + ballImageView.getWidth() / 2f;
        float ballCenterY = ballY + ballImageView.getHeight() / 2f;

        float creatureCenterX = creatureImageView.getX() + creatureImageView.getWidth() / 2f;
        float creatureCenterY = creatureImageView.getY() + creatureImageView.getHeight() / 2f;

        double dx = ballCenterX - creatureCenterX;
        double dy = ballCenterY - creatureCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        float captureRadius = creatureImageView.getWidth() * 1.2f;
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

        for (int i = 0; i < 10; i++) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star);
            star.setLayoutParams(new ConstraintLayout.LayoutParams(starSize, starSize));

            // Position au centre de la créature
            float centerX = creatureImageView.getX() + creatureImageView.getWidth() / 2f - starSize/2f;
            float centerY = creatureImageView.getY() + creatureImageView.getHeight() / 2f - starSize/2f;
            star.setX(centerX);
            star.setY(centerY);

            ConstraintLayout root = findViewById(R.id.captureRoot);
            root.addView(star);

            // Animation translation + alpha + scale
            star.animate()
                    .translationXBy((float)(Math.random()*300-150)) // dispersion horizontal plus grande
                    .translationYBy((float)(Math.random()*-300))      // monte plus haut
                    .scaleX(2.5f)  // augmente la taille pendant l'animation
                    .scaleY(2.5f)
                    .alpha(0f)
                    .setDuration(1200)
                    .withEndAction(() -> root.removeView(star))
                    .start();
        }
    }
}