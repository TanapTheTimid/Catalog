package com.timid.catalog;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.*;

import java.io.*;

import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.*;
import android.app.*;
import android.content.Intent;
import android.widget.*;
import android.os.Build;
import android.provider.MediaStore;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.content.pm.PackageManager;
import android.annotation.TargetApi;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    //final codes
    public static final String[] PERMISSIONS = {"android.permission.CAMERA"
            , "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    public static final String OBJECTSTORE_FILENAME = "object_store";//object storage filename
    //activity request codes
    public static final int CREATE_NEW_CATEGORY_REQUEST = 1;
    public static final int IMAGE_CAPTURE_REQUEST = 2;
    public static final int PERMISSION_REQUEST = 3;
    public static final int PICK_PHOTO_REQUEST = 4;
    public static final int DELETE_CATEGORY_REQUEST = 5;
    //intent bundle extra string code
    public static final String INTENT_EXTRA_CATNAME = "new_category_name";
    public static final String INTENT_EXTRA_CATDESC = "new_category_description";
    public static final String INTENT_EXTRA_CATEGORY_POS = "category_position";
    //display variables
    public static float mmtopx;
    public static float dptopx;
    public static Display display;
    public static Point displayPoint;
    //recycler view variables
    private RecyclerView categoriesView;
    private RecyclerAdapter rAdapter;
    private RecyclerView.LayoutManager rLayoutManager;
    //current working variables for dealing with adding image and taking pictures
    private Category currentCategory;
    private File currentImageFile;

    //deserialized objectstore
    public static CategoryObjectStore categoryObjectStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check permissions on only android 6 or newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkPermissions();
        //get display vars
        display = getWindowManager().getDefaultDisplay();
        displayPoint = new Point();
        display.getSize(displayPoint);
        mmtopx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1,
                getResources().getDisplayMetrics());
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dptopx = metrics.density;

        setupFabBtn();
        getObjectStore();

        //create recycler view and set attributes
        categoriesView = (RecyclerView) findViewById(R.id.categories_view);
        categoriesView.setHasFixedSize(false);

        rLayoutManager = new LinearLayoutManager(this);
        categoriesView.setLayoutManager(rLayoutManager);
        //create recycler adapter with callback methods
        rAdapter = new RecyclerAdapter(categoryObjectStore.getCategoryList(),
                new RecyclerAdapter.ClickActionCallback() {

                    private boolean menuState = false;

                    @Override
                    public void onCameraRequest(Category category) {
                        currentCategory = category;

                        takePicture(category);
                    }

                    @Override
                    public void onGalleryRequest(Category category) {
                        currentCategory = category;

                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhotoIntent, PICK_PHOTO_REQUEST);
                    }

                    @Override
                    public void onOpenCategory(int pos) {

                        Intent intent = new Intent(MainActivity.this, ViewCategoryActivity.class);
                        intent.putExtra(INTENT_EXTRA_CATEGORY_POS, pos);
                        startActivityForResult(intent, DELETE_CATEGORY_REQUEST);
                    }

                    final int ANIM_DURATION = 200;

                    @Override
                    public void onMoreOptions(final View v, View src){
                        ValueAnimator va = new ValueAnimator();
                        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){

                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                int value = (int) (animation.getAnimatedValue());
                                v.getLayoutParams().width = value;
                                v.requestLayout();
                            }
                        });

                        if(!menuState) {
                            menuState = true;
                            ImageView imv = (ImageView) src;
                            imv.setImageResource(R.drawable.ic_right_more);

                            int original = v.getLayoutParams().width;
                            va.setIntValues(original, original + (int)(72 * dptopx));
                        }else{
                            menuState = false;
                            ImageView imv = (ImageView) src;
                            imv.setImageResource(R.drawable.ic_left_more);

                            int original = v.getLayoutParams().width;
                            va.setIntValues(original, original - (int)(72 * dptopx));
                        }
                        va.setInterpolator(new DecelerateInterpolator());
                        va.setDuration(ANIM_DURATION);
                        va.start();
                    }
                });
        categoriesView.setAdapter(rAdapter);
    }
    //check permission only on api 23 and above
    @TargetApi(23)
    private void checkPermissions() {
        int x = 0;
        for (String perm : PERMISSIONS) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                x++;
            }
        }
        if (x > 0) {
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST);
        }
    }
    //handles permission denial
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Rejected")
                        .setMessage("This app requires all of the requested permissions to" +
                                "be fully functional. Please accept all permissions to use" +
                                "this application. This application does not collect your " +
                                "personal information").show();
            }
        }
    }
    //starts an activity that takes pictures
    private void takePicture(Category category) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(category);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.timid.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST);
            }
        }
    }
    //create filename for storing picture + makes the directory for the category if it does not exist
    private File createImageFile(Category category) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        String imageFileName = category.getCategoryName() + timeStamp + "_";

        //uses the default PICTURES directory and creates a new directory for the category
        //if that is needed
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                File.separator + category.getCategoryName());

        storageDir.mkdir();

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentImageFile = image;

        return image;
    }

    //changes the back button function accordingly
    @Override
    public void onBackPressed() {
        if (plusBtnOpened) {
            closePlusBtnMenu();
        } else {
            super.onBackPressed();
        }
    }

    //variables for animating floating action buttons
    private boolean plusBtnOpened;
    private int yoffset;
    private int duration = 200;
    private int startDelay = 60;
    private FloatingActionButton plusBtn;
    private FloatingActionButton cameraBtn;
    private FloatingActionButton newCatBtn;

    private void closePlusBtnMenu() {
        plusBtnOpened = false;
        cameraBtn.setClickable(false);
        newCatBtn.setClickable(false);
        //animate disappearance
        cameraBtn.clearAnimation();
        newCatBtn.clearAnimation();
        cameraBtn.animate().alpha(0).setDuration(duration).setStartDelay(0);
        newCatBtn.animate().alpha(0).setDuration(duration).setStartDelay(0);
    }

    private static final int LOLLIPOP_OFFSET = 8;

    //sets up fab buttons
    private void setupFabBtn() {
        plusBtn = (FloatingActionButton) findViewById(R.id.plus_btn);
        cameraBtn = (FloatingActionButton) findViewById(R.id.camera_btn);
        newCatBtn = (FloatingActionButton) findViewById(R.id.new_category_btn);
        plusBtnOpened = false;

        yoffset = (int) mmtopx * 2;//y start offset for the animation
        //compensate for static shadow in android bellow lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            plusBtn.setY(plusBtn.getY() + dptopx * LOLLIPOP_OFFSET);
            plusBtn.setX(plusBtn.getX() + dptopx * LOLLIPOP_OFFSET);

            cameraBtn.setX(cameraBtn.getX() + dptopx * LOLLIPOP_OFFSET);
            cameraBtn.setY(cameraBtn.getY() + dptopx * LOLLIPOP_OFFSET * 8);

            newCatBtn.setX(newCatBtn.getX() + dptopx * LOLLIPOP_OFFSET);
            newCatBtn.setY(newCatBtn.getY() + dptopx * LOLLIPOP_OFFSET * 4);
        }
        //disables click at first
        cameraBtn.setClickable(false);
        newCatBtn.setClickable(false);

        cameraBtn.setAlpha((float) 0);
        newCatBtn.setAlpha((float) 0);
        //open the quick menu when plus btn is pressed
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!plusBtnOpened) {
                    plusBtnOpened = true;
                    cameraBtn.setClickable(true);
                    newCatBtn.setClickable(true);
                    cameraBtn.setY(cameraBtn.getY() + yoffset);
                    newCatBtn.setY(newCatBtn.getY() + yoffset);

                    cameraBtn.clearAnimation();
                    newCatBtn.clearAnimation();
                    newCatBtn.animate().alpha(1).yBy(-yoffset).setDuration(duration).setStartDelay(0);
                    cameraBtn.animate().alpha(1).yBy(-yoffset).setDuration(duration).setStartDelay(startDelay);
                } else {
                    closePlusBtnMenu();
                }
            }
        });
        //add new category
        newCatBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddCategoryActivity.class);
                startActivityForResult(intent, CREATE_NEW_CATEGORY_REQUEST);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //result from create new category
        //gets data, create the category, add to categoryObjectStore, then save objectstore
        //notify add category to adapter
        if (requestCode == CREATE_NEW_CATEGORY_REQUEST && resultCode == RESULT_OK) {
            String newCatName = data.getStringExtra(INTENT_EXTRA_CATNAME);
            String newCatDesc = data.getStringExtra(INTENT_EXTRA_CATDESC);

            Category newCat = new Category(newCatName);
            newCat.setDescription(newCatDesc);
            categoryObjectStore.addCategory(newCat);

            saveObjectStore(this);

            rAdapter.notifyItemInserted(categoryObjectStore.getCategoryList().size());
        }
        //result from image capture
        if (requestCode == IMAGE_CAPTURE_REQUEST) {
            if(resultCode == RESULT_OK) {
                //get the path of current working file, set timestamp, and add object
                Category.NoteItem item = new Category.NoteItem(currentImageFile.getAbsolutePath());
                item.timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date(currentImageFile.lastModified()));
                currentCategory.addNoteItem(item);

                saveObjectStore(this);

                //make the file known to gallery app
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

                Uri contentUri = Uri.fromFile(currentImageFile);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "New photo added!", Toast.LENGTH_SHORT).show();//notify user
            }else{
                currentImageFile.delete();
            }
        }
        //result from picking picture
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = data.getData();
            //get the path of file
            //get File compatible path
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(photoUri, projection, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String path = cursor.getString(columnIndex);
            cursor.close();
            // create new noteitem with path
            Category.NoteItem newItem = new Category.NoteItem(path);
            //set time stamp
            File file = new File(path);
            newItem.timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date(file.lastModified()));

            currentCategory.addNoteItem(newItem);
            saveObjectStore(this);

            Toast.makeText(this, "Added photo from gallery!", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == DELETE_CATEGORY_REQUEST && resultCode == RESULT_OK){
            int pos = data.getIntExtra(INTENT_EXTRA_CATEGORY_POS,-1);
            Log.e("pos" , pos + "");
            if(pos != -1) {
                rAdapter.notifyItemRemoved(pos);
            }
        }
    }

    //get object from file
    private void getObjectStore() {
        try {
            FileInputStream fis = getApplicationContext().openFileInput(OBJECTSTORE_FILENAME);
            try {
                ObjectInputStream ois = new ObjectInputStream(fis);
                categoryObjectStore = (CategoryObjectStore) ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fis.close();
        } catch (FileNotFoundException e) {
            categoryObjectStore = new CategoryObjectStore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //saves object
    public static void saveObjectStore(Context c) {
        try {
            FileOutputStream fos = c.openFileOutput(OBJECTSTORE_FILENAME, MODE_PRIVATE);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(categoryObjectStore);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(c, "File write fail", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {

        }
    }
}
