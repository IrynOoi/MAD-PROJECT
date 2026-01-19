//HistoryAdapter.kt
package edu.utem.ftmk.slm02

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val historyList: List<PredictionResult>,
    private val onItemClick: (PredictionResult) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs must match your item layout (e.g., item_history.xml)
        val tvFoodName: TextView = view.findViewById(R.id.tvHistoryFoodName)
        val tvModelName: TextView = view.findViewById(R.id.tvHistoryModelName)
        val tvPredicted: TextView = view.findViewById(R.id.tvHistoryPredicted)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvLatency: TextView = view.findViewById(R.id.tvHistoryLatency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvFoodName.text = item.foodItem.name

        // Clean model name
        val cleanName = item.modelName
            .replace(".gguf", "")
            .replace("-instruct-Q4_K_M", "")
            .replace("-instruct-q4_k_m", "")

        holder.tvModelName.text = "Model: $cleanName"
        holder.tvPredicted.text = "Predicted: ${item.predictedAllergens}"

        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(item.timestamp))

        if (item.metrics != null) {
            holder.tvLatency.text = "${item.metrics.latencyMs} ms"
            holder.tvLatency.visibility = View.VISIBLE
        } else {
            holder.tvLatency.visibility = View.GONE
        }

        // Handle Click -> Passes item to Activity
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = historyList.size
}