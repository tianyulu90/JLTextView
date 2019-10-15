package com.jliveui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.jlive_text_view.view.EndLineIndentationTextView;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EndLineIndentationTextView endLineIndentationTextView = findViewById(R.id.tv);
        endLineIndentationTextView.setText("ksjjlksjjlkfjsadlksjjlksjsjjlkklkfjsadlksjjl");

        endLineIndentationTextView.setOnFollowedClickListener(new EndLineIndentationTextView.OnFollowedClickListener() {
            @Override
            public void onClick(int state) {
                endLineIndentationTextView.changeState();
                Log.e(TAG, "onClick: state == " + state);
            }
        });

    }
}
