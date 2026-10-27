package kr.ac.mjc.capstar;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CheckActivity extends AppCompatActivity {

    //선언
    private EditText editTextCode;
    private EditText editTextPassword;
    private Button buttonCheck;
    private ImageView videoView;
    private ImageView logoIv;
    private ImageView darkIv;
    private ProgressBar progress;
    private ImageView qr;
    String editTextCodeST;
    String editTextPasswordSt;

    private long backKeyPressedTime = 0;
    private Toast toast;

    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);

        doFullScreen();

        //초기화

        videoView = findViewById(R.id.videoView);
        Glide.with(CheckActivity.this).load(R.raw.ba)//.transform(new BlurTransformation(CheckActivity.this))
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(videoView);
        progress = findViewById(R.id.progress);
        logoIv = findViewById(R.id.logo);
        darkIv = findViewById(R.id.darkIv);
        editTextCode = (EditText) findViewById(R.id.et_code);
        editTextPassword = (EditText) findViewById(R.id.et_password);
        buttonCheck = (Button) findViewById(R.id.btn_check);
        qr = findViewById(R.id.qr_Iv);

        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        database = firebaseDatabase.getReference();

        AlphaAnimation alphaAnimation = new AlphaAnimation(0,1);
        alphaAnimation.setStartOffset(1000);
        alphaAnimation.setDuration(3500);
        alphaAnimation.setFillAfter(true);
        AlphaAnimation alphaAnimation2 = new AlphaAnimation(0,1);
        alphaAnimation2.setStartOffset(1000);
        alphaAnimation2.setDuration(3500);
        alphaAnimation2.setFillAfter(true);

        logoIv.startAnimation(alphaAnimation);
        darkIv.startAnimation(alphaAnimation2);

        //자동로그인
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                FirebaseUser currentUser = auth.getCurrentUser();

                if(currentUser != null){
                    String userEmail = currentUser.getEmail().split("@")[0];
                    if(userEmail.equals("anonymous")){
                        auth.getInstance().signOut();

                        logoIv.animate().translationY(-650).setDuration(650).withLayer();
                        Handler handler2 = new Handler();
                        handler2.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                editTextCode.setVisibility(View.VISIBLE);
                                editTextPassword.setVisibility(View.VISIBLE);
                                buttonCheck.setVisibility(View.VISIBLE);
                                qr.setVisibility(View.VISIBLE);
                                doNotFullScreen();
                            }
                        }, 700);

                    }else {
                        changeActivity(0, userEmail);
                    }
                }else {
                    int[] logoLocation = new int[2];
                    int[] tologoLocation = new int[2];

                    logoIv.animate().translationY(-650).setDuration(650).withLayer();
                    Handler handler2 = new Handler();
                    handler2.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            editTextCode.setVisibility(View.VISIBLE);
                            editTextPassword.setVisibility(View.VISIBLE);
                            buttonCheck.setVisibility(View.VISIBLE);
                            qr.setVisibility(View.VISIBLE);
                            doNotFullScreen();
                        }
                    }, 700);
                }
            }
        }, 3000);



        //확인 버튼을 눌렀을 때
        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonCheck.setEnabled(false);
                progress.setVisibility(View.VISIBLE);

                //코드와 비밀번호 문자열 변수 (간단하게)
                editTextCodeST = editTextCode.getText().toString();
                editTextPasswordSt = editTextPassword.getText().toString();

                //코드와 비밀번호 둘 다 입력했을 때 (관리자)
                if (!(editTextCodeST.getBytes().length <= 0) && !(editTextPasswordSt.getBytes().length <= 0)){
                    checkManager(editTextCodeST + "@admin.com", editTextPasswordSt);
                }
                //코드만 입력했을 때 (사용자)
                else if (!(editTextCodeST.getBytes().length <= 0)) {
                    checkUser(editTextCodeST);
                }
                //코드와 비밀번호 둘 다 입력하지 않았을 때 (경고창)
                else
                 {
                    Toast.makeText(CheckActivity.this, "장소코드는 필수 입력사항입니다.", Toast.LENGTH_LONG).show();
                    progress.setVisibility(View.INVISIBLE);
                    buttonCheck.setEnabled(true);
                }
            }
        });

        qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckActivity.this, qrActivity.class);
                startActivity(intent);
            }
        });
    }

    //뒤로가기 눌렀을 때 확인창, 뒤로가기 두번 누르면 종료
    @Override
    public void onBackPressed() {
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교. 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지났으면 Toast 출력
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(this, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finishAffinity();
            System.runFinalization();
            System.exit(0);
            toast.cancel();
        }
    }

    //관리자로 체크인
    private void checkManager(final String code, String password) {

        auth.signInWithEmailAndPassword(code, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            changeActivity(0, code);
                        } else {
                            Toast.makeText(CheckActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                            progress.setVisibility(View.INVISIBLE);
                            buttonCheck.setEnabled(true);
                        }

                    }
                });
    }

    //사용자로 체크인
    private void checkUser (final String code) {
        database.child(code).child("SpaceInfo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SpaceInfo spaceInfo = snapshot.getValue(SpaceInfo.class);
                if(spaceInfo != null){

                    new AlertDialog.Builder(CheckActivity.this)
                            .setMessage(spaceInfo.name + " " + spaceInfo.location)
                            .setPositiveButton("취소", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which){
                                    progress.setVisibility(View.INVISIBLE);
                                    buttonCheck.setEnabled(true);
                                }
                            })
                            .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which){
                                    checkUserOk(code);
                                }
                            })
                            .show();
                }else{
                    progress.setVisibility(View.INVISIBLE);
                    buttonCheck.setEnabled(true);
                    Toast.makeText(CheckActivity.this, "장소를 찾을 수 없습니다. 코드를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progress.setVisibility(View.INVISIBLE);
                buttonCheck.setEnabled(true);
                Toast.makeText(CheckActivity.this, "연결 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void checkUserOk(final String code){
        auth.signInWithEmailAndPassword("anonymous@anonymous.com", "qwer1234")
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            changeActivity(1, code);
                        } else {
                            Toast.makeText(CheckActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //화면 전환 (체크인)
    private void changeActivity (int ac, String code) {
        Intent intent;
        switch (ac){
            case 0:
                if (auth.getCurrentUser() != null) {
                    intent = new Intent(CheckActivity.this, ManagerMainActivity.class);
                    startActivity(intent);
                    //화면전환 애니메이션 제거
                    overridePendingTransition(0, 0);
                    finish();
                    //Toast.makeText(CheckActivity.this, "로그인되었습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1:
                if(auth.getCurrentUser() != null) {
                    intent = new Intent(CheckActivity.this, UserMainActivity.class);
                    intent.putExtra("code", code);
                    startActivity(intent);
                    //화면전환 애니메이션 제거
                    overridePendingTransition(0, 0);
                    progress.setVisibility(View.INVISIBLE);
                    buttonCheck.setEnabled(true);
                    //Toast.makeText(CheckActivity.this, "로그인되었습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private float getVideoProportion(){
        return 1.77777f;
    }

    private void doFullScreen() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void doNotFullScreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
