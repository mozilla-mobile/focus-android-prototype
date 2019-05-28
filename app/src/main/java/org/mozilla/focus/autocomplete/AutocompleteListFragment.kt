/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.autocomplete

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_autocomplete_customdomains.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mozilla.components.browser.domains.CustomDomains
import org.mozilla.focus.R
import org.mozilla.focus.settings.BaseSettingsFragment
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import java.util.Collections
import kotlin.coroutines.CoroutineContext

typealias DomainFormatter = (String) -> String
/**
 * Fragment showing settings UI listing all custom autocomplete domains entered by the user.
 */
open class AutocompleteListFragment : Fragment(), CoroutineScope {
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    /**
     * ItemTouchHelper for reordering items in the domain list.
     */
    val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(
            object : SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition

                    (recyclerView.adapter as DomainListAdapter).move(from, to)

                    return true
                }

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {}

                override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
                    if (viewHolder is AddActionViewHolder) {
                        return ItemTouchHelper.Callback.makeMovementFlags(0, 0)
                    }

                    return super.getMovementFlags(recyclerView, viewHolder)
                }

                override fun onSelectedChanged(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (viewHolder is DomainViewHolder) {
                        viewHolder.onSelected()
                    }
                }

                override fun clearView(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    if (viewHolder is DomainViewHolder) {
                        viewHolder.onCleared()
                    }
                }

                override fun canDropOver(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    current: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    if (target is AddActionViewHolder) {
                        return false
                    }

                    return super.canDropOver(recyclerView, current, target)
                }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * In selection mode the user can select and remove items. In non-selection mode the list can
     * be reordered by the user.
     */
    open fun isSelectionMode() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_autocomplete_customdomains, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        domainList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity, androidx.recyclerview.widget.RecyclerView.VERTICAL, false)
        domainList.adapter = DomainListAdapter()
        domainList.setHasFixedSize(true)

        if (!isSelectionMode()) {
            itemTouchHelper.attachToRecyclerView(domainList)
        }
    }

    override fun onResume() {
        super.onResume()

        if (job.isCancelled) {
            job = Job()
        }

        (activity as BaseSettingsFragment.ActionBarUpdater).apply {
            updateTitle(R.string.preference_autocomplete_subitem_manage_sites)
            updateIcon(R.drawable.ic_back)
        }

        (domainList.adapter as DomainListAdapter).refresh(activity!!) {
            activity?.invalidateOptionsMenu()
        }
    }

    override fun onPause() {
        job.cancel()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_autocomplete_list, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        val removeItem = menu?.findItem(R.id.remove)

        removeItem?.let {
            it.isVisible = isSelectionMode() || domainList.adapter!!.itemCount > 1
            val isEnabled = !isSelectionMode() || (domainList.adapter as DomainListAdapter).selection().isNotEmpty()
            ViewUtils.setMenuItemEnabled(it, isEnabled)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.remove -> {
            fragmentManager!!
                    .beginTransaction()
                    .replace(R.id.container, AutocompleteRemoveFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /**
     * Adapter implementation for the list of custom autocomplete domains.
     */
    inner class DomainListAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
        private val domains: MutableList<String> = mutableListOf()
        private val selectedDomains: MutableList<String> = mutableListOf()

        fun refresh(context: Context, body: (() -> Unit)? = null) {
            launch(Main) {
                val updatedDomains = async { CustomDomains.load(context) }.await()

                domains.clear()
                domains.addAll(updatedDomains)

                notifyDataSetChanged()

                body?.invoke()
            }
        }

        override fun getItemViewType(position: Int) =
                when (position) {
                    domains.size -> AddActionViewHolder.LAYOUT_ID
                    else -> DomainViewHolder.LAYOUT_ID
                }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder =
                when (viewType) {
                    AddActionViewHolder.LAYOUT_ID ->
                            AddActionViewHolder(
                                    this@AutocompleteListFragment,
                                    LayoutInflater.from(parent.context).inflate(viewType, parent, false))
                    DomainViewHolder.LAYOUT_ID ->
                            DomainViewHolder(
                                    LayoutInflater.from(parent.context).inflate(viewType, parent, false),
                                    { AutocompleteDomainFormatter.format(it) })
                    else -> throw IllegalArgumentException("Unknown view type: $viewType")
                }

        override fun getItemCount(): Int = domains.size + if (isSelectionMode()) 0 else 1

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            if (holder is DomainViewHolder) {
                holder.bind(
                        domains[position],
                        isSelectionMode(),
                        selectedDomains,
                        itemTouchHelper,
                        this@AutocompleteListFragment)
            }
        }

        override fun onViewRecycled(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
            if (holder is DomainViewHolder) {
                holder.checkBoxView.setOnCheckedChangeListener(null)
            }
        }

        fun selection(): List<String> = selectedDomains

        fun move(from: Int, to: Int) {
            Collections.swap(domains, from, to)
            notifyItemMoved(from, to)

            launch(IO) {
                CustomDomains.save(activity!!.applicationContext, domains)

                TelemetryWrapper.reorderAutocompleteDomainEvent(from, to)
            }
        }
    }

    /**
     * ViewHolder implementation for a domain item in the list.
     */
    private class DomainViewHolder(
        itemView: View,
        val domainFormatter: DomainFormatter? = null
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val domainView: TextView = itemView.findViewById(R.id.domainView)
        val checkBoxView: CheckBox = itemView.findViewById(R.id.checkbox)
        val handleView: View = itemView.findViewById(R.id.handleView)

        companion object {
            val LAYOUT_ID = R.layout.item_custom_domain
        }

        fun bind(
            domain: String,
            isSelectionMode: Boolean,
            selectedDomains: MutableList<String>,
            itemTouchHelper: ItemTouchHelper,
            fragment: AutocompleteListFragment
        ) {
            domainView.text = domainFormatter?.invoke(domain) ?: domain

            checkBoxView.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            checkBoxView.isChecked = selectedDomains.contains(domain)
            checkBoxView.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (isChecked) {
                    selectedDomains.add(domain)
                } else {
                    selectedDomains.remove(domain)
                }

                fragment.activity?.invalidateOptionsMenu()
            }

            handleView.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            handleView.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                false
            }

            if (isSelectionMode) {
                itemView.setOnClickListener {
                    checkBoxView.isChecked = !checkBoxView.isChecked
                }
            }
        }

        fun onSelected() {
            itemView.setBackgroundColor(Color.DKGRAY)
        }

        fun onCleared() {
            itemView.setBackgroundColor(0)
        }
    }

    /**
     * ViewHolder implementation for a "Add custom domain" item at the bottom of the list.
     */
    private class AddActionViewHolder(
        val fragment: AutocompleteListFragment,
        itemView: View
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                fragment.fragmentManager!!
                        .beginTransaction()
                        .replace(R.id.container, AutocompleteAddFragment())
                        .addToBackStack(null)
                        .commit()
            }
        }

        companion object {
            val LAYOUT_ID = R.layout.item_add_custom_domain
        }
    }
}
