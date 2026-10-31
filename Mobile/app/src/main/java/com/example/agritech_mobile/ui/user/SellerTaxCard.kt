package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.agritech_mobile.data.remote.dto.SellerTaxEstimate
import com.example.agritech_mobile.ui.theme.AgritechTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SellerTaxCard(tax: SellerTaxEstimate, selectedMonth: Int?) {
    var showDetails by remember(tax.year) { mutableStateOf(false) }
    StatisticsCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Thuế ước tính", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp)) {
                Text("Năm ${tax.year}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tax.totalTax?.let(::statisticsMoney) ?: "Chưa xác định",
                style = MaterialTheme.typography.headlineMedium,
                color = if (tax.totalTax == null) MaterialTheme.colorScheme.onSurfaceVariant else StatisticsGreen)
            Text(when (tax.status) {
                "BELOW_THRESHOLD" -> "Chưa vượt ngưỡng tính thuế"
                "INCOME_METHOD_REQUIRED" -> "Trên 3 tỷ: cần tính theo thu nhập"
                "OUTSIDE_POLICY_YEAR" -> "Chưa hỗ trợ năm này"
                else -> "Tổng GTGT và TNCN ước tính"
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaxValue("Doanh thu tính thuế", tax.annualRevenue)
            TaxValue("Ngưỡng cả năm", tax.threshold)
        }
        Text("Chỉ tính hàng có vốn > 0 · Ước tính theo năm",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { showDetails = true }, contentPadding = PaddingValues(0.dp)) {
            Text("Xem cách tính", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
    if (showDetails) {
        ModalBottomSheet(onDismissRequest = { showDetails = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            TaxDetails(tax, selectedMonth)
        }
    }
}

@Composable
private fun TaxDetails(tax: SellerTaxEstimate, selectedMonth: Int?) {
    var showSources by remember(tax.year) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cách tính thuế", style = MaterialTheme.typography.titleLarge)
            TaxNote("Cả năm ${tax.year} · Chỉ các đơn đã hoàn thành")
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaxValue("Doanh thu tính thuế", tax.annualRevenue)
            TaxNote("Chỉ hàng có giá vốn > 0 khi đặt đơn. Hàng vốn = 0 hoặc chưa nhập vốn vẫn nằm trong doanh thu tổng.")
            if (tax.excludedRevenue > 0) TaxValue("Doanh thu loại khỏi tính thuế", tax.excludedRevenue)
        }
        if (tax.status == "OUTSIDE_POLICY_YEAR") {
            TaxNote("Chưa hỗ trợ quy tắc thuế cho năm trước 2026.")
        } else if (tax.status == "BELOW_THRESHOLD") {
            TaxFormula("Chưa vượt ngưỡng", "Doanh thu năm ≤ ${statisticsMoney(tax.threshold)}", 0.0)
            TaxNote("Chỉ tính thuế khi vượt ngưỡng. Đúng ${statisticsMoney(tax.threshold)} vẫn ước tính 0 đ.")
        } else {
            TaxFormula("Thuế GTGT · ${taxPercent(tax.vatRate)}%",
                "${statisticsMoney(tax.annualRevenue)} × ${taxPercent(tax.vatRate)}%", tax.vat)
            TaxNote("GTGT tính trên toàn bộ doanh thu tính thuế, không chỉ phần vượt ngưỡng.")
            if (tax.status == "INCOME_METHOD_REQUIRED") {
                TaxNote("Trên 3 tỷ đồng/năm cần tính TNCN theo thu nhập và chi phí hợp lệ. Chưa xác định tổng thuế.")
            } else {
                TaxFormula("Thuế TNCN · ${taxPercent(tax.incomeTaxRate)}%",
                    "(${statisticsMoney(tax.annualRevenue)} − ${statisticsMoney(tax.threshold)}) × ${taxPercent(tax.incomeTaxRate)}%",
                    tax.incomeTaxBeforeReduction)
                if (tax.incomeTaxReductionRate > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaxValue("Giảm ${taxPercent(tax.incomeTaxReductionRate)}% TNCN", tax.incomeTaxReduction)
                        TaxNote("Ước tính ưu đãi năm ${tax.year}, giả định tổng doanh thu các kênh không quá 10 tỷ đồng.")
                        TaxValue("TNCN sau giảm", tax.incomeTax)
                    }
                }
            }
        }
        if (tax.totalTax != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TaxValue("Tổng thuế ước tính", tax.totalTax, emphasize = true)
                if (selectedMonth == null) {
                    TaxValue("Lợi nhuận đã tính sau thuế", tax.annualProfitAfterEstimatedTax)
                    TaxNote("Lợi nhuận sau giao hàng − thuế ước tính. Giá vốn chưa nhập được tính 0 đ. Chưa trừ phí sàn, thanh toán và chi phí khác.")
                }
            }
        }
        if (selectedMonth != null) TaxNote("Đang xem tháng $selectedMonth; thuế vẫn tính cả năm và chưa phân bổ vào lợi nhuận tháng.")
        Column(Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lưu ý về ước tính", style = MaterialTheme.typography.titleSmall)
            TaxNote("App coi hàng có vốn > 0 là mua bán lại. Giá vốn không quyết định nghĩa vụ thuế thực tế; hàng tự sản xuất có thể được miễn.")
            TaxNote("Chỉ gồm tiền hàng trên app, không gồm phí giao và doanh thu ngoài app. Giả định hộ/cá nhân cư trú tính thuế trên doanh thu; cần đối chiếu trước khi kê khai.")
            TaxNote("Đây không phải thuế đã nộp và không tự trừ tiền đơn. Khấu trừ qua sàn được xét riêng.")
        }
        Column {
            TextButton(onClick = { showSources = !showSources }, contentPadding = PaddingValues(0.dp)) {
                Text("Căn cứ pháp lý", style = MaterialTheme.typography.labelLarge)
                Icon(if (showSources) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null, modifier = Modifier.size(20.dp))
            }
            if (showSources) {
                TaxSource("Ngưỡng · Nghị định 141/2026/NĐ-CP",
                    "https://xaydungchinhsach.chinhphu.vn/toan-van-nghi-dinh-so-141-2026-nd-cp-nang-nguong-doanh-thu-khong-phai-chiu-thue-len-1-ty-dong-119260504154326455.htm") { uriHandler.openUri(it) }
                TaxSource("GTGT · Điều 12 Luật GTGT hợp nhất",
                    "https://xdcs.cdnchinhphu.vn/446259493575335936/2026/6/5/2026-305-114-vbhn-vpqh-17806273642891917566820.pdf") { uriHandler.openUri(it) }
                TaxSource("TNCN · Điều 7 Luật 109/2025/QH15",
                    "https://chinhphu.vn/?classid=1&docid=216495&pageid=27160&typegroupid=3") { uriHandler.openUri(it) }
                TaxSource("Giảm thuế · Nghị quyết 43/2026/QH16",
                    "https://chinhphu.vn/?classid=0&docid=219330&pageid=27160") { uriHandler.openUri(it) }
            }
        }
    }
}

@Composable
private fun TaxFormula(title: String, formula: String, amount: Double?) {
    Column(Modifier.fillMaxWidth()
        .background(StatisticsGreen.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaxValue(title, amount, emphasize = true)
        TaxNote(formula)
    }
}

@Composable
private fun TaxNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun taxPercent(rate: Double) = java.math.BigDecimal.valueOf(rate).movePointRight(2)
    .stripTrailingZeros().toPlainString().replace('.', ',')

@Composable
private fun TaxValue(label: String, amount: Double?, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount?.let(::statisticsMoney) ?: "Chưa xác định", Modifier.weight(1f),
            textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) StatisticsGreen else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TaxSource(label: String, url: String, onOpen: (String) -> Unit) {
    TextButton(onClick = { onOpen(url) }, contentPadding = PaddingValues(0.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Thuế - Chưa vượt ngưỡng", showBackground = true, widthDp = 360)
@Preview(name = "Thuế - Chữ lớn", showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
private fun SellerTaxCardPreview() {
    AgritechTheme { SellerTaxCard(taxPreviewData(), selectedMonth = 10) }
}

@Preview(name = "Thuế - Chi tiết", showBackground = true, widthDp = 411, heightDp = 1000)
@Composable
private fun SellerTaxDetailsPreview() {
    AgritechTheme {
        TaxDetails(taxPreviewData().copy(status = "ESTIMATED", annualRevenue = 1_200_000_000.0,
            vat = 12_000_000.0, incomeTaxBase = 200_000_000.0, incomeTaxBeforeReduction = 1_000_000.0,
            incomeTaxReduction = 300_000.0, incomeTax = 700_000.0, totalTax = 12_700_000.0), null)
    }
}

private fun taxPreviewData() = SellerTaxEstimate(
    year = 2026, status = "BELOW_THRESHOLD", annualRevenue = 120_000_000.0,
    excludedRevenue = 50_000_000.0, qualifyingItems = 12, threshold = 1_000_000_000.0,
    vatRate = 0.01, incomeTaxRate = 0.005, incomeTaxReductionRate = 0.3,
    vat = 0.0, incomeTaxBase = 0.0, incomeTaxBeforeReduction = 0.0, incomeTaxReduction = 0.0,
    incomeTax = 0.0, totalTax = 0.0, annualProfitAfterEstimatedTax = null
)
