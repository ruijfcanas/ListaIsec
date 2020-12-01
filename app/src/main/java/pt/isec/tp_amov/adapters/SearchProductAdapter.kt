package pt.isec.tp_amov.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.isec.tp_amov.R
import pt.isec.tp_amov.adapters.viewholder.StoredProductsHolder
import pt.isec.tp_amov.interfaces.ItemClickListener
import pt.isec.tp_amov.objects.DataProduct

class SearchProductAdapter(val data: ArrayList<DataProduct>,
                           val itemClick: ItemClickListener) : RecyclerView.Adapter<StoredProductsHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoredProductsHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_search_list, parent,false)

        return StoredProductsHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: StoredProductsHolder, position: Int) {
        holder.update(data[position].name, data[position].category.toString())
        holder.itemView.setOnClickListener{
            itemClick.onItemClickListener(data[position])
        }
    }
}