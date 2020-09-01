package com.tamhuynh.trackme.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tamhuynh.trackme.WorkoutRecordData
import com.tamhuynh.trackme.databinding.FragmentListWorkoutBinding
import com.tamhuynh.trackme.ui.adapter.BaseRecyclerViewAdapter
import com.tamhuynh.trackme.ui.adapter.RecyclerViewLoadMoreScroll
import com.tamhuynh.trackme.ui.adapter.WorkoutListAdapter
import com.tamhuynh.trackme.viewmodels.LocationUpdateViewModel

class ListWorkoutFragment : Fragment() {

    private val TAG = "ListWorkoutFragment"
    private lateinit var binding: FragmentListWorkoutBinding
    lateinit var scrollListener: RecyclerViewLoadMoreScroll
    lateinit var mLayoutManager: RecyclerView.LayoutManager
    lateinit var workoutListAdapter: WorkoutListAdapter
    private var index: Int = 0
    private val LIMIT = 10
    private lateinit var activityListener : ItemClickCallbacks

    private val locationUpdateViewModel by lazy {
        ViewModelProvider(this).get(LocationUpdateViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ItemClickCallbacks) {
            activityListener = context
        } else {
            throw RuntimeException("$context must implement PermissionRequestFragment.Callbacks")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentListWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        mLayoutManager = LinearLayoutManager(activity)
        scrollListener = RecyclerViewLoadMoreScroll(mLayoutManager as LinearLayoutManager)
        scrollListener.setOnLoadMoreListener(object : BaseRecyclerViewAdapter.OnLoadMoreListener {
            override fun onLoadMore() {
                Log.d(TAG, "onLoadMore called index ${index}")
                bindData(LIMIT, index)
            }
        })

        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addOnScrollListener(scrollListener)
        activity?.applicationContext?.let {
            var mDividerItemDecoration = DividerItemDecoration(it,
                    (mLayoutManager as LinearLayoutManager).orientation)

            binding.recyclerView.addItemDecoration(mDividerItemDecoration)
            workoutListAdapter = WorkoutListAdapter(it, object : BaseRecyclerViewAdapter.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
//                    Toast.makeText(context, "onItemClick ${position}", Toast.LENGTH_LONG).show()
                    activityListener.itemWorkoutSelected(workoutListAdapter.mDatas[position])
                }

            }, object : BaseRecyclerViewAdapter.OnLongItemClickListener {
                override fun onLongItemClick(view: View, position: Int) {
//                    Toast.makeText(context, "onLongItemClick ${position}", Toast.LENGTH_LONG).show()
                }

            })
            binding.recyclerView.adapter = workoutListAdapter

        }
        bindData(LIMIT, index)
    }

    private fun bindData(limit: Int, index: Int) {
        locationUpdateViewModel.getListWorkoutRecord(limit, limit * index).observe(
                viewLifecycleOwner, Observer {
            if(it != null && it.isNotEmpty()) {
                this.index = index.plus(1)
                workoutListAdapter.addAllData(it)
            } else {
                scrollListener.setOnLoadMoreListener(object : BaseRecyclerViewAdapter.OnLoadMoreListener {
                    override fun onLoadMore() {
                        // do nothing
                    }
                })
            }

        }
        )
    }

    companion object {
        fun newInstance() = ListWorkoutFragment()
    }

    override fun onPause() {
        super.onPause()
        index = 0
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    interface ItemClickCallbacks {
        fun itemWorkoutSelected(workoutRecordData: WorkoutRecordData)
    }

}