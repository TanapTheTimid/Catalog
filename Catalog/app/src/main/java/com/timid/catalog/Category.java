package com.timid.catalog;

import android.net.Uri;

import java.io.Serializable;
import java.util.ArrayList;
/**
 * Created by Poom on 1/2/2017.
 *
 * This object stores the information about the category
 * including its elements and the NoteItem(s) it contains
 */

public class Category implements Serializable{
    private static final long serialVersionUID = 5804774077992278173L;

    public static class NoteItem implements Serializable{
        private static final long serialVersionUID = 7426195393078107360L;

        public NoteItem(String imgUrl){
            this.imgUrl = imgUrl;
        }

        public NoteItem(){}

        public String imgUrl;
        public String description;
        public String timeStamp;
    }

    private String categoryName = "";
    private String description = "";
    private ArrayList<NoteItem> noteItemList = new ArrayList<>();

    public Category(){}

    public Category(String categoryName){
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<NoteItem> getNoteItemList(){
        return noteItemList;
    }

    public String getCategoryName(){
        return categoryName;
    }

    public void setCategoryName(String categoryName){
        this.categoryName = categoryName;
    }

    public void addNoteItem(NoteItem noteItem){
        noteItemList.add(noteItem);
    }
}
