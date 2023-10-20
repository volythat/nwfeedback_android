package com.newway.nwfeedback
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newway.nwfeedback.databinding.FeedbackPhotoItemBinding
import com.newway.nwfeedback.databinding.PickPhotoFeedbackItemBinding


interface FeedbackAdapterInterface {
    fun onClickPickerPhoto()
    fun onClickRemovePhoto(position: Int)
}

class FeedbackAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items : ArrayList<Uri> = arrayListOf()
    var listener: FeedbackAdapterInterface? = null

    class PickPhotoViewHolder(val binding: PickPhotoFeedbackItemBinding) : RecyclerView.ViewHolder(binding.root)
    class PhotoViewHolder(val binding: FeedbackPhotoItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return position
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType > 0){
            val binding = FeedbackPhotoItemBinding.inflate(layoutInflater,parent,false)
            PhotoViewHolder(binding)
        }else{
            val binding = PickPhotoFeedbackItemBinding.inflate(layoutInflater,parent,false)
            PickPhotoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder){
            is PhotoViewHolder ->{
                if (position > 0) {
                    holder.binding.imvPhoto.setImageURI(items[position - 1])
                }
                holder.binding.btnRemove.singleClick {
                    removeItem(position)
                    listener?.onClickRemovePhoto(position)
                }
            }
            is PickPhotoViewHolder -> {
                holder.binding.cardView.singleClick {
                    listener?.onClickPickerPhoto()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + 1
    }

    fun addItems(value:ArrayList<Uri>){
        items = value
        notifyItemRangeInserted(0,items.size)
    }
    fun addImage(uri: Uri){
        items.add(uri)
        notifyItemInserted(items.size)
    }
    private fun removeItem(position: Int){
        items.removeAt(position - 1)
        notifyItemRemoved(position)
    }
}