/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.menu.home

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.mvp_menu_item.view.icon
import kotlinx.android.synthetic.main.mvp_menu_item.view.title
import org.mozilla.focus.R
import org.mozilla.focus.utils.MvpFeatureManager
import org.mozilla.focus.whatsnew.WhatsNew

/**
 * Adapter implementation to be used with the HomeMenu class.
 *
 * The menu structure is hard-coded in the init block of the class.
 */
class MvpHomeMenuAdapter(
    context: Context,
    private val listener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items: List<MvpMenuItem> = listOf(
        MvpMenuItem(R.id.whats_new, MvpWhatsNewViewHolder.LAYOUT_ID, context.getString(R.string.menu_whats_new)),
        MvpMenuItem(R.id.help, MvpMenuItemViewHolder.LAYOUT_ID, context.getString(R.string.menu_help)),
        MvpMenuItem(R.id.settings, MvpMenuItemViewHolder.LAYOUT_ID, context.getString(R.string.menu_settings))
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            MvpWhatsNewViewHolder.LAYOUT_ID -> MvpWhatsNewViewHolder(view, listener)
            MvpMenuItemViewHolder.LAYOUT_ID -> MvpMenuItemViewHolder(view as ConstraintLayout, listener)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MvpMenuItemViewHolder -> holder.bind(items[position])
            is MvpWhatsNewViewHolder -> holder.bind()
            else -> throw IllegalArgumentException("Unknown view holder")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position].viewType
}

/**
 * ViewHolder implementation for regular menu items with just a label.
 */
private class MvpMenuItemViewHolder(
    val containerView: ConstraintLayout,
    val listener: View.OnClickListener
) : RecyclerView.ViewHolder(containerView) {

    companion object {
        val LAYOUT_ID: Int = R.layout.mvp_menu_item
    }

    fun bind(item: MvpMenuItem) {
        val iconResourceId = if (item.id == R.id.help) {
            R.drawable.ic_help
        } else {
            if (MvpFeatureManager.isEnabled) {
                R.drawable.ic_mvp_settings
            } else {
                R.drawable.ic_settings
            }
        }
        containerView.apply {
            id = item.id
            title.text = item.label
            icon.setBackgroundResource(iconResourceId)
            setOnClickListener(listener)
        }
    }
}

/**
 * ViewHolder implementation for the "What's New" menu item. The item looks differently based
 * on whether the app was updated recently.
 */
private class MvpWhatsNewViewHolder(
    itemView: View,
    val listener: View.OnClickListener
) : RecyclerView.ViewHolder(itemView) {
    val dotView: View = itemView.findViewById(R.id.dot)

    companion object {
        val LAYOUT_ID: Int = R.layout.mvp_menu_whatsnew
    }

    fun bind() {
        val updated = WhatsNew.shouldHighlightWhatsNew(itemView.context)

        if (updated) {
            itemView.setBackgroundResource(R.drawable.menu_item_dark_background)
        }

        itemView.setOnClickListener(listener)

        dotView.visibility = if (updated) View.VISIBLE else View.GONE
    }
}

/**
 * Simple data class for describing menu items.
 */
private class MvpMenuItem(
    val id: Int,
    val viewType: Int,
    val label: String
)
