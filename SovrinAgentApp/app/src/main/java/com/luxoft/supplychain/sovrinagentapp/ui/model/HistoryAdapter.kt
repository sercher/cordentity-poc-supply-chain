/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.luxoft.supplychain.sovrinagentapp.ui.model

import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.luxoft.supplychain.sovrinagentapp.R
import com.luxoft.supplychain.sovrinagentapp.data.PackageState
import com.luxoft.supplychain.sovrinagentapp.data.Product
import com.luxoft.supplychain.sovrinagentapp.data.ProductOperation
import com.luxoft.supplychain.sovrinagentapp.ui.SimpleScannerActivity
import com.luxoft.supplychain.sovrinagentapp.ui.TrackPackageActivity
import com.luxoft.supplychain.sovrinagentapp.utils.DateTimeUtils
import com.luxoft.supplychain.sovrinagentapp.utils.gone
import com.luxoft.supplychain.sovrinagentapp.utils.inflate
import com.luxoft.supplychain.sovrinagentapp.utils.visible
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.item_history.view.textViewHistoryItemMessage
import kotlinx.android.synthetic.main.item_history_new.view.*
import kotlinx.android.synthetic.main.item_order.view.linearLayoutScanQr

class HistoryAdapter(realm: Realm) : RecyclerView.Adapter<HistoryAdapter.OrderViewHolder>() {

    private val orders: RealmResults<Product> = realm.where(Product::class.java).sort("collectedAt", Sort.DESCENDING).isNotNull("collectedAt").findAll()
    private val productOperations: RealmResults<ProductOperation> = realm.where(ProductOperation::class.java).findAll()

    var realmChangeListener = RealmChangeListener<Realm> {
        Log.i("TAG", "Change occurred!")
        this.notifyDataSetChanged()
    }

    init {
        realm.addChangeListener(realmChangeListener)
    }

    //region ******************** OVERRIDE *********************************************************

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder(viewGroup.context.inflate(R.layout.item_history_new, viewGroup))
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        if (order == null) {
            Log.i("TAG", "Item not found for index $position")
        } else {
            bindNormalItem(order, holder)
        }
    }

    override fun getItemCount(): Int {
        return orders.size
    }

    //endregion OVERRIDE

    private fun bindNormalItem(order: Product, holder: OrderViewHolder) {
        holder.title.text = order.medicineName
//        holder.sn.text = "SN: " + order.serial
        holder.message.text = order.currentStateMessage(PackageState.valueOf(order.state!!).ordinal)
        holder.title.setOnClickListener {
            startActivity(holder.title.context, Intent().setClass(holder.title.context, TrackPackageActivity::class.java).putExtra("serial", order.serial), null)
        }
        holder.qrButton.setOnClickListener {
            ContextCompat.startActivity(holder.qrButton.context,
                Intent().setClass(holder.qrButton.context, SimpleScannerActivity::class.java)
                    .putExtra("collected_at", order.collectedAt)
                    .putExtra("serial", order.serial)
                    .putExtra("state", order.state), null
            )
        }
        holder.linearLayoutExpand.setOnClickListener {
            if (holder.linearLayoutHistoryContent.visibility == View.GONE) {
                holder.linearLayoutHistoryContent.visible()
                holder.imageViewExpand.setImageDrawable(holder.imageViewExpand.context.getDrawable(R.drawable.up))
            } else {
                holder.linearLayoutHistoryContent.gone()
                holder.imageViewExpand.setImageDrawable(holder.imageViewExpand.context.getDrawable(R.drawable.down))
            }
        }

        holder.linearLayoutHistoryContent.removeAllViews()


        val view: View? = View.inflate(holder.itemView.context, R.layout.item_history_content, null)
        val textViewHistoryContentItemHeader = view?.findViewById(R.id.textViewHistoryContentItemHeader) as TextView
        val textViewHistoryContentItemName = view?.findViewById(R.id.textViewHistoryContentItemName) as TextView
        textViewHistoryContentItemHeader.text = "DID LICENSE"
        textViewHistoryContentItemName.text = "09928390239TYDVCHD8999"
        holder.linearLayoutHistoryContent.addView(view)

        val view1: View? = View.inflate(holder.itemView.context, R.layout.item_history_content, null)
        val textViewHistoryContentItemHeader1 = view1?.findViewById(R.id.textViewHistoryContentItemHeader) as TextView
        val textViewHistoryContentItemName1 = view1?.findViewById(R.id.textViewHistoryContentItemName) as TextView
        textViewHistoryContentItemHeader1.text = "AUTHORITY"
        textViewHistoryContentItemName1.text = "TC SEEHOF"
        holder.linearLayoutHistoryContent.addView(view1)

        val view2: View? = View.inflate(holder.itemView.context, R.layout.item_history_content, null)
        val textViewHistoryContentItemHeader2 = view2?.findViewById(R.id.textViewHistoryContentItemHeader) as TextView
        val textViewHistoryContentItemName2 = view2?.findViewById(R.id.textViewHistoryContentItemName) as TextView
        textViewHistoryContentItemHeader2.text = "MANUFACTURE"
        textViewHistoryContentItemName2.text = "Manufacturing Astura 673434"
        holder.linearLayoutHistoryContent.addView(view2)

        holder.qrButton.visible()
        holder.linearLayoutLicenseList.gone()
        for (productOperation in productOperations) {
            if (productOperation.at!! == order.collectedAt && productOperation.by.equals("approved")) {
                holder.qrButton.gone()
                holder.linearLayoutLicenseList.visible()
            }
        }

        holder.textViewHistoryItemDate.text = DateTimeUtils.parseDateTime(order.collectedAt!!, "dd MMM yyyy HH:mm:ss")
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = itemView.textViewHistoryItemMedicineName as TextView
        var message: TextView = itemView.textViewHistoryItemMessage as TextView
        var textViewHistoryItemDate: TextView = itemView.textViewHistoryItemDate as TextView
        var qrButton: View = itemView.linearLayoutScanQr
        var linearLayoutHistoryContent: LinearLayout = itemView.linearLayoutHistoryContent
        var linearLayoutLicenseList: LinearLayout = itemView.linearLayoutLicenseList
        var linearLayoutExpand: LinearLayout = itemView.linearLayoutExpand
        var imageViewExpand: ImageView = itemView.imageViewExpand
//        var sn: TextView = itemView.listitem_sn as TextView
    }
}
