package com.h.pixeldroid.postCreation.carousel

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h.pixeldroid.R


class CarouselAdapter(
    @LayoutRes private val itemLayout: Int,
    @IdRes private val imageViewId: Int,
    var listener: OnItemClickListener? = null,
    private val imageScaleType: ImageView.ScaleType,
    private val imagePlaceholder: Drawable?,
    private val carousel: Boolean
) : RecyclerView.Adapter<CarouselAdapter.MyViewHolder>() {

    private val dataList: MutableList<CarouselItem> = mutableListOf()



    class MyViewHolder(itemView: View, imageViewId: Int) : RecyclerView.ViewHolder(itemView) {
        var img: ImageView = itemView.findViewById(imageViewId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return if(!carousel){
            if(viewType == 0) {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.image_album_creation, parent, false)
                MyViewHolder(view, R.id.galleryImage)
            } else {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.add_more_album_creation, parent, false)
                MyViewHolder(view, R.id.addPhotoSquare)
            }
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(itemLayout, parent, false)
            MyViewHolder(view, imageViewId)
        }
    }

    override fun getItemCount(): Int {
        return if(carousel) dataList.size
        else dataList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if(position == dataList.size) return 1
        return 0
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if(carousel) {
            holder.img.scaleType = imageScaleType
        }

        dataList.elementAtOrNull(position)?.let {
            Glide.with(holder.itemView.context)
                .load(it.imageUrl)
                .placeholder(imagePlaceholder)
                .into(holder.img)
        }

        // Init listeners
        listener?.apply {

            holder.itemView.setOnClickListener {
                this.onClick(position)
            }

            holder.itemView.setOnLongClickListener {
                this.onLongClick(position)

                true
            }

        }
    }

    fun getItem(position: Int): CarouselItem? {
        return if (position < dataList.size) {
            dataList[position]
        } else {
            null
        }
    }

    fun addAll(dataList: List<CarouselItem>) {
        this.dataList.clear()

        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }

    fun add(item: CarouselItem) {
        this.dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

}