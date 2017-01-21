package com.timid.catalog;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.support.v4.app.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.content.res.Configuration;

import com.squareup.picasso.Picasso;

import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;

public class ViewCategoryActivity extends AppCompatActivity {
    //constants
    private RecyclerView recyclerView;
    private ImagePreviewRecyclerAdapter adapter;
    private Category category;
    private int categoryPosition;
    //layout variables
    private LinearLayout imvHolder;
    private ImageView imv;
    private Toolbar previewToolbar;
    private Toolbar fullviewToolbar;
    private RelativeLayout bgColorer;
    private RelativeLayout viewingArea;
    //the viewing state - used for back button action
    static AtomicBoolean inFullView = new AtomicBoolean(false);

    //caching the view for orientation change
    static private int currentPos;
    static private View v;

    //public orientation var
    public static boolean isPortrait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_view_category);
        //checks orientation
        isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        //for displaying full image - frame
        imvHolder = (LinearLayout) findViewById(R.id.full_img_view_holder);
        imvHolder.setAlpha((float)0);

        //-image view
        imv = (ImageView) findViewById(R.id.full_img_view);
        final ScaleGestureDetector scaleListener =
                new ScaleGestureDetector(getApplicationContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        imv.setScaleY(imv.getScaleY()*detector.getScaleFactor());
                        imv.setScaleX(imv.getScaleX()*detector.getScaleFactor());
                        imv.requestLayout();
                        return true;
                    }
                });

        imv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleListener.onTouchEvent(event);
                return true;
            }
        });

        //for darkening the background behind imv when in full screen view
        bgColorer = (RelativeLayout) findViewById(R.id.bg_colorer);
        bgColorer.setAlpha((float) 0);
        //viewing area for calculating height in landscape
        viewingArea = (RelativeLayout) findViewById(R.id.viewing_area);
        //get category from the position given in the intent
        categoryPosition = getIntent().getIntExtra(MainActivity.INTENT_EXTRA_CATEGORY_POS,-1);
        category = MainActivity.categoryObjectStore.getCategoryList().get(categoryPosition);
        Log.e("category", category.getCategoryName());
        //create 2 toolbars, one for preview, one for full screen. hide the fullscreen one first
        previewToolbar = (Toolbar) findViewById(R.id.toolbar_view_img);
        fullviewToolbar = (Toolbar) findViewById(R.id.toolbar_full_screen);
        fullviewToolbar.setAlpha((float) 0);
        fullviewToolbar.setVisibility(View.GONE);

        //sets fake shadow for devices below lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            previewToolbar.setBackground(ContextCompat.getDrawable(getApplicationContext()
                    , R.drawable.shadow_bg));
        }
        //set support action bar attributes
        setSupportActionBar(previewToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(category.getCategoryName());

        recyclerView = (RecyclerView) findViewById(R.id.view_image_recycler);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));

        //item decoration for offsetting images and giving uniform margin
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration(){
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state){
                super.getItemOffsets(outRect, view, parent, state);
                int position = parent.getChildLayoutPosition(view);
                //offsets the second and third images of the row accordingly
                if(position % 3 == 1)
                    outRect.set((int) (MainActivity.dptopx * 1.5),0,0,0);
                else if(position % 3 == 2)
                    outRect.set((int) (MainActivity.dptopx * 3),0,0,0);
            }
        });

        adapter = new ImagePreviewRecyclerAdapter(category.getNoteItemList()
                ,new ImagePreviewRecyclerAdapter.ActionCallback(){
            //callback
            public void onImageClick(int pos, View v){
                if(!inFullView.get()) {
                    inFullView.set(true);
                    ViewCategoryActivity.currentPos = pos;
                    ViewCategoryActivity.v = v;
                    openImage(category.getNoteItemList().get(pos).imgUrl, (ImageView) v,true);
                }
            }
        });

        recyclerView.setAdapter(adapter);

        if(inFullView.get()){
            openImage(category.getNoteItemList().get(ViewCategoryActivity.currentPos).imgUrl, (ImageView)ViewCategoryActivity.v ,false);
        }
    }

    public int getStatusBarHeight(){
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height","dimen", "android");
        if(resourceId > 0){
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getActionBarSize(){
        // Calculate ActionBar's height
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }

        return actionBarHeight;
    }

    //get the margin of the parents going up (depth) steps
    //top margin
    public float getTop(int depth, View v){
        depth--;
        View vp = (View) v.getParent();
        return depth > 0 ? getTop(depth-1,vp)+vp.getY() : vp.getY();
    }
    //get the margin of the parents going up (depth) steps
    //left margin
    public float getLeft(int depth, View v){
        depth--;
        View vp = (View) v.getParent();
        return depth > 0 ? getTop(depth,vp)+vp.getX() : vp.getX();
    }

    //animation related variables
    //constant
    private final int ANIM_DUR = 200;
    //cache variables
    float currentX;
    float currentY;
    int originalW;
    int originalHHolder;
    int originalHImage;

    //open full image
    public void openImage(String imgUrl,final ImageView v,boolean useAnimation){

        int anim_dur = ANIM_DUR;
        if(!useAnimation){
            anim_dur = 0;
        }
        //calculate final width and height according to orientation
        //also calculate the x offset
        int finalW = MainActivity.displayPoint.x;
        int finalH = MainActivity.displayPoint.y - getStatusBarHeight() - getActionBarSize();
        float xoffset = 0;

        if(!isPortrait){
            finalW = MainActivity.displayPoint.y ;
            finalH = MainActivity.displayPoint.x - getStatusBarHeight() - getActionBarSize();
            xoffset = (float)(MainActivity.displayPoint.y - finalW)/(float)2;
        }

        fullviewToolbar.setVisibility(View.VISIBLE);

        File imgFile = new File(imgUrl);
        //find the height of the image that corresponds to the width ofthe screen
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgUrl,options);
        float scaleFactorFull = Math.max((float)options.outWidth/ (float)finalW,(float)options.outHeight/ (float)finalH);
        final float fullH = options.outHeight/scaleFactorFull;
        final float fullW = options.outWidth/scaleFactorFull;

        //load full image with picasso
        Picasso.with(v.getContext())
                .load(imgFile)
                .placeholder(v.getDrawable())
                .into(imv);

        //moves the image view to the location of the corresponding preview
        //get the location relative to imv
        currentX = v.getX()+ getLeft(3,v);// + (v.getLayoutParams().width/2);
        currentY = v.getY()+ getTop(3,v);// + (v.getLayoutParams().height/2);

        imvHolder.setX(currentX);
        imvHolder.setY(currentY);

        //sets value for the back animation
        originalW = v.getWidth();
        originalHHolder = ((View)v.getParent()).getHeight();

        originalHImage = v.getHeight();

        //animate width of imvholder
        ValueAnimator holderVax = new ValueAnimator();
        holderVax.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imvHolder.getLayoutParams().width = value;
                imvHolder.requestLayout();
            }
        });
        holderVax.setIntValues(originalW,finalW);
        holderVax.setDuration(anim_dur);
        holderVax.setInterpolator(new AccelerateDecelerateInterpolator());

        //animate height of imvholder
        ValueAnimator holderVay = new ValueAnimator();
        holderVay.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imvHolder.getLayoutParams().height = value;
                imvHolder.requestLayout();
            }
        });
        holderVay.setIntValues(originalHHolder,(int)fullH);
        holderVay.setDuration(anim_dur);
        holderVay.setInterpolator(new AccelerateDecelerateInterpolator());


        //animate width of image view
        ValueAnimator imvVax = new ValueAnimator();
        imvVax.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imv.getLayoutParams().width = value;
                imv.requestLayout();
            }
        });
        imvVax.setIntValues(originalW,finalW);
        imvVax.setDuration(anim_dur);
        imvVax.setInterpolator(new AccelerateDecelerateInterpolator());

        //animate height of image view
        ValueAnimator imvVay = new ValueAnimator();
        imvVay.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imv.getLayoutParams().height = value;
                imv.requestLayout();
            }
        });
        imvVay.setIntValues(v.getHeight(),(int)fullH);
        imvVay.setDuration(anim_dur);
        imvVay.setInterpolator(new AccelerateDecelerateInterpolator());

        //set start scale value
        imvHolder.setScaleX(1);
        imvHolder.setScaleY(1);
        imvHolder.setAlpha((float) 1);
        imvHolder.clearAnimation();
        //animate background
        bgColorer.animate().setInterpolator(new AccelerateDecelerateInterpolator()).alpha(1).setDuration(anim_dur);
        //make the top toolbar visible
        fullviewToolbar.animate().setInterpolator(new AccelerateDecelerateInterpolator()).alpha(1).setDuration(anim_dur);
        //animate imageview x,y, alpha
        imvHolder.animate().setListener(null)
                .setInterpolator(new AccelerateDecelerateInterpolator()).x(0+xoffset)
                            .y(0).setDuration(anim_dur);
        //start the width and height animation
        //holder anim
        holderVax.start();
        holderVay.start();
        //image view anim
        imvVax.start();
        imvVay.start();

        //set the status bar color if possible
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }

    }

    public void closeImage(){
        inFullView.set(false);

        //animate width
        ValueAnimator holderVax = new ValueAnimator();
        holderVax.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imvHolder.getLayoutParams().width = value;
                imvHolder.requestLayout();
            }
        });
        holderVax.setIntValues(imvHolder.getLayoutParams().width,originalW);
        holderVax.setDuration(ANIM_DUR);
        holderVax.setInterpolator(new AccelerateDecelerateInterpolator());

        //animate height
        ValueAnimator holderVay = new ValueAnimator();
        holderVay.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imvHolder.getLayoutParams().height = value;
                imvHolder.requestLayout();
            }
        });
        holderVay.setIntValues(imvHolder.getLayoutParams().height, originalHHolder);
        holderVay.setDuration(ANIM_DUR);
        holderVay.setInterpolator(new AccelerateDecelerateInterpolator());

        //animate width of image view
        ValueAnimator imvVax = new ValueAnimator();
        imvVax.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imv.getLayoutParams().width = value;
                imv.requestLayout();
            }
        });
        imvVax.setIntValues(imv.getLayoutParams().width,originalW);
        imvVax.setDuration(ANIM_DUR);
        imvVax.setInterpolator(new AccelerateDecelerateInterpolator());

        //animate height of image view
        ValueAnimator imvVay = new ValueAnimator();
        imvVay.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation){
                int value = (int) animation.getAnimatedValue();
                imv.getLayoutParams().height = value;
                imv.requestLayout();
            }
        });
        imvVay.setIntValues(imv.getLayoutParams().height,originalHImage);
        imvVay.setDuration(ANIM_DUR);
        imvVay.setInterpolator(new AccelerateDecelerateInterpolator());


        fullviewToolbar.animate().setInterpolator(new AccelerateDecelerateInterpolator()).alpha(0).setDuration(ANIM_DUR);
        bgColorer.animate().setInterpolator(new AccelerateDecelerateInterpolator()).alpha(0).setDuration(ANIM_DUR);

        imvHolder.clearAnimation();
        imvHolder.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                /*.alpha(0).scaleX((float)0.5).scaleY((float)0.5)*/.setDuration(ANIM_DUR).x(currentX).y(currentY)
                .setListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        imvHolder.getLayoutParams().width = 0;
                        imvHolder.getLayoutParams().height = 0;
                        imvHolder.setX(0);
                        imvHolder.setY(0);
                        imvHolder.setAlpha((float) 0);
                        fullviewToolbar.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }
                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

        holderVax.start();
        holderVay.start();
        imvVax.start();
        imvVay.start();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(imvHolder.getContext(),R.color.colorPrimaryDark));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_category_menu,menu);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        //called to fix rendering problem upon relaunching app
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed(){
        if(!inFullView.get())
            super.onBackPressed();
        else
            closeImage();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.delete_category:
                new AlertDialog.Builder(this)
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete this category?" +
                                " Your images will not be deleted and will still " +
                                "be accessable through your gallery application.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int button){
                                deleteCategory();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteCategory(){
        MainActivity.categoryObjectStore.getCategoryList().remove(category);
        MainActivity.saveObjectStore(getApplicationContext());
        Intent intent = new Intent();
        intent.putExtra(MainActivity.INTENT_EXTRA_CATEGORY_POS, categoryPosition );
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onBackFullview(View v){
        if(inFullView.get()){
            closeImage();
        }
    }

    public void onDeletePicFullview(View v){
        Category.NoteItem item = category.getNoteItemList().get(currentPos);

        //get the content URI for the photo
        // the uri for external image location
        Uri photoUri = MediaStore.Images.Media.getContentUri("external");
        String[] projection = {MediaStore.Images.ImageColumns._ID};
        //TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = getContentResolver().query(photoUri, projection, MediaStore.Images.ImageColumns.DATA + " LIKE ?", new String[] { item.imgUrl }, null);
        cursor.moveToFirst();
        //get column index
        int columnIndex = cursor.getColumnIndex(projection[0]);
        long photoId = cursor.getLong(columnIndex);
        cursor.close();
        //get the complete uri of the image
        Uri uri = Uri.parse(photoUri.toString() + "/" + photoId);

        //delete image
        getContentResolver().delete(uri,null,null);

        category.getNoteItemList().remove(item);
        MainActivity.saveObjectStore(getApplicationContext());
        closeImage();
        adapter.notifyItemRemoved(currentPos);
    }
}
