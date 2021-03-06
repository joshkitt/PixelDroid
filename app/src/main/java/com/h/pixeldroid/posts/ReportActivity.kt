package com.h.pixeldroid.posts

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityReportBinding
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

class ReportActivity : BaseActivity() {

    private lateinit var binding: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.report)

        val status = intent.getSerializableExtra(Status.POST_TAG) as Status?

        //get the currently active user
        val user = db.userDao().getActiveUser()


        binding.reportTargetTextview.text = getString(R.string.report_target).format(status?.account?.acct)


        binding.reportButton.setOnClickListener{
            binding.reportButton.visibility = View.INVISIBLE
            binding.reportProgressBar.visibility = View.VISIBLE

            binding.textInputLayout.editText?.isEnabled = false

            val accessToken = user?.accessToken.orEmpty()
            val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

            lifecycleScope.launchWhenCreated {
                try {
                    api.report("Bearer $accessToken", status?.account?.id!!, listOf(status), binding.textInputLayout.editText?.text.toString())

                    reportStatus(true)
                } catch (exception: IOException) {
                    reportStatus(false)
                } catch (exception: HttpException) {
                    reportStatus(false)
                }
            }
        }
    }

    private fun reportStatus(success: Boolean){
        if(success){
            binding.reportProgressBar.visibility = View.GONE
            binding.reportButton.isEnabled = false
            binding.reportButton.text = getString(R.string.reported)
            binding.reportButton.visibility = View.VISIBLE
        } else {
            binding.textInputLayout.error = getString(R.string.report_error)
            binding.reportButton.visibility = View.VISIBLE
            binding.textInputLayout.editText?.isEnabled = true
            binding.reportProgressBar.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}