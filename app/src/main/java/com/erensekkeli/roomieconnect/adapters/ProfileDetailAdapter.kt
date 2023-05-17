package com.erensekkeli.roomieconnect.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.erensekkeli.roomieconnect.R
import com.erensekkeli.roomieconnect.databinding.ProfileDetailItemBinding

class ProfileDetailAdapter(private val data: HashMap<String, String?>): RecyclerView.Adapter<ProfileDetailAdapter.ViewHolder>() {
    class ViewHolder(val binding: ProfileDetailItemBinding): RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = data.keys.elementAt(position)
        val value = data.values.elementAt(position)
        holder.binding.title.text = key
        holder.binding.content.text = value ?: "-"

        if(key == "Status" && value != null) {
            holder.binding.content.text = when(value.toInt()) {
                0 -> holder.itemView.context.getString(R.string.status_0)
                1 -> holder.itemView.context.getString(R.string.status_1)
                2 -> holder.itemView.context.getString(R.string.status_2)
                else -> "-"
            }
        }

        if(key == "Contact Mail" && value != null && value != "-" && value != "") {
            holder.binding.cardItem.setOnClickListener {
                val subject = R.string.mail_subject.toString()
                val body = R.string.mail_body.toString()
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$value?subject=$subject&body=$body")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(value))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                try {
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, R.string.no_mail_app, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (key == "Contact Phone" && value != null && value != "-" && value != "") {
            holder.binding.cardItem.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$value")
                }
                try{
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, R.string.no_phone_app, Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


}