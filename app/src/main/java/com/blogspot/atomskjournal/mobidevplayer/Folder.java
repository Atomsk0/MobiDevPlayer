package com.blogspot.atomskjournal.mobidevplayer;


import java.io.File;
import java.io.IOException;


/**
 * Created by Максим on 24.12.2015.
 */
public class Folder extends File {
    private String name;

    public Folder(String path) {
        super(path);
    }


    public String getName() {

        try {
            return this.getCanonicalPath().substring(this.getCanonicalPath().lastIndexOf("/") + 1);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }









}
