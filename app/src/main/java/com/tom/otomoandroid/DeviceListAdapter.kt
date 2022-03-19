package com.tom.otomoandroid

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class DeviceListAdapter(private val context: Context, private val deviceList: List<Any>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var textName: TextView = v.findViewById(R.id.deviceName)
        var textAddress: TextView = v.findViewById(R.id.deviceMac)
        var linearLayout: LinearLayout = v.findViewById(R.id.deviceLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.device_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemHolder = holder as ViewHolder
        val deviceInfo: DeviceInfo = deviceList[position] as DeviceInfo
        itemHolder.textName.text = deviceInfo.name
        itemHolder.textAddress.text = deviceInfo.hardwareAddress

        // When a device is selected
        itemHolder.linearLayout.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            // Send device details to the MainActivity
            intent.putExtra("deviceName", deviceInfo.name)
            intent.putExtra("deviceAddress", deviceInfo.hardwareAddress)
            // Call MainActivity
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}
