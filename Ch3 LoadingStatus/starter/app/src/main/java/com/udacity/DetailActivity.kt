package com.udacity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
    }

}
