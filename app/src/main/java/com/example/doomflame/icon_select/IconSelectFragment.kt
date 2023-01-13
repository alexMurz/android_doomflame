package com.example.doomflame.icon_select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doomflame.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor

class IconSelectFragment : Fragment() {

    companion object {
        fun newInstance() = IconSelectFragment()
    }

    private val clickStream = PublishProcessor.create<Int>()

    private lateinit var viewModel: IconSelectViewModel
    private lateinit var recycler: RecyclerView
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { IconSelectAdapter(clickStream) }


    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[IconSelectViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_icon_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.recycler)
        recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@IconSelectFragment.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        disposable.addAll(
            viewModel
                .observeItems()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { adapter.submitList(it) },
            clickStream.subscribe {
                viewModel.handleClick(requireContext(), it)
            }
        )
    }

    override fun onPause() {
        super.onPause()
        disposable.clear()
    }
}