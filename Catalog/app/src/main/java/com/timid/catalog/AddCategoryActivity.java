package com.timid.catalog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.util.Log;

public class AddCategoryActivity extends AppCompatActivity {

    TextView catName;
    TextView catDesc;
    Button addBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_category);

        catName = (TextView) findViewById(R.id.new_cat_name);
        catDesc = (TextView) findViewById(R.id.new_cat_desc);
        addBtn = (Button) findViewById(R.id.add_cat_btn);
        //retrieve info from textbox then set result and finish activity
        addBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!catName.getText().toString().isEmpty()) {
                    Intent intent = new Intent();
                    intent.putExtra(MainActivity.INTENT_EXTRA_CATNAME, catName.getText().toString());
                    intent.putExtra(MainActivity.INTENT_EXTRA_CATDESC, catDesc.getText().toString());
                    setResult(RESULT_OK,intent);
                    finish();
                }
            }
        });
    }
}
