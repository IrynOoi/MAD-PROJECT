// CsvExporter.kt
// CsvExporter.kt
package edu.utem.ftmk.slm02

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportPredictionsToCache(
        context: Context,
        predictions: List<PredictionResult>,
        modelNameHeader: String
    ): File {

        // 1. Generate Filename with Model Name
        val fileTimeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val safeModelName = modelNameHeader
            .replace("All Models", "all_models", ignoreCase = true)
            .replace(".gguf", "", ignoreCase = true)
            .trim()
            .replace(" ", "_")
            .lowercase()

        val fileName = "history_${safeModelName}_$fileTimeStamp.csv"
        val file = File(context.cacheDir, fileName)

        // 2. Formatter for the CSV rows
        val rowDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        FileWriter(file).use { writer ->

            // Sort by ID
            val groupedByModel = predictions
                .sortedBy { it.foodItem.id.toIntOrNull() ?: 0 }
                .groupBy { it.modelName }

            groupedByModel.forEach { (modelName, results) ->

                writer.append("MODEL:,\"$modelName\"\n")

                // ---- Table Header (ALL TIME UNITS IN SECONDS) ----
                writer.append(
                    "Date & Time,Data ID,Food Name,Ingredients,Link,Raw Allergens,Mapped Allergen,Predicted," +
                            "Quality: Precision,Quality: Recall,Quality: F1 Score,Quality: Exact Match (%),Quality: Hamming Loss,Quality: FNR (%)," +
                            "Safety: Hallucination (%),Safety: Over-Prediction (%),Safety: Abstention Success (0/1)," +
                            // CHANGED: All time metrics are now (s)
                            "Efficiency: Latency (s),Efficiency: Total Time (s),Efficiency: TTFT (s),Efficiency: ITPS (tokens/s),Efficiency: OTPS (tokens/s),Efficiency: Eval Time (s)," +
                            "Efficiency: Java Heap (KB),Efficiency: Native Heap (KB),Efficiency: PSS (KB)\n"
                )

                results.forEach { result ->
                    val item = result.foodItem
                    val groundTruth = item.allergensMapped
                    val predicted = result.predictedAllergens

                    // Calculate metrics
                    val calc = MetricsCalculator.calculate(groundTruth, predicted)
                    val m = result.metrics

                    // Format Date
                    val readableDate = rowDateFormatter.format(Date(result.timestamp))

                    // Percentages
                    val exactMatchPct = if (calc.exactMatch) 100 else 0
                    val fnrPct = calc.falseNegativeRate * 100.0
                    val hallucinationPct = if (calc.isHallucination) 100 else 0
                    val overPredPct = if (calc.isOverPrediction) 100 else 0
                    val abstention = if (calc.isAbstentionSuccess) 1 else 0

                    // --- TIME CONVERSIONS (ms -> s) ---
                    val latencySec = (m?.latencyMs ?: 0) / 1000.0
                    val totalTimeSec = latencySec // Total Time is same as Latency
                    val ttftSec = (m?.ttft ?: 0) / 1000.0
                    val evalTimeSec = (m?.oet ?: 0) / 1000.0

                    // Sanitize strings
                    val safeIngredients = item.ingredients.replace("\"", "\"\"")
                    val safeName = item.name.replace("\"", "\"\"")
                    val safeLink = item.link.replace("\"", "\"\"")

                    writer.append(
                        // --- FOOD DETAILS ---
                        "\"$readableDate\"," +
                                "\"${item.id}\"," +
                                "\"$safeName\"," +
                                "\"$safeIngredients\"," +
                                "\"$safeLink\"," +
                                "\"${item.allergens}\"," +
                                "\"$groundTruth\"," +
                                "\"$predicted\"," +

                                // --- QUALITY ---
                                "${String.format("%.4f", calc.precision)}," +
                                "${String.format("%.4f", calc.recall)}," +
                                "${String.format("%.4f", calc.f1Score)}," +
                                "$exactMatchPct," +
                                "${String.format("%.4f", calc.hammingLoss)}," +
                                "${String.format("%.2f", fnrPct)}," +

                                // --- SAFETY ---
                                "$hallucinationPct," +
                                "$overPredPct," +
                                "$abstention," +

                                // --- EFFICIENCY (Speed in Seconds) ---
                                "${String.format("%.4f", latencySec)}," +    // Latency (s)
                                "${String.format("%.4f", totalTimeSec)}," +  // Total Time (s)
                                "${String.format("%.4f", ttftSec)}," +       // TTFT (s)
                                "${m?.itps ?: ""}," +
                                "${m?.otps ?: ""}," +
                                "${String.format("%.4f", evalTimeSec)}," +   // Eval Time (s)

                                // --- EFFICIENCY (Memory) ---
                                "${m?.javaHeapKb ?: ""}," +
                                "${m?.nativeHeapKb ?: ""}," +
                                "${m?.totalPssKb ?: ""}\n"
                    )
                }
                writer.append("\n") // Empty row between models
            }
        }
        return file
    }

    fun getShareableUri(context: Context, file: File) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
}