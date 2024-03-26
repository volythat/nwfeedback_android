package com.newway.nwfeedback.nwphotospicker

import android.R.attr.bitmap
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.newway.nwfeedback.databinding.FragmentNwPhotosPickerBinding
import com.newway.nwfeedback.nwphotospicker.adapter.NWFolderAdapter
import com.newway.nwfeedback.nwphotospicker.adapter.NWFolderAdapterInterface
import com.newway.nwfeedback.nwphotospicker.adapter.NWPhotosAdapter
import com.newway.nwfeedback.nwphotospicker.adapter.NWPhotosAdapterInterface
import com.newway.nwfeedback.nwphotospicker.extension.rotateImage
import com.newway.nwfeedback.nwphotospicker.extension.singleClick
import com.newway.nwfeedback.nwphotospicker.model.NWMedia
import com.newway.nwfeedback.nwphotospicker.model.NWPhotoDirectory
import com.newway.nwfeedback.nwphotospicker.viewmodel.NWPhotosPickerViewModel
import java.io.File
import java.util.concurrent.TimeUnit


interface NWPhotosPickerDialogInterface {
    fun onDismissWithImages(images: List<Uri>)
}

class NWPhotosPickerFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(isShowCamera: Boolean, maxSelect: Int) : NWPhotosPickerFragment {
            val dialog = NWPhotosPickerFragment()
            dialog.isShowCamera = isShowCamera
            dialog.maxSelect = maxSelect
            return dialog
        }
    }

    private lateinit var viewModel: NWPhotosPickerViewModel
    private lateinit var binding: FragmentNwPhotosPickerBinding
    var listener: NWPhotosPickerDialogInterface? = null

    private lateinit var photosAdapter: NWPhotosAdapter
    private lateinit var folderAdapter: NWFolderAdapter

    private var isShowFolderView : Boolean = false
    private var maxSelect : Int = 1
    private var isShowCamera : Boolean = true
    private var photoURI : Uri? = null

    private var launcher = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if (it){
            onPickPhotoDone()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[NWPhotosPickerViewModel::class.java]
        binding = FragmentNwPhotosPickerBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return super.onCreateDialog(savedInstanceState).apply {

            window?.setDimAmount(0.6f)

            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
                bottomSheet.setBackgroundResource(android.R.color.transparent)

                val displayMetrics = resources.displayMetrics
                val height = displayMetrics.heightPixels

                val params: ViewGroup.LayoutParams = binding.rootLayout.layoutParams
                params.height = height
                binding.rootLayout.requestLayout()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        bind()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onDismissWithImages(listOf())
    }


    // LAYOUT
    private fun setUpView(){
        binding.btnClose.singleClick {
            this.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                listener?.onDismissWithImages(listOf())
            }, 100)
        }
        binding.btnDone.isVisible = maxSelect != 1
        binding.btnDone.singleClick {
            this.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                listener?.onDismissWithImages(viewModel.getSelectedMedias())
            }, 100)
        }
        binding.btnChangeListPhoto.singleClick {
            binding.arrow.rotateImage(180f)
            isShowFolderView = !isShowFolderView
            binding.contentFolderView.isVisible = isShowFolderView
        }
        setUpPhotoView()
        setUpFolderView()
    }

    private fun bind(){
        viewModel.getMedia()
        viewModel.getPhotoDirs()

        viewModel.isDataChanged.observe(viewLifecycleOwner){
            Log.e("NWPhotosPicker","bind: isDataChanged")
            viewModel.getMedia()
        }
        viewModel.folders.observe(viewLifecycleOwner){
            if (::folderAdapter.isInitialized){
                folderAdapter.setContent(it)
            }
        }
        viewModel.medias.observe(viewLifecycleOwner){
            if (::photosAdapter.isInitialized){
                photosAdapter.setContent(it)
            }
        }
    }

    private fun setUpPhotoView() {
        binding.listImage.layoutManager = GridLayoutManager(context,3)
        photosAdapter = NWPhotosAdapter()
        photosAdapter.showCamera = isShowCamera
        binding.listImage.adapter = photosAdapter
        photosAdapter.listener = object : NWPhotosAdapterInterface {
            override fun onClickPhoto(media: NWMedia) {
                if (maxSelect == 1){
                    //dismiss
                    this@NWPhotosPickerFragment.dismiss()
                    Handler(Looper.getMainLooper()).postDelayed({
                        listener?.onDismissWithImages(listOf(media.path))
                    }, 100)
                }else{
                    //dismiss when max selected
                    val selected = viewModel.getSelectedMedias()
                    if (maxSelect <= selected.size){
                        this@NWPhotosPickerFragment.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            listener?.onDismissWithImages(selected)
                        }, 100)
                    }
                }
            }

            override fun onClickCamera() {
                try {
                    openCamera()
                }catch(e:Exception){
                    Log.e("NWPhotosPicker","onClickCamera: error = ${e.localizedMessage}")
                }
            }
        }
    }
    private fun setUpFolderView(){
        binding.listFolder.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        folderAdapter = NWFolderAdapter()
        folderAdapter.listener = object : NWFolderAdapterInterface {
            override fun onClickFolder(folder: NWPhotoDirectory) {
                Log.e( "NWPhotosPicker","onClickFolder: ")
                binding.arrow.rotateImage(180f)
                isShowFolderView = false
                viewModel.getMedia(folder.bucketId)
                binding.contentFolderView.isVisible = isShowFolderView

            }
        }
        binding.listFolder.adapter = folderAdapter
    }
    //FUN

    fun openCamera() {
        context?.let { ctx ->
            createImageFileInAppDir()?.let { file ->
                photoURI = FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.provider",
                    file
                )
                launcher.launch(photoURI)
            }
        }
    }
    private fun createImageFileInAppDir(): File? {
        return if (context != null) {
            val imagePath = context?.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            File(
                imagePath,
                "JPEG_${TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())}" + ".jpg"
            )
        } else {
            null
        }
    }
    private fun onPickPhotoDone(){
        if (maxSelect == 1) {
            photoURI?.let {
                this@NWPhotosPickerFragment.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    listener?.onDismissWithImages(listOf(it))
                }, 100)
            }
        }
    }

}


