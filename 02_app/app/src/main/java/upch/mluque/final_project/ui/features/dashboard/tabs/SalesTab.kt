package upch.mluque.final_project.ui.features.dashboard.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import upch.mluque.final_project.ui.components.VicoColumnChart
import upch.mluque.final_project.ui.components.VicoLineChart
import upch.mluque.final_project.ui.features.dashboard.RevenueSeries
import upch.mluque.final_project.ui.features.dashboard.components.ChartSection
import upch.mluque.final_project.ui.features.dashboard.components.DashboardSection
import upch.mluque.final_project.ui.features.dashboard.components.DashboardSectionsLayout
import upch.mluque.final_project.ui.features.dashboard.components.KpiCard
import upch.mluque.final_project.ui.features.dashboard.components.ServiceProgressRow
import upch.mluque.final_project.ui.features.dashboard.seriesLabel
import upch.mluque.final_project.utils.UiTranslations

@Composable
internal fun SalesTab(
    language: String,
    revenueSeries: RevenueSeries,
    averageTicket: Double,
    serviceDistribution: List<Pair<String, Float>>,
    revenueByService: List<Pair<String, Double>>
) {
    val context = LocalContext.current

    val sections = listOf(
        DashboardSection {
            ChartSection(UiTranslations.getString(context, "insights_revenue_over_time", language)) {
                VicoLineChart(
                    values = revenueSeries.points.map { it.second.toFloat() },
                    labels = revenueSeries.points.map {
                        seriesLabel(context, language, revenueSeries.granularity, it.first)
                    },
                    emptyText = UiTranslations.getString(context, "insights_no_data", language)
                )
            }
        },
        DashboardSection {
            KpiCard(
                modifier = Modifier.fillMaxWidth(),
                title = UiTranslations.getString(context, "insights_avg_ticket", language),
                value = "S/ ${String.format("%.2f", averageTicket)}",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        },
        DashboardSection {
            ChartSection(UiTranslations.getString(context, "insights_revenue_by_service", language)) {
                VicoColumnChart(
                    values = revenueByService.map { it.second.toFloat() },
                    labels = revenueByService.map {
                        UiTranslations.translateService(it.first, language, context)
                    },
                    emptyText = UiTranslations.getString(context, "insights_no_data", language)
                )
            }
        },
        DashboardSection {
            ChartSection(UiTranslations.getString(context, "insights_services", language)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (serviceDistribution.isEmpty()) {
                        Text(
                            UiTranslations.getString(context, "insights_no_data", language),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    serviceDistribution.forEach { (service, percentage) ->
                        ServiceProgressRow(service, percentage, language)
                    }
                }
            }
        }
    )

    DashboardSectionsLayout(sections)
}
