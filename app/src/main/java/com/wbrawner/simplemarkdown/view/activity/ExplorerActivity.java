package com.wbrawner.simplemarkdown.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.wbrawner.simplemarkdown.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class ExplorerActivity extends AppCompatActivity {
    private Handler fileHandler = new Handler();
    private ListView listView;
    private File[] mounts;
    private String docsDirPath;
    private String defaultDocsDirPath;
    private int requestCode;
    private String filePath;
    private boolean isSave = false;
    private EditText fileName;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(MainActivity.EXTRA_REQUEST_CODE)) {
            finish();
            return;
        }

        defaultDocsDirPath = Environment.getExternalStorageDirectory() + "/" +
                Environment.DIRECTORY_DOCUMENTS;
        docsDirPath = defaultDocsDirPath;

        requestCode = intent.getIntExtra(MainActivity.EXTRA_REQUEST_CODE, -1);
        switch (requestCode) {
            case MainActivity.OPEN_FILE_REQUEST:
                break;
            case MainActivity.SAVE_FILE_REQUEST:
                isSave = true;
                fileName = findViewById(R.id.file_name);
                fileName.setVisibility(View.VISIBLE);
                if (intent.hasExtra(MainActivity.EXTRA_FILE)) {
                    File file = (File) intent.getSerializableExtra(MainActivity.EXTRA_FILE);
                    if (file.exists() && file.canWrite()) {
                        docsDirPath = file.getParentFile().getAbsolutePath();
                        fileName.setText(file.getName());
                    } else {
                        fileName.setText("Untitled.md");
                    }
                }
                saveButton = findViewById(R.id.button_save);
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener((v) -> {
                    Intent fileIntent = new Intent();
                    String absolutePath = String.format(
                            Locale.ENGLISH,
                            "%s/%s",
                            filePath,
                            fileName.getText().toString()
                    );
                    fileIntent.putExtra(MainActivity.EXTRA_FILE_PATH, absolutePath);
                    setResult(RESULT_OK, fileIntent);
                    finish();
                });
                break;
            default:
                finish();
                return;
        }
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(view ->
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show()
//        );

        listView = findViewById(R.id.file_list);
        File docsDir = new File(docsDirPath);
        if (!docsDir.exists()) {
            docsDir = Environment.getExternalStorageDirectory();
        }
        updateListView(docsDir);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (hasRemovableStorage()) {
            getMenuInflater().inflate(R.menu.menu_explorer, menu);
            if (filePath.contains(mounts[1].getAbsolutePath())) {
                menu.findItem(R.id.action_use_sdcard).setChecked(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_use_sdcard:
                if (!hasRemovableStorage()) {
                    // We shouldn't get here to begin with but better safe than sorry
                    break;
                }
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    updateListView(mounts[1]);
                } else {
                    updateListView(new File(defaultDocsDirPath));
                }
        }
        return true;
    }

    private boolean hasRemovableStorage() {
        mounts = getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS);
        return mounts.length > 1;
    }

    private List<HashMap<String, Object>> loadFiles(File docsDir) {
        TreeSet files = new TreeSet<HashMap<String, Object>>((o1, o2) ->
                ((String) o1.get("name")).compareToIgnoreCase((String) o2.get("name"))) {
        };
        TreeSet dirs = new TreeSet<HashMap<String, Object>>((o1, o2) ->
                ((String) o1.get("name")).compareToIgnoreCase((String) o2.get("name"))) {
        };
        if (docsDir.getParentFile() != null && docsDir.getParentFile().canRead()) {
            HashMap<String, Object> fileHashMap = new HashMap<>();
            fileHashMap.put("name", "..");
            fileHashMap.put("file", docsDir.getParentFile());
            dirs.add(fileHashMap);
        }

        if (docsDir.listFiles() != null) {
            for (File file : docsDir.listFiles()) {
                if (file.isDirectory()) {
                    HashMap<String, Object> fileHashMap = new HashMap<>();
                    fileHashMap.put("name", file.getName());
                    fileHashMap.put("file", file);
                    dirs.add(fileHashMap);
                    continue;
                }
                if (!file.getName().endsWith("md")
                        && !file.getName().endsWith("markdown")
                        && !file.getName().endsWith("text")) {
                    continue;
                }
                HashMap<String, Object> fileHashMap = new HashMap<>();
                fileHashMap.put("name", file.getName());
                fileHashMap.put("file", file);
                files.add(fileHashMap);
            }
        }

        List<HashMap<String, Object>> sortedFiles = new ArrayList<>(dirs);
        sortedFiles.addAll(files);

        return sortedFiles;
    }

    private void updateListView(File filesDir) {
        setTitle(filesDir.getName());
        filePath = filesDir.getAbsolutePath();
        fileHandler.post(() -> {
            List<HashMap<String, Object>> files = loadFiles(filesDir);

            listView.setAdapter(new SimpleAdapter(
                    this,
                    files,
                    android.R.layout.simple_list_item_1,
                    new String[]{"name"},
                    new int[]{android.R.id.text1}
            ));

            listView.setOnItemClickListener((parent, view, position, id) -> {
                File clickedFile = (File) files.get(position).get("file");
                if (clickedFile.isFile()) {
                    handleFileClick(clickedFile);
                } else if (clickedFile.isDirectory()) {
                    updateListView(clickedFile);
                }
            });
        });
    }

    void handleFileClick(File file) {
        if (isSave) {
            if (fileName != null) {
                fileName.setText(file.getName());
            }
        } else {
            Intent fileIntent = new Intent();
            fileIntent.putExtra(MainActivity.EXTRA_FILE, file);
            setResult(RESULT_OK, fileIntent);
            finish();
        }
    }
}