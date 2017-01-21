package com.timid.catalog;

import java.io.Serializable;
import java.util.ArrayList;
/**
 * Created by Poom on 1/2/2017.
 *
 * This object stores all the Category(s) in the application
 */

public class CategoryObjectStore implements Serializable{
    private static final long serialVersionUID = -9030184040530399635L;

    private ArrayList<Category> categoryList = new ArrayList<>();

    public void addCategory(Category category){
        categoryList.add(category);
    }

    public void removeCategory(Category category){
        categoryList.remove(category);
    }

    public ArrayList<Category> getCategoryList(){
        return categoryList;
    }
}
