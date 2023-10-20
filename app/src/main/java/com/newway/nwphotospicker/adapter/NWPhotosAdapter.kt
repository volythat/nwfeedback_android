package com.newway.nwphotospicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.newway.nwfeedback.R
import com.newway.nwfeedback.databinding.ItemPhotoLayoutBinding
import com.newway.nwphotospicker.extension.loadImage
import com.newway.nwphotospicker.extension.singleClick
import com.newway.nwphotospicker.model.NWMedia

interface NWPhotosAdapterInterface {
    fun onClickCamera()
    fun onClickPhoto(media:NWMedia)
}
class NWPhotosAdapter : RecyclerView.Adapter<NWPhotosAdapter.PhotoViewHolder>(){
    var medias: List<NWMedia> = listOf()
    var listener : NWPhotosAdapterInterface? = null
    var showCamera : Boolean = false

    class PhotoViewHolder(val binding: ItemPhotoLayoutBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (showCamera) {
            medias.size + 1
        }else{
            medias.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showCamera){
            if (position == 0) 1 else 2
        }else{
            2
        }
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val type = getItemViewType(position)
        if (type == 1){
            holder.binding.imvPhoto.setImageResource(R.drawable.image_camera)
            holder.binding.checkbox.isVisible = false
            holder.binding.root.singleClick {
                listener?.onClickCamera()
            }
        }else{
            val index = position - (if (showCamera) 1 else 0)
            if (index < medias.size){
                val media = medias[index]
                loadImage(holder.binding.imvPhoto,media.path)

                holder.binding.checkbox.isChecked = media.isSelected
                holder.binding.checkbox.isVisible = media.isSelected
                holder.binding.transparentBg.isVisible = media.isSelected
                holder.binding.checkbox.isEnabled = false
                holder.binding.root.singleClick {
                    media.isSelected = !media.isSelected
                    holder.binding.checkbox.isChecked = media.isSelected
                    holder.binding.checkbox.isVisible = media.isSelected
                    holder.binding.transparentBg.isVisible = media.isSelected
                    listener?.onClickPhoto(media)
                }
            }
        }
    }
    fun setContent(value:List<NWMedia>){
        medias = value
        notifyDataSetChanged()
    }
}