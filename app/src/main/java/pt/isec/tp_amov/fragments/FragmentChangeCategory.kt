package pt.isec.tp_amov.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import pt.isec.tp_amov.R
import pt.isec.tp_amov.adapters.ArrayRecyclerAdapter
import pt.isec.tp_amov.interfaces.ItemClickListenerInterface
import pt.isec.tp_amov.model.Model
import pt.isec.tp_amov.model.ModelView


class FragmentChangeCategory : Fragment(), ItemClickListenerInterface<String>{
    private val dataList = ArrayList<String>(Model.config.categories)
    lateinit var rvList: RecyclerView
    lateinit var adapter: ArrayRecyclerAdapter
    lateinit var lM: RecyclerView.LayoutManager
    lateinit var act : Context

    private lateinit var dialogRemove: AlertDialog
    private var removeStr = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        act = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onDestroyView() {
        try {
            if (dialogRemove.isShowing)
                dialogRemove.dismiss()
        } catch (e: UninitializedPropertyAccessException) {}
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!removeStr.isNullOrEmpty())
            ModelView.removeString = removeStr
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_category, container, false)
        rvList = view.findViewById(R.id.rvManageCategory)
        adapter = ArrayRecyclerAdapter(dataList, this)
        lM = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        rvList.adapter = adapter
        rvList.layoutManager = lM

        if (savedInstanceState != null) {
            if (ModelView.unitRemoveShowing)
                removeListDlg(ModelView.removeString)
        }

        return view
    }

    override fun onItemClickListener(data: String) {
        removeListDlg(data)
    }

    private fun removeListDlg(data: String){
        ModelView.categoryRemoveShowing = false

        val builder = AlertDialog.Builder(act)     //Construct the builder
        val inflater = this.layoutInflater
        val viewLayout : View = inflater.inflate(R.layout.dialog_remove_item, null)  //The layout we want to inflate
        val sb = StringBuilder()
        ModelView.removeString = data
        viewLayout.findViewById<TextView>(R.id.tvRemoveItemDlg).text = sb
                .append(act.getString(R.string.remove_item_description_dlg))
                .append(" ")
                .append(data)
        builder.setView(viewLayout)
        builder.setCancelable(true)
        builder.setOnCancelListener {
            ModelView.categoryRemoveShowing = false
            removeStr = ""
        }

        builder.setPositiveButton(getString(R.string.delete_dlg)) {dialog, id ->
            ModelView.categoryRemoveShowing = false
            removeStr = ""
            Model.config.categories.remove(data)
            adapter.data = ArrayList(Model.config.categories)
            adapter.notifyDataSetChanged()
        }
        builder.setNegativeButton(getString(R.string.cancel_list)) { dialog, id ->
            dialog.dismiss()
            ModelView.categoryRemoveShowing = false
            removeStr = ""
        }
        dialogRemove = builder.show()
    }
}