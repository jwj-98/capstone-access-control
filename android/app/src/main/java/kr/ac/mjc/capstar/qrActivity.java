package kr.ac.mjc.capstar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class qrActivity extends AppCompatActivity {

    EditText nameEt;
    EditText phoneNoEt;
    EditText locationEt;
    ImageView generateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        nameEt = findViewById(R.id.et_name);
        phoneNoEt = findViewById(R.id.et_pnumber);
        generateBtn = findViewById(R.id.submit_btn);
        locationEt = findViewById(R.id.et_location);

        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String jsonData = "{\"code\": \"" + locationEt.getText().toString() + "\", \"name\": \"" +
                        nameEt.getText().toString() + "\", \"phoneNo\": \"" + phoneNoEt.getText().toString() + "\"}";
                Intent intent = new Intent(qrActivity.this, QrImagePopup.class);
                intent.putExtra("data", jsonData);
                startActivity(intent);
            }
        });
    }
}