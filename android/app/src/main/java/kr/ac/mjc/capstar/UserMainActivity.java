package kr.ac.mjc.capstar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class UserMainActivity extends AppCompatActivity {

    //선언
    String code;

    private long backKeyPressedTime = 0;
    private Toast toast;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference database;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseFirestore firestore;
    FirebaseAuth auth;

    Intent intent;

    ImageView faceIv;
    EditText nameEt;
    EditText phoneNoEt;
    EditText locationEt;
    Button submitBtn;
    ImageView progress;

    Uri selectImage = null;

    final int IMAGE_PICK = 1234;

    String name ;
    String phoneNo;
    String location;

    @Override
    protected void onStart() {
        super.onStart();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usermain);

        firebaseDatabase = FirebaseDatabase.getInstance();
        database = firebaseDatabase.getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        firestore = FirebaseFirestore.getInstance();

        intent = getIntent();
        code = intent.getExtras().getString("code");

        faceIv = findViewById(R.id.iv_photo);
        nameEt = findViewById(R.id.et_name);
        phoneNoEt = findViewById(R.id.et_pnumber);
        locationEt = findViewById(R.id.et_location);
        submitBtn = findViewById(R.id.submit_btn);
        progress = findViewById(R.id.progress);

        faceIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(Intent.ACTION_PICK);
                intent1.setType("image/*");
                startActivityForResult(intent1, IMAGE_PICK);
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitBtn.setEnabled(false);
                Glide.with(UserMainActivity.this).load(R.raw.loadinggg).into(progress);
                progress.setVisibility(View.VISIBLE);
                name = nameEt.getText().toString();
                phoneNo = phoneNoEt.getText().toString();
                location = locationEt.getText().toString();

                if (name == null || phoneNo == null || location == null || selectImage == null) {
                    submitBtn.setEnabled(true);
                    progress.setVisibility(View.INVISIBLE);
                    Toast.makeText(UserMainActivity.this, "입력하지않은 값이 있습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    database.child(code).child("User").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d("jwj", snapshot.toString());
                            if (snapshot.getValue() != null) {

                                String ss = snapshot.toString();
                                if (ss.contains("name_phoneNo=" + name + "_" + phoneNo)) {
                                    Toast.makeText(UserMainActivity.this, "이미 등록된 사용자 입니다.", Toast.LENGTH_SHORT).show();
                                    submitBtn.setEnabled(true);
                                    progress.setVisibility(View.INVISIBLE);
                                } else {
                                    registerUser();
                                }
                            } else {
                                registerUser();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK){
            selectImage = data.getData();
            faceIv.setImageURI(selectImage);
        }
    }

    void registerUser(){
        final String key = database.child(code).child("User").push().getKey();
        storageReference.child(code).child("UserFace").child(key).putFile(selectImage)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        storageReference.child(code + "/UserFace/" + key).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                SpaceUser spUser = new SpaceUser(code, name, phoneNo, location, uri.toString(), name + "_" + phoneNo);
                                database.child(code).child("User").child(key).setValue(spUser);
                                Toast.makeText(UserMainActivity.this, "사용자 등록 완료", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                            }
                        });

                    }
                });
    }

    //액티비티 종료 시 익명사용자 로그아웃
    @Override
    protected void onDestroy() {
        super.onDestroy();
        auth.getInstance().signOut();
    }
}
