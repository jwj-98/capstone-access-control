package kr.ac.mjc.capstar;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ManagerMainActivity extends AppCompatActivity {

    //선언
    private RecyclerView list_rv;
    private Button buttonLogout;
    private Button btn;
    private long backKeyPressedTime = 0;
    private Toast toast;
    private TextView spaceNameTv;
    private TextView spaceLocationTv;
    private TextView spaceCodeTv;
    private ImageView progressBar;
    private ImageView deleteBtn;
    SpaceInfo spaceInfo;

    FirebaseAuth auth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference database;
    FirebaseStorage firebaseStorage;
    StorageReference storage;

    String placeCode;

    ArrayList<SpaceLog> logList;
    LogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managermain);

        auth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        database = firebaseDatabase.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        storage = firebaseStorage.getReference();

        placeCode = auth.getCurrentUser().getEmail().split("@")[0];

        logList = new ArrayList<SpaceLog>();
        logAdapter = new LogAdapter(this, logList);

        //초기화
        buttonLogout = (Button) findViewById(R.id.btn_logout);
        btn = (Button) findViewById(R.id.button);
        spaceCodeTv = findViewById(R.id.spaceCodeTv);
        spaceLocationTv = findViewById(R.id.spaceLocationTv);
        spaceNameTv = findViewById(R.id.spaceNaemTv);
        list_rv = findViewById(R.id.list_rv);
        list_rv.setAdapter(logAdapter);
        list_rv.setLayoutManager(new LinearLayoutManager(this));
        deleteBtn = findViewById(R.id.deleteBtn);
        progressBar = findViewById(R.id.progressBar);
        Glide.with(this).load(R.raw.loadinggg).into(progressBar);

        database.child(placeCode).child("SpaceInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.INVISIBLE);
                spaceInfo = snapshot.getValue(SpaceInfo.class);
                spaceCodeTv.setText(spaceInfo.code);
                spaceNameTv.setText(spaceInfo.name);
                spaceLocationTv.setText(spaceInfo.location);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        database.child(placeCode).child("Log").orderByKey().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.getValue().equals("")) {
                    SpaceLog newLog = new SpaceLog();
                    newLog.key = snapshot.getKey();
                    logList.add(newLog);
                }else {
                    SpaceLog newLog = snapshot.getValue(SpaceLog.class);
                    newLog.key = snapshot.getKey();
                    logList.add(newLog);
                    logAdapter.notifyDataSetChanged();
                    list_rv.scrollToPosition(logList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                final String logKey = snapshot.getKey();
                int index = 0;
                for(SpaceLog log: logList){
                    if(logKey.equals(log.key)){
                        SpaceLog newLog = snapshot.getValue(SpaceLog.class);
                        newLog.key = log.key;
                        index = logList.indexOf(log);
                        logList.set(index, newLog);
                        break;
                    }
                }
                logAdapter.notifyDataSetChanged();
                list_rv.scrollToPosition(index);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                final String logKey = snapshot.getKey();
                SpaceLog targetLog;
                for(SpaceLog log: logList){
                    if(logKey.equals(log.key)){
                        if(log.authMethod.equals("QR코드")) {
                            storage.child(placeCode + "/Log/" + log.key + "/QRAuth").delete();
                        }
                        logList.remove(logList.indexOf(log));

                        break;
                    }
                }
                logAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("databaseErr", error.toString());
            }
        });

        //로그아웃 버튼을 눌렀을 때
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(ManagerMainActivity.this)
                        .setMessage(spaceInfo.name + " " + spaceInfo.location + "에서 로그아웃합니다.")
                        .setPositiveButton("취소", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){

                            }
                        })
                        .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){
                                auth.getInstance().signOut();
                                Intent intent = new Intent(ManagerMainActivity.this, CheckActivity.class);
                                startActivity(intent);
                                //화면전환 애니메이션 제거
                                overridePendingTransition(0, 0);
                                finish();
                            }
                        })
                        .show();



            }
        });

        //로그 생성
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeLog(placeCode);
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(ManagerMainActivity.this)
                        .setMessage("모든 출입기록을 초기화합니다.")
                        .setPositiveButton("취소", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){

                            }
                        })
                        .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which){
                                database.child(placeCode).child("Log").removeValue();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지났으면 Toast 출력
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(this, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finishAffinity();
            System.runFinalization();
            System.exit(0);
            toast.cancel();
        }
    }

    void makeLog(String placeCode){

        final String code = placeCode;

        storage.
                child(placeCode).
                child("Log/testAuthImage/jia.jpg").
                getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SpaceLog makeSpaceLog = new SpaceLog(code, date, true, "테스트", uri.toString(), "dev_");
                String key = database.child(code).child("Log").push().getKey();
                database.child(code).child("Log").child(key).setValue(makeSpaceLog);
            }
        });

    }
}


