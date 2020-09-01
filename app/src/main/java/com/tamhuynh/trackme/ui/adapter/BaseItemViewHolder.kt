package com.tamhuynh.trackme.ui.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseItemViewHolder<in Data>(
        itemView: View,
        protected val mItemClickListener: BaseRecyclerViewAdapter.OnItemClickListener?,
        protected val mLongItemClickListener: BaseRecyclerViewAdapter.OnLongItemClickListener?
) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

    protected var mContext: Context = itemView.context


    abstract fun bind(data: Data, position: Int)

    override fun onClick(p0: View?) {
        mItemClickListener?.onItemClick(p0!!, adapterPosition)
    }

    override fun onLongClick(p0: View?): Boolean {
        mLongItemClickListener?.onLongItemClick(p0!!, adapterPosition)
        return true
    }

}