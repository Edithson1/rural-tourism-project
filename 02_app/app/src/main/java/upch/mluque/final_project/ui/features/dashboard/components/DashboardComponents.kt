package upch.mluque.final_project.ui.features.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import upch.mluque.final_project.ui.features.dashboard.DashboardFilter
import upch.mluque.final_project.utils.UiTranslations

private const val WIDE_LAYOUT_BREAKPOINT_DP = 600

@Composable
internal fun rememberDashboardColumns(): Int {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return if (widthDp >= WIDE_LAYOUT_BREAKPOINT_DP) 2 else 1
}

@Composable
internal fun isWideLayout(): Boolean =
    LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_BREAKPOINT_DP

internal class DashboardSection(
    val fullWidth: Boolean = false,
    val content: @Composable () -> Unit
)

@Composable
internal fun DashboardSectionsLayout(sections: List<DashboardSection>) {
    val columns = rememberDashboardColumns()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
    ) {
        items(
            count = sections.size,
            span = { index ->
                if (columns == 1 || sections[index].fullWidth) GridItemSpan(maxLineSpan)
                else GridItemSpan(1)
            }
        ) { index ->
            sections[index].content()
        }
    }
}

internal data class KpiData(
    val title: String,
    val value: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
internal fun KpiGrid(items: List<KpiData>, modifier: Modifier = Modifier) {
    val perRow = if (isWideLayout()) 4 else 2
    val rows = items.chunked(perRow)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { kpi ->
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = kpi.title,
                        value = kpi.value,
                        containerColor = kpi.containerColor,
                        contentColor = kpi.contentColor
                    )
                }
                // Add empty spacers if the row is not full to maintain alignment
                if (rowItems.size < perRow) {
                    repeat(perRow - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSelector(
    currentFilter: DashboardFilter,
    language: String,
    onFilterSelected: (DashboardFilter) -> Unit
) {
    val context = LocalContext.current
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(DashboardFilter.entries.toTypedArray()) { filter ->
            val labelKey = when (filter) {
                DashboardFilter.ALL -> "insights_filter_all"
                DashboardFilter.LAST_7_DAYS -> "insights_filter_week"
                DashboardFilter.THIS_MONTH -> "insights_filter_month"
                DashboardFilter.THIS_YEAR -> "insights_filter_year"
            }
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(UiTranslations.getString(context, labelKey, language), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun ChartSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun ServiceProgressRow(service: String, percentage: Float, language: String) {
    val context = LocalContext.current
    val translatedService = UiTranslations.translateService(service, language, context)
    val progress by animateFloatAsState(targetValue = percentage, animationSpec = tween(1200), label = "service")

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = translatedService, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "${(percentage * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
fun RevenueEstimateCard(revenue: Map<String, Double>, language: String) {
    if (revenue.isEmpty()) return
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = UiTranslations.getString(context, "insights_revenue_est", language),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            revenue.forEach { (currency, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = currency, fontWeight = FontWeight.Medium)
                    Text(text = String.format("%.2f", amount), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
