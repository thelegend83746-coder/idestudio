package com.idestudio.app.ui.create;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.idestudio.app.R;
import com.idestudio.app.core.constants.AppConstants;
import com.idestudio.app.data.models.TemplateModel;

import java.util.List;

public class ChooseTemplateActivity extends AppCompatActivity {

    private RecyclerView recyclerTemplates;
    private TemplateAdapter adapter;
    private MaterialButton btnExit;
    private MaterialButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_template);

        initViews();
        setupListeners();
    }

    private void initViews() {
        recyclerTemplates = findViewById(R.id.recycler_templates);
        btnExit = findViewById(R.id.btn_template_exit);
        btnNext = findViewById(R.id.btn_template_next);

        recyclerTemplates.setLayoutManager(new LinearLayoutManager(this));
        List<TemplateModel> templates = TemplateModel.getTemplates();
        adapter = new TemplateAdapter(templates, null);
        recyclerTemplates.setAdapter(adapter);
    }

    private void setupListeners() {
        btnExit.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            TemplateModel selected = adapter.getSelectedTemplate();
            Intent intent = new Intent(ChooseTemplateActivity.this, ConfigureProjectActivity.class);
            intent.putExtra(AppConstants.EXTRA_TEMPLATE_NAME, selected.getName());
            startActivity(intent);
        });
    }
}
