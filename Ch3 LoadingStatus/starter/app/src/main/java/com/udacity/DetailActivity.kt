package com.udacity

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import com.udacity.MainActivity.Companion.KEY_DOWNLOAD_STATUS
import com.udacity.MainActivity.Companion.KEY_FILE_NAME
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.content_detail.*

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        setSupportActionBar(toolbar)

        val extra = intent.extras
        val name = extra?.getString(KEY_FILE_NAME)
        file_name_textview.text = name
        val status = extra?.getString(KEY_DOWNLOAD_STATUS)
        status_textview.text = status

        if (status == getString(R.string.download_status_failure)) {
            status_textview.setTextColor(Color.RED)
        }

        return_home_button.setOnClickListener {
            container.transitionToStart()
            container.setTransitionListener(transitionListener)
        }
    }

    private val transitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

        override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
            finish()
        }

        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
    }

}
