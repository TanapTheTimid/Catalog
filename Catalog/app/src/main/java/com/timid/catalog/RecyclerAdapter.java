package com.timid.catalog;

import android.animation.ValueAnimator;
import android.graphics.Point;
import android.support.v7.widget.*;
import android.util.Log;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import java.util.ArrayList;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder>{

    private ArrayList<Category> categoryList = new ArrayList<>();
    private ClickActionCallback callback;

    public RecyclerAdapter(ArrayList<Category> categoryList, ClickActionCallback callback){
        this.categoryList = categoryList;
        this.callback = callback;
    }

    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_element,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.ViewHolder holder, int position) {
        holder.name.setText(categoryList.get(position).getCategoryName());
        holder.desc.setText(categoryList.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView desc;
        CardView card;
        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name_text);
            card = (CardView) itemView.findViewById(R.id.card_view_cat);
            desc = (TextView) itemView.findViewById(R.id.desc_text);
            final View quickAction = itemView.findViewById(R.id.quick_action);
            final ImageButton more = (ImageButton) itemView.findViewById(R.id.cat_btn_more);

            //reduce card width and set X to simulate margin
            card.getLayoutParams().width = MainActivity.displayPoint.x - (int)(8*MainActivity.dptopx);
            card.setX(4 * MainActivity.dptopx);
            //button for quickly taking picture
            ImageButton camera = (ImageButton) itemView.findViewById(R.id.photo_to_cat_btn);
            camera.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    callback.onCameraRequest(categoryList.get(getAdapterPosition()));
                }
            });
            //get pic from gallery
            ImageButton gallery = (ImageButton) itemView.findViewById(R.id.photo_from_gal_btn);
            gallery.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    callback.onGalleryRequest(categoryList.get(getAdapterPosition()));
                }
            });
            //expand the quick action
            more.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    callback.onMoreOptions(quickAction, more);
                }
            });
            //open category on clicking the main card background
            card.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    callback.onOpenCategory(getAdapterPosition());
                }
            });
        }


    }
    public interface ClickActionCallback{
        void onCameraRequest(Category category);
        void onGalleryRequest(Category category);
        void onOpenCategory(int pos);
        void onMoreOptions(View parent, View src);
    }
}
