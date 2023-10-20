package com.newway.nwphotospicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newway.nwfeedback.databinding.ItemFolderLayoutBinding
import com.newway.nwphotospicker.extension.loadImage
import com.newway.nwphotospicker.extension.singleClick
import com.newway.nwphotospicker.model.NWPhotoDirectory

interface NWFolderAdapterInterface {
    fun onClickFolder(folder:NWPhotoDirectory)
}

class NWFolderAdapter : RecyclerView.Adapter<NWFolderAdapter.FolderViewHolder>(){
    private var folders: List<NWPhotoDirectory> = listOf()
    var listener : NWFolderAdapterInterface? = null

    class FolderViewHolder(val binding: ItemFolderLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        if (position < folders.size){
            val folder = folders[position]
            loadImage(holder.binding.imvPhoto,folder.getCoverPath())

            holder.binding.folderTitle.text = folder.name?.ifEmpty { "All Photos" } ?: "All Photos"
            holder.binding.folderCount.text = folder.medias.size.toString()
            holder.binding.root.singleClick {
                listener?.onClickFolder(folder)
            }
        }
    }
    fun setContent(value:List<NWPhotoDirectory>){
        folders = value
        notifyDataSetChanged()
    }
}