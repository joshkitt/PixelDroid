package com.h.pixeldroid.postCreation

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.MainActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.postCreation.camera.CameraActivity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Attachment
import com.h.pixeldroid.utils.api.objects.Instance
import com.h.pixeldroid.postCreation.photoEdit.PhotoEditActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_post_creation.*
import kotlinx.android.synthetic.main.image_album_creation.view.*
import okhttp3.MultipartBody
import org.imaginativeworld.whynotimagecarousel.CarouselItem
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "Post Creation Activity"
private const val MORE_PICTURES_REQUEST_CODE = 0xffff

data class PhotoData(
        var imageUri: Uri,
        var uploadId: String? = null,
        var progress: Int? = null
)

class PostCreationActivity : BaseActivity() {

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI

    private var positionResult = 0
    private var user: UserDatabaseEntity? = null

    private val photoData: ArrayList<PhotoData> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        // get image URIs
        if(intent.clipData != null) {
            val count = intent.clipData!!.itemCount
            for (i in 0 until count) {
                intent.clipData!!.getItemAt(i).uri.let {
                    photoData.add(PhotoData(it))
                }
            }
        }

        user = db.userDao().getActiveUser()

        val instances = db.instanceDao().getAll()

        val textField = findViewById<TextInputLayout>(R.id.postTextInputLayout)

        textField.counterMaxLength = if (user != null){
            val thisInstances =
                instances.filter { instanceDatabaseEntity ->
                    instanceDatabaseEntity.uri.contains(user!!.instance_uri)
                }
            thisInstances.first().max_toot_chars
        } else {
            Instance.DEFAULT_MAX_TOOT_CHARS
        }

        accessToken = user?.accessToken.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        val carousel: ImageCarousel = findViewById(R.id.carousel)
        carousel.addData(photoData.map { CarouselItem(it.imageUri.toString()) })

        // get the description and send the post
        findViewById<Button>(R.id.post_creation_send_button).setOnClickListener {
            if (validateDescription() && photoData.isNotEmpty()) upload()
        }

        // Button to retry image upload when it fails
        findViewById<Button>(R.id.retry_upload_button).setOnClickListener {
            upload_error.visibility = View.GONE
            photoData.forEach {
                it.uploadId = null
                it.progress = null
            }
            upload()
        }

        findViewById<ImageButton>(R.id.editPhotoButton).setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                edit(currentPosition)
            }
        }

        findViewById<ImageButton>(R.id.addPhotoButton).setOnClickListener {
            val intent = Intent(it.context, CameraActivity::class.java)
            this@PostCreationActivity.startActivityForResult(intent, MORE_PICTURES_REQUEST_CODE)
        }

        findViewById<ImageButton>(R.id.savePhotoButton).setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                savePicture(it, currentPosition)
            }
        }


        findViewById<ImageButton>(R.id.removePhotoButton).setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                photoData.removeAt(currentPosition)
                carousel.addData(photoData.map { CarouselItem(it.imageUri.toString()) })
            }
            //TODO("have to fix upload to allow deleting!")
        }
    }

    private fun savePicture(button: View, currentPosition: Int) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".png"
        val pair = getOutputFile(name)
        val outputStream: OutputStream = pair.first
        val path: String = pair.second

        contentResolver.openInputStream(photoData[currentPosition].imageUri)!!.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        if(path.startsWith("file")) {
            MediaScannerConnection.scanFile(
                this,
                arrayOf(path.toUri().toFile().absolutePath),
                null
            ) { path, uri ->
                if (uri == null) {
                    Log.e(
                        "NEW IMAGE SCAN FAILED",
                        "Tried to scan $path, but it failed"
                    )
                }
            }
        }
        Snackbar.make(
            button, getString(R.string.save_image_success),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun getOutputFile(name: String): Pair<OutputStream, String> {
        val outputStream: OutputStream
        val path: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )
            val imageUri: Uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
            path = imageUri.toString()
            outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri))!!
        } else {
            @Suppress("DEPRECATION") val imagesDir =
                Environment.getExternalStoragePublicDirectory(getString(R.string.app_name))
            imagesDir.mkdir()
            val file = File(imagesDir, name)
            path = Uri.fromFile(file).toString()
            outputStream = file.outputStream()
        }
        return Pair(outputStream, path)
    }


    private fun validateDescription(): Boolean {
        val textField = findViewById<TextInputLayout>(R.id.postTextInputLayout)
        val content = textField.editText?.length() ?: 0
        if (content > textField.counterMaxLength) {
            // error, too many characters
            textField.error = getString(R.string.description_max_characters).format(textField.counterMaxLength)
            return false
        }
        return true
    }

    /**
     * Uploads the images that are in the [posts] array.
     * Keeps track of them in the [progressList] (for the upload progress), and the [muListOfIds]
     * (for the list of ids of the uploads).
     * @param newImagesStartingIndex is the index in the [posts] array we want to start uploading at.
     * Indices before this are already uploading, or done uploading, from before.
     * @param editedImage contains the index of the image that was edited. If set, other images are
     * not uploaded again: they should already be uploading, or be done uploading, from before.
     */
    private fun upload() {
        enableButton(false)
        uploadProgressBar.visibility = View.VISIBLE
        upload_completed_textview.visibility = View.INVISIBLE
        removePhotoButton.isEnabled = false
        editPhotoButton.isEnabled = false
        addPhotoButton.isEnabled = false

        for (data in photoData) {
            val imageUri = data.imageUri
            val imageInputStream = contentResolver.openInputStream(imageUri)!!

            val size =
                if (imageUri.toString().startsWith("content")) {
                    contentResolver.query(imageUri, null, null, null, null)
                        ?.use { cursor ->
                        /* Get the column indexes of the data in the Cursor,
                         * move to the first row in the Cursor, get the data,
                         * and display it.
                         */
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            cursor.moveToFirst()
                            cursor.getLong(sizeIndex)
                        } ?: 0
                } else {
                    imageUri.toFile().length()
                }

            val imagePart = ProgressRequestBody(imageInputStream, size)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis().toString(), imagePart)
                .build()

            val sub = imagePart.progressSubject
                .subscribeOn(Schedulers.io())
                .subscribe { percentage ->
                    data.progress = percentage.toInt()
                    uploadProgressBar.progress =
                        photoData.sumBy { it.progress ?: 0 } / photoData.size
                }

            var postSub: Disposable? = null
            val inter = pixelfedAPI.mediaUpload("Bearer $accessToken", requestBody.parts[0])

            postSub = inter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { attachment: Attachment ->
                        data.progress = 0
                        data.uploadId = attachment.id!!
                    },
                    { e ->
                        upload_error.visibility = View.VISIBLE
                        e.printStackTrace()
                        postSub?.dispose()
                        sub.dispose()
                    },
                    {
                        data.progress = 100
                        if(photoData.all{it.progress == 100}){
                            uploadProgressBar.visibility = View.GONE
                            upload_completed_textview.visibility = View.VISIBLE
                            post()
                        }
                        postSub?.dispose()
                        sub.dispose()
                    }
                )
        }
    }

    private fun post() {
        val description = new_post_description_input_field.text.toString()
        enableButton(false)
        lifecycleScope.launchWhenCreated {
            try {
                pixelfedAPI.postStatus(
                    authorization = "Bearer $accessToken",
                    statusText = description,
                    media_ids = photoData.mapNotNull { it.uploadId }.toList()
                )
                Toast.makeText(applicationContext,getString(R.string.upload_post_success),
                    Toast.LENGTH_SHORT).show()
                val intent = Intent(this@PostCreationActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (exception: IOException) {
                Toast.makeText(applicationContext,getString(R.string.upload_post_error),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.toString())
                enableButton(true)
            } catch (exception: HttpException) {
                Toast.makeText(applicationContext,getString(R.string.upload_post_failed),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.response().toString() + exception.message().toString())
                enableButton(true)
            }
        }
    }

    private fun enableButton(enable: Boolean = true){
        post_creation_send_button.isEnabled = enable
        if(enable){
            posting_progress_bar.visibility = View.GONE
            post_creation_send_button.visibility = View.VISIBLE
        } else {
            posting_progress_bar.visibility = View.VISIBLE
            post_creation_send_button.visibility = View.GONE
        }

    }

    private fun edit(position: Int) {
        positionResult = position

        val intent = Intent(this, PhotoEditActivity::class.java)
            .putExtra("picture_uri", photoData[position].imageUri)
            .putExtra("no upload", false)
        startActivityForResult(intent, positionResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == positionResult) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                photoData[positionResult].imageUri = data.getStringExtra("result")!!.toUri()

                carousel.addData(photoData.map { CarouselItem(it.imageUri.toString()) })

                photoData[positionResult].progress = null
                photoData[positionResult].uploadId = null
            } else if(resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == MORE_PICTURES_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data?.clipData != null) {

                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri: Uri = data.clipData!!.getItemAt(i).uri
                    photoData.add(PhotoData(imageUri))
                }

                carousel.addData(photoData.map { CarouselItem(it.imageUri.toString()) })
            } else if(resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Error while adding images", Toast.LENGTH_SHORT).show()
            }
        }
    }
/*
    inner class PostCreationAdapter(private val posts: ArrayList<String>): RecyclerView.Adapter<PostCreationAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                if(viewType == 0) LayoutInflater.from(parent.context)
                .inflate(R.layout.image_album_creation, parent, false)
                else LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_more_album_creation, parent, false)
            return ViewHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            if(position == posts.size) return 1
            return 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if(position != posts.size) {
                holder.bindImage()
            } else{
                holder.bindPlusButton()
            }
        }

        override fun getItemCount(): Int = posts.size + 1

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindImage() {
                val image = Uri.parse(
                    posts[adapterPosition]
                )
                // load image
                Glide.with(itemView.context)
                    .load(image)
                    .centerCrop()
                    .into(itemView.galleryImage)
                // adding click or tap handler for the image layout
                itemView.setOnClickListener {
                    this@PostCreationActivity.onClick(adapterPosition)
                }

            }

            fun bindPlusButton() {
                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, CameraActivity::class.java)
                    this@PostCreationActivity.startActivityForResult(intent, MORE_PICTURES_REQUEST_CODE)
                }
            }
        }
    }
 */
}