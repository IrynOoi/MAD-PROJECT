//HistoryActivity.kt
package edu.utem.ftmk.slm02

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var spinnerFilterModel: Spinner
    private lateinit var btnBack: ImageButton

    private val firebaseService = FirebaseService()
    private var allHistoryList: List<PredictionResult> = emptyList()

    private val modelsList = listOf(
        "All Models",
        "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        "qwen2.5-3b-instruct-q4_k_m.gguf",
        "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        "Phi-3.5-mini-instruct-Q4_K_M.gguf",
        "Phi-3-mini-4k-instruct-q4.gguf",
        "Vikhr-Gemma-2B-instruct-Q4_K_M.gguf"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerViewHistory)
        progressBar = findViewById(R.id.progressBarHistory)
        tvEmpty = findViewById(R.id.tvEmptyHistory)
        spinnerFilterModel = findViewById(R.id.spinnerFilterModel)
        btnBack = findViewById(R.id.btnBackHistory)

        recyclerView.layoutManager = LinearLayoutManager(this)
        btnBack.setOnClickListener { finish() }

        setupModelFilter()
        loadHistory()
    }

    private fun setupModelFilter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilterModel.adapter = adapter

        spinnerFilterModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedModel = modelsList[position]
                filterAndDisplayHistory(selectedModel)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            allHistoryList = firebaseService.getPredictionHistory()

            progressBar.visibility = View.GONE
            val currentSelection = modelsList[spinnerFilterModel.selectedItemPosition]
            filterAndDisplayHistory(currentSelection)
        }
    }

    private fun filterAndDisplayHistory(modelName: String) {
        val filteredList = if (modelName == "All Models") {
            allHistoryList
        } else {
            allHistoryList.filter { it.modelName == modelName }
        }

        if (filteredList.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            // --- NEW: Initialize Adapter with Click Listener ---
            val adapter = HistoryAdapter(filteredList) { selectedResult ->
                // Open the Detail Activity
                val intent = Intent(this, HistoryDetailActivity::class.java)
                intent.putExtra("EXTRA_RESULT", selectedResult)
                startActivity(intent)
            }
            recyclerView.adapter = adapter

        } else {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No history found for $modelName"
        }
    }
}