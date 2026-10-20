package com.example.financecalc1

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MainMenuAdapter(private val context: Context, private val menuItems: List<MainMenuItem>) :
    RecyclerView.Adapter<MainMenuAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardViewMenuItem)
        val titleTextView: TextView = itemView.findViewById(R.id.tvMenuTitle)
        val iconImageView: ImageView = itemView.findViewById(R.id.ivMenuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_main_menu, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = menuItems[position]
        holder.titleTextView.text = currentItem.title
        holder.iconImageView.setImageResource(currentItem.iconResId) // Здесь будет установка иконки

        holder.cardView.setOnClickListener {
            context.startActivity(currentItem.intent)
        }
    }

    override fun getItemCount() = menuItems.size
}