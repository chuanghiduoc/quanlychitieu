package com.example.quanlychitieu.adapter.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Drawable deleteIcon;
    private final Drawable editIcon;
    private final ColorDrawable deleteBackground;
    private final ColorDrawable editBackground;
    private final int iconMargin;
    private final Paint clearPaint;
    private final SwipeActionListener listener;

    public interface SwipeActionListener {
        void onDelete(int position);
        void onEdit(int position);
    }

    public SwipeToDeleteCallback(Context context, SwipeActionListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;

        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);

        deleteBackground = new ColorDrawable(Color.parseColor("#F44336")); // Red color
        editBackground = new ColorDrawable(Color.parseColor("#2196F3")); // Blue color

        iconMargin = context.getResources().getDimensionPixelSize(R.dimen.swipe_icon_margin);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't want to support moving items
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.LEFT) {
            listener.onDelete(position);
        } else {
            listener.onEdit(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        boolean isCanceled = dX == 0 && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        // Draw background based on swipe direction
        if (dX < 0) { // Swiping left (delete)
            // Draw red delete background
            deleteBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            deleteBackground.draw(c);

            // Calculate position for the delete icon
            int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
            int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;

            // Set bounds and draw the delete icon
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.draw(c);

        } else if (dX > 0) { // Swiping right (edit)
            // Draw blue edit background
            editBackground.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + (int) dX, itemView.getBottom());
            editBackground.draw(c);

            // Calculate position for the edit icon
            int iconTop = itemView.getTop() + (itemHeight - editIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + editIcon.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = itemView.getLeft() + iconMargin + editIcon.getIntrinsicWidth();

            // Set bounds and draw the edit icon
            editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            editIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}
