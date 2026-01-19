//History Detail Activity.kt
package edu.utem.ftmk.slm02

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail) // Points to your provided XML

        // Get Data
        val result = intent.getParcelableExtra<PredictionResult>("EXTRA_RESULT")

        if (result != null) {
            setupViews(result)
        } else {
            finish() // Close if data is missing
        }

        findViewById<ImageButton>(R.id.btnBackDetail).setOnClickListener {
            finish()
        }
    }

    private fun setupViews(result: PredictionResult) {
        // --- 1. Basic Info ---
        findViewById<TextView>(R.id.tvDetailName).text = result.foodItem.name
        findViewById<TextView>(R.id.tvDetailId).text = "#${result.foodItem.id}"

        // FIX 1: Changed .source to .link
        findViewById<TextView>(R.id.tvDetailLink).text = result.foodItem.link

        // --- 2. Composition ---
        findViewById<TextView>(R.id.tvDetailIngredients).text = result.foodItem.ingredients

        // FIX 2: Changed .allergensRaw to .allergens
        findViewById<TextView>(R.id.tvDetailRawAllergens).text = result.foodItem.allergens

        findViewById<TextView>(R.id.tvDetailMappedAllergens).text = result.foodItem.allergensMapped

        // --- 3. AI Analysis ---
        val cleanModelName = result.modelName
            .replace(".gguf", "")
            .replace("-instruct-Q4_K_M", "")
            .replace("-instruct-q4_k_m", "") // Added lowercase check

        findViewById<TextView>(R.id.tvDetailModelName).text = cleanModelName
        findViewById<TextView>(R.id.tvDetailPredicted).text = result.predictedAllergens

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        findViewById<TextView>(R.id.tvDetailTimestamp).text = sdf.format(Date(result.timestamp))

        // --- 4. Validation (Calculated on the fly) ---
        val quality = MetricsCalculator.calculate(result.foodItem.allergensMapped, result.predictedAllergens)

        findViewById<TextView>(R.id.tvValPrecision).text = "%.2f".format(quality.precision)
        findViewById<TextView>(R.id.tvValRecall).text = "%.2f".format(quality.recall)
        findViewById<TextView>(R.id.tvValF1).text = "%.2f".format(quality.f1Score)

        val exactMatchText = if (quality.exactMatch) "YES" else "NO"
        findViewById<TextView>(R.id.tvValExactMatch).text = exactMatchText

        findViewById<TextView>(R.id.tvValHamming).text = "%.2f".format(quality.hammingLoss)
        findViewById<TextView>(R.id.tvValFNR).text = "%.2f".format(quality.falseNegativeRate)

        // Safety Metrics
        findViewById<TextView>(R.id.tvValHallucination).text = if (quality.isHallucination) "YES" else "NO"
        findViewById<TextView>(R.id.tvValOverPred).text = if (quality.isOverPrediction) "YES" else "NO"

        // TNR (Abstention) logic
        val tnrText = if (quality.isAbstentionCase) {
            if (quality.isAbstentionSuccess) "Correct" else "Failed"
        } else {
            "N/A"
        }
        findViewById<TextView>(R.id.tvValAbstention).text = tnrText

        // --- 5. Efficiency Metrics (From PredictionResult) ---
        val metrics = result.metrics
        if (metrics != null) {
            // Conversions: ms -> s, KB -> MB
            val latencySec = metrics.latencyMs / 1000.0
            val ttftSec = metrics.ttft / 1000.0
            val oetSec = metrics.oet / 1000.0

            val javaMb = metrics.javaHeapKb / 1024.0
            val nativeMb = metrics.nativeHeapKb / 1024.0
            val pssMb = metrics.totalPssKb / 1024.0

            findViewById<TextView>(R.id.tvValLatency).text = "%.2f s".format(latencySec)
            findViewById<TextView>(R.id.tvValTotalTime).text = "%.2f s".format(latencySec)
            findViewById<TextView>(R.id.tvValTTFT).text = "%.2f s".format(ttftSec)
            findViewById<TextView>(R.id.tvValOET).text = "%.2f s".format(oetSec)

// Add .toDouble()
            findViewById<TextView>(R.id.tvValITPS).text = "%.1f".format(metrics.itps.toDouble())
            findViewById<TextView>(R.id.tvValOTPS).text = "%.1f".format(metrics.otps.toDouble())

            findViewById<TextView>(R.id.tvValJava).text = "%.2f MB".format(javaMb)
            findViewById<TextView>(R.id.tvValNative).text = "%.2f MB".format(nativeMb)
            findViewById<TextView>(R.id.tvValPSS).text = "%.2f MB".format(pssMb)
        } else {
            findViewById<TextView>(R.id.tvValLatency).text = "N/A"
        }
    }
}