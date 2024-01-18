package com.example.filemanager;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    ImageButton one;
    ListView listOfFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
        one= findViewById(R.id.one);

    }
    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<String>();
        private boolean[] selection;

        public void setData(List<String> data){
            if (data != null){
                this.data.clear();
                if (data.size() > 0){
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection){
            if (selection != null){
                this.selection = new boolean[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    this.selection[i] = selection[i];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item1,parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.item1)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if (selection != null){
                if (selection[position]){
                    holder.info.setBackgroundColor(Color.argb(50,5,5,5));
                }else {
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }

        class ViewHolder{
            TextView info;
            ViewHolder(TextView info){
                this.info = info;
            }
        }
    }
    private static final int REQUEST_PERMISSION = 1234;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSION_COUNT = 2;

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied() {
            int p = 0;
            while (p < PERMISSION_COUNT) {
                if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                p++;
            }
            return false;
        }

        private boolean isFileManagerInitialized = false;

    private boolean[] selection;
    private File[] files;
    private int filesFoundCount;
    private List<String> fileList;
    private ImageButton refreshButton;
    private File dir;

    private boolean isLongClick;
    private String currentPath;

    private String copyPath;
    private int selectedItemIndex;

    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSION);
            return;
        }
        if (!isFileManagerInitialized){
            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
            final TextView pathOutput = findViewById(R.id.pathOutput);
            final ListView listOfFiles = findViewById(R.id.listOfFiles);
            final TextAdapter textAdapter1 = new TextAdapter();
            listOfFiles.setAdapter(textAdapter1);
            fileList = new ArrayList<>();


            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));
                    dir = new File(currentPath);
                    files = dir.listFiles();
                    filesFoundCount = files.length;
                    selection = new boolean[filesFoundCount];
                    textAdapter1.setSelection(selection);

                    fileList.clear();
                    for (int i = 0; i < filesFoundCount; i++) {
                        fileList.add(String.valueOf(files[i].getAbsolutePath()));
                    }
                    textAdapter1.setData(fileList);
                }
            });

            refreshButton.callOnClick();

            final ImageButton goBackButton = findViewById(R.id.back);

            goBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)){
                        return;
                    }
                    currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                    refreshButton.callOnClick();
                }
            });

            listOfFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isLongClick) {
                                if (files[position].isDirectory()) {
                                    currentPath = files[position].getAbsolutePath();
                                    refreshButton.callOnClick();
                                }
                            }
                        }
                    },50);
                }
            });

            listOfFiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick = true;
                    selection[position] = !selection[position];
                    textAdapter1.setSelection(selection);
                    int selectionCount = 0;
                    for (int i = 0; i < selection.length; i++) {
                        if (selection[i]){
                            selectionCount++;
                        }
                    }
                    if (selectionCount>0){
                        findViewById(R.id.buttonBar).setVisibility(View.VISIBLE);
                        if (selectionCount ==1) {
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                            if (!files[selectedItemIndex].isDirectory()){
                                findViewById(R.id.copy).setVisibility(View.VISIBLE);
                            }
                        }else {
                            findViewById(R.id.copy).setVisibility(View.GONE);
                            {
                                findViewById(R.id.rename).setVisibility(View.GONE);
                            }
                        }
                    }else {
                        findViewById(R.id.buttonBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLongClick = false;
                        }
                    },1000);
                    return false;
                }
            });

            one.setOnClickListener(v -> {
                final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                deleteDialog.setTitle("Delete");
                deleteDialog.setMessage("Do you really want to delete this file?");
                deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        refreshButton.callOnClick();
                    }
                });
                deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < files.length; i++) {
                            if (selection[i]){
                                deleteFileOrFolder(files[i]);
                            }
                        }
                        refreshButton.callOnClick();
                    }
                });
                deleteDialog.show();
            });
            final ImageButton crateNewFolder = findViewById(R.id.newFolder);
            crateNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("new Folder");
                    final EditText input = new EditText(MainActivity.this);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(MainActivity.this.currentPath +"/"+input.getText());
                                    if (!newFolder.exists()){
                                        newFolder.mkdir();
                                        refreshButton.callOnClick();
                                    }
                                }
                            });
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    newFolderDialog.show();
                }
            });

//            RENAMING A FILE BELOW-----------------------------------------------------------------

            final Button rename = findViewById(R.id.rename);
            rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename to");
                    final EditText input = new EditText(MainActivity.this);
                    final String renamePath = files[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Rename",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String s = new File(renamePath).getParent()+"/"+input.getText();
                                    File newFile = new File(s);
                                    new File(renamePath).renameTo(newFile);
                                    refreshButton.callOnClick();
                                    selection = new boolean[files.length];
                                    textAdapter1.setSelection(selection);
                                }
                            });
                    renameDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    refreshButton.callOnClick();
                                }
                            });
                    renameDialog.show();
                }
            });

//            COPYING A FILE BELOW------------------------------------------------------------------

            final Button copy = findViewById(R.id.copy);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = files[selectedItemIndex].getAbsolutePath();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });

//            Pasting a file below------------------------------------------------------------------

            final Button paste = findViewById(R.id.paste);
            paste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    paste.setVisibility(View.GONE);
                    String dstPath = currentPath + copyPath.substring(copyPath.lastIndexOf('/'));
                    copy(new File(copyPath), new File(dstPath));
                    files = new File(currentPath).listFiles();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    refreshButton.callOnClick();
                }
            });

            isFileManagerInitialized = true;
            }else {
                refreshButton.callOnClick();
        }
        }

    private void copy(File src , File dst){
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] bfr = new byte[1024];
            int length;
            while ((length = in.read(bfr))>0){
                out.write(bfr,0,length);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void deleteFileOrFolder(File fileOrFolder){
        if (fileOrFolder.isDirectory()){
            if (fileOrFolder.list().length == 0){
                fileOrFolder.delete();
            }else {
                String files[] = fileOrFolder.list();
                for (String temp: files){
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (fileOrFolder.list().length==0){
                    fileOrFolder.delete();
                }
            }
        }else fileOrFolder.delete();
    }
    @SuppressLint("NewApi")
    public void setRequestPermissionResult(final int requestCode, final String[] permissions, final int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length>0){
            if (arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else onResume();
        }
    }
}