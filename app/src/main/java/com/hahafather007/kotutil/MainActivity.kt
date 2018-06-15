package com.hahafather007.kotutil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity(), RxController {
    override val rxComposite = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initData()
    }

    override fun onDestroy() {
        super.onDestroy()

        onRxRelease()
    }

    private fun initData(){

    }
}
