package com.tamhuynh.trackme.ui.adapter

import android.content.Context
import android.view.ViewGroup
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.WorkoutRecordData

class WorkoutListAdapter(context: Context,
                         mItemClickListener: OnItemClickListener,
                         mLongItemClickListener: OnLongItemClickListener) : BaseRecyclerViewAdapter<WorkoutRecordData, WorkoutViewHolder>(context) {


    init {
        this.mItemClickListener = mItemClickListener
        this.mLongItemClickListener = mLongItemClickListener
    }

    override fun getItemResourceLayout(viewType: Int): Int {
        return R.layout.item_workout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        var viewHolder = WorkoutViewHolder(getView(parent, viewType), mItemClickListener, mLongItemClickListener)
        return viewHolder
    }
}