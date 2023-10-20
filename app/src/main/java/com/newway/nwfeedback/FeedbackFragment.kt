package com.newway.nwfeedback

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.newway.nwfeedback.databinding.FragmentFeedbackBinding
import com.newway.nwphotospicker.NWPhotosPickerDialogInterface
import com.newway.nwphotospicker.NWPhotosPickerFragment
import zendesk.core.Zendesk
import zendesk.support.Support
import java.io.File

interface FeedbackFragmentInterface {
    fun sentFeedbackSuccessfully()
}
class FeedbackFragment : BottomSheetDialogFragment()  {
    private var projectId : String = ""

    companion object {
        const val TAG = "FeedbackFragment"

        fun setUp(context:Context,zendUrl:String,zendAppId:String,zendClientId:String){
            Zendesk.INSTANCE.init(context, zendUrl,
                zendAppId,
                zendClientId
            )

            Support.INSTANCE.init(Zendesk.INSTANCE)
        }
        fun instance(projectId:String) : FeedbackFragment {
            val dialog = FeedbackFragment()
            dialog.projectId = projectId
            return dialog
        }

    }

    private lateinit var binding: FragmentFeedbackBinding
    private lateinit var viewModel: FeedbackViewModel
    private lateinit var adapter : FeedbackAdapter

    var listener: FeedbackFragmentInterface? = null
    private var model = SMFeedbackModel()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val storagePermissions33 =
        arrayOf<String>(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

    private val storagePermissions = arrayOf<String>(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel = ViewModelProvider(this)[FeedbackViewModel::class.java]
        binding = FragmentFeedbackBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return super.onCreateDialog(savedInstanceState).apply {

            window?.setDimAmount(0.6f)

            setOnShowListener {
//
                val displayMetrics = resources.displayMetrics
                val height = displayMetrics.heightPixels

                val params: ViewGroup.LayoutParams = binding.rootView.layoutParams
                params.height = (height.toFloat() * 0.8f).toInt()
                binding.rootView.requestLayout()
                (dialog as BottomSheetDialog).behavior.state = STATE_EXPANDED
                (dialog as BottomSheetDialog).behavior.peekHeight = params.height
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        bind()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setUpView(){
        getFolder()?.let {
            viewModel.clearCache(it)
        }

        adapter = FeedbackAdapter()
        adapter.listener = object : FeedbackAdapterInterface {
            override fun onClickPickerPhoto() {
                showPickerImage()
            }

            override fun onClickRemovePhoto(position: Int) {

            }

        }
        binding.listPhotos.layoutManager = LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)
        binding.listPhotos.adapter = adapter

        binding.btnClose.singleClick {
            dismissAllowingStateLoss()
        }
        binding.btnSend.singleClick {
            activity?.hideKeyboard()
            tapToSendButton()
        }
        binding.lbEmail.addTextChangedListener {
            viewModel.validate(binding.lbEmail.text.toString(),binding.lbContent.text.toString())
        }
        binding.lbContent.addTextChangedListener {
            viewModel.validate(binding.lbEmail.text.toString(),binding.lbContent.text.toString())
        }
    }
    private fun bind(){
        viewModel.enableSendButton.observe(viewLifecycleOwner){
            binding.btnSend.isVisible = it
        }
    }

    private fun tapToSendButton(){
        if (allowUse()) {
            model.projectId = projectId
            model.addImages(adapter.items)
            model.email = binding.lbEmail.text.toString()
            model.content = binding.lbContent.text.toString()

            if (model.isValid(requireActivity())) {
                showLoading()
                viewModel.createIdentify(model.email)
                model.uploadMultiAttachments {
                    if (it) {
                        postToZendesk()
                    } else {
                        hideLoading()
                        alertUploadRetry()
                    }
                }
            }
        }
    }

    private fun postToZendesk(){
        if (allowUse()) {
            val size = requireActivity().screenSize()
            val resolution = "${size.widthPixels}/${size.heightPixels}"
            viewModel.sendFeedback(model, requireContext(), resolution) { response ->
                hideLoading()
                if (response) {
                    dismissAllowingStateLoss()
                    listener?.sentFeedbackSuccessfully()
                } else {
                    alertPostRetry()
                }
            }
        }
    }

    //permission
    fun showDialog(activity: FragmentActivity){
        isCancelable = false
        show(activity.supportFragmentManager, TAG)
    }

    fun permissions(): Array<String> {
        val p: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermissions33
        } else {
            storagePermissions
        }
        return p
    }

    //alert

    private fun alertUploadRetry(){
        if (allowUse()) {
            context?.let {
                AlertDialog.Builder(it)
                    .setTitle("Send failed!")
                    .setCancelable(false)
                    .setPositiveButton("Retry") { dialog, _ ->
                        dialog.dismiss()
                        tapToSendButton()
                    }
                    .setPositiveButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun alertPostRetry(){
        if (allowUse()) {
            context?.let {
                AlertDialog.Builder(it)
                    .setTitle("Send failed!")
                    .setCancelable(false)
                    .setPositiveButton("Retry") { dialog, _ ->
                        dialog.dismiss()
                        postToZendesk()
                    }
                    .setPositiveButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    //MARK: - Fun
    private fun allowUse():Boolean {
        if (activity == null) return false
        if (!isAdded) return false
        if (activity?.isDestroyed == true) return false
        if (activity?.isFinishing == true) return false

        return true
    }

    private fun showLoading(){
        binding.viewLoading.isVisible = true
    }
    private fun hideLoading(){
        binding.viewLoading.isVisible = false
    }

    //MARK: - picker

    private fun showPickerImage(){
        if (allowUse()) {
            val dialog = NWPhotosPickerFragment.newInstance(isShowCamera = true, maxSelect = 1)
            dialog.listener = object : NWPhotosPickerDialogInterface {
                override fun onDismissWithImages(images: List<Uri>) {
                    images.firstOrNull()?.let {
                        getPhoto(it)
                    }
                }
            }
            dialog.show(parentFragmentManager, "NWPhotosPickerFragment")
        }
    }
    private fun getFolder():File? {
        val dir = context?.getExternalFilesDir(null)?.let {
            File(it,".cache_feedback").apply { mkdirs() }
        }
        return if (dir != null && dir.exists()) dir else context?.filesDir
    }
    private fun getPhoto(uri:Uri){
        Glide.with(this).asBitmap().load(uri).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                getFolder()?.let {
                    val file = viewModel.savePhotoFeedback(resource,it)
                    if (file.exists()){
                        adapter.addImage(file.toUri())
                    }
                }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                Toast.makeText(this@FeedbackFragment.context,"Unknown error",Toast.LENGTH_LONG).show()
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
    }

    //
    fun Activity.hideKeyboard(){
        val inputMethodManager = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        inputMethodManager?.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
    }

}