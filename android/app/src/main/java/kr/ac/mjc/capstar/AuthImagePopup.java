package kr.ac.mjc.capstar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

public class AuthImagePopup extends Activity {

    ImageView img;

    static final String INTENT_URL="authUrl";

    LinearLayout userInfo;
    LinearLayout userLayout;
    ImageView userImage;
    TextView userName;
    TextView userPhoneNo;
    TextView userAdd;

    SpaceLog spaceLog;
    SpaceUser spaceuser;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.auth_image);


        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();


        Intent intent = getIntent();
        spaceLog = intent.getParcelableExtra(INTENT_URL);

        userInfo = findViewById(R.id.userInfo);
        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.name_tv);
        userPhoneNo = findViewById(R.id.phoneNo_tv);
        userAdd = findViewById(R.id.add_tv);
        userLayout = findViewById(R.id.userLayout);

        img = findViewById(R.id.imageView);
        Glide.with(img).load(spaceLog.authUrl).into(img);

        if(spaceLog.authCheck){
            if(spaceLog.authMethod.equals("QR코드")){

                databaseReference.child(spaceLog.code).child("User").child("/")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                String snapshotData = snapshot.getValue().toString();
                                snapshotData = snapshotData.substring(1, snapshotData.length() - 1);
                                String[] userList = snapshotData.split("\\},");
                                String key = "/";
                                for (String user: userList){
                                    if(user.contains("name_phoneNo=" + spaceLog.name_phoneNo)){
                                        key = user.split("=")[0];
                                        Log.d("jwj", key);
                                        break;
                                    }
                                }

                                databaseReference.child(spaceLog.code).child("User").child(key)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                userLayout.setVisibility(View.VISIBLE);
                                                userInfo.setVisibility(View.VISIBLE);
                                                spaceuser = snapshot.getValue(SpaceUser.class);
                                                Glide.with(userImage).load(spaceuser.faceImgUrl).into(userImage);
                                                userName.setText(spaceuser.name);
                                                userPhoneNo.setText(spaceuser.phoneNo);
                                                userAdd.setText(spaceuser.userLocation);

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }
            else if(spaceLog.authMethod.equals("지문인식")){
                databaseReference.child(spaceLog.code).child("User").orderByKey()
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String snapshotData = snapshot.getValue().toString();
                                snapshotData = snapshotData.substring(1, snapshotData.length() - 1);
                                String[] userList = snapshotData.split("\\}, ");
                                String key = "/";
                                for (String user: userList){
                                    if(user.contains("name_phoneNo=" + spaceLog.name_phoneNo)){
                                        key = user.split("=")[0];
                                        break;
                                    }
                                }

                                databaseReference.child(spaceLog.code).child("User").child(key)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                img.setVisibility(View.GONE);
                                                spaceuser = snapshot.getValue(SpaceUser.class);
                                                Glide.with(userImage).load(spaceuser.faceImgUrl).into(userImage);
                                                userName.setText(spaceuser.name);
                                                userPhoneNo.setText(spaceuser.phoneNo);
                                                userAdd.setText(spaceuser.userLocation);
                                                userLayout.setVisibility(View.VISIBLE);
                                                userInfo.setVisibility(View.VISIBLE);

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }
            else {
                userInfo.setVisibility(View.VISIBLE);
            }
        }else {
            userInfo.setVisibility(View.VISIBLE);
        }
    }

}
