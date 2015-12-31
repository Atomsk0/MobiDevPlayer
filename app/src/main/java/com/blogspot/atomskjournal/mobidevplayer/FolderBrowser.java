package com.blogspot.atomskjournal.mobidevplayer;


import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;

public class FolderBrowser extends ListActivity {

    private ArrayList<Folder> directoryEntries = new ArrayList<Folder>();

    private File currentDirectory = new File("/");

    private FolderAdapter directoryList;

    @Override

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_browser);

        ImageButton buttonUp = (ImageButton) findViewById(R.id.up_button);
        ImageButton buttonOk = (ImageButton) findViewById(R.id.ok_button);
        ImageButton buttonCancel = (ImageButton) findViewById(R.id.cancel_button);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.up_button:
                        upOneLevel();
                        break;
                    case R.id.cancel_button:
                        finish();
                        break;
                    case R.id.ok_button:
                        pickCurrentAudio();

                        break;
                    default:break;
                }
            }
        };
        buttonUp.setOnClickListener(onClickListener);
        buttonCancel.setOnClickListener(onClickListener);
        buttonOk.setOnClickListener(onClickListener);

        //browse to root directory

        browseTo(new File("/"));

    }

    //browse one level up

    private void upOneLevel(){

        if(this.currentDirectory.getParent() != null) {

            this.browseTo(this.currentDirectory.getParentFile());

        } else {
            finish();
        }

    }

    //browse to file or directory

    private void browseTo(final File aDirectory){

        //if we want to browse directory

        if (aDirectory.isDirectory()){

            currentDirectory = aDirectory;
            fill(aDirectory.listFiles());
            TextView titleManager = (TextView) findViewById(R.id.titleManager);
            titleManager.setText(aDirectory.getAbsolutePath());

        } else {

            //if we want to open file

            currentDirectory = aDirectory;
            pickCurrentAudio();

        }
    }

    //fill list of folders/files

    private void fill(File[] files) {


        this.directoryEntries.clear();


        for (File file : files) {

            if (!file.isDirectory() && file.getAbsolutePath().substring(file.getAbsolutePath()
                        .lastIndexOf(".")+1).matches("mp3|aac|ogg|wav|flac")) {

                this.directoryEntries.add(new Folder(file.getAbsolutePath()));

            } else if (file.isDirectory()) {

                    this.directoryEntries.add(new Folder(file.getAbsolutePath()));
                }
            }

        directoryList = new FolderAdapter(getBaseContext(), this.directoryEntries);

        this.setListAdapter(directoryList);

    }

    //when you clicked onto item

    @Override

    protected void onListItemClick(ListView l, View v, int position, long id) {

        int selectionRowID = position;

        String selectedFileString = this.directoryEntries.get(selectionRowID).getAbsolutePath();

        File clickedFile = clickedFile = new File(selectedFileString);

            if (clickedFile != null)

                try {
                    this.browseTo(clickedFile);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    directoryList.notifyDataSetChanged();
                }
    }

    private void pickCurrentAudio() {

        MainActivity.setTrackUri(currentDirectory.getAbsolutePath());
        MainActivity.fromBrowser = true;
        finish();

    }

}
