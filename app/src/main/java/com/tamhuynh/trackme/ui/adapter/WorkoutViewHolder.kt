package com.tamhuynh.trackme.ui.adapter

import android.view.View
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.WorkoutRecordData
import kotlinx.android.synthetic.main.item_workout.view.*

class WorkoutViewHolder(item: View,
                        mItemClickListener: BaseRecyclerViewAdapter.OnItemClickListener?,
                        mOnLongItemClickListener: BaseRecyclerViewAdapter.OnLongItemClickListener?) :
        BaseItemViewHolder<WorkoutRecordData>(item, mItemClickListener, mOnLongItemClickListener) {

    override fun bind(data: WorkoutRecordData, position: Int) {
        itemView.tvWorkoutID.text = mContext.getString(R.string.workout_index_text) + " " +  data.listWorkoutRecordEntity.workoutID.toString()
        itemView.tvDate.text = data.listWorkoutRecordEntity.date.toString()
        itemView.setOnClickListener {
            mItemClickListener?.onItemClick(itemView, position)
        }
    }
}