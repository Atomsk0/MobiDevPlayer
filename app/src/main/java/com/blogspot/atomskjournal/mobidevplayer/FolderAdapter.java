package com.blogspot.atomskjournal.mobidevplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Максим on 24.12.2015.
 */
public class FolderAdapter extends BaseAdapter{

    private ArrayList<Folder> folders;
    private LayoutInflater foldersInflater;

    public FolderAdapter(Context context, ArrayList<Folder> folders) {
        this.folders = folders;
        foldersInflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return folders.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout foldersLayout = (LinearLayout)foldersInflater.inflate(R.layout.folder,
                parent, false);
        TextView folderName = (TextView) foldersLayout.findViewById(R.id.folder_name);
        ImageView folderIcon = (ImageView) foldersLayout.findViewById(R.id.folder_icon);

        Folder currentFolder = folders.get(position);

        folderName.setText(currentFolder.getName());
        if (currentFolder.isFile()) {
            folderIcon.setImageResource(R.drawable.ic_track_idle);
        }

        return foldersLayout;
    }
}
