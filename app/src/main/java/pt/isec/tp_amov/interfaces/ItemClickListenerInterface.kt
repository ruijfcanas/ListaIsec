package pt.isec.tp_amov.interfaces

import pt.isec.tp_amov.objects.DataProduct


interface ItemClickListenerInterface<T>{
    fun onItemClickListener(data: T)
}