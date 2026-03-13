package com.example.vapestoreapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vapestoreapp.ui.theme.PastelBlue
import com.example.vapestoreapp.ui.theme.PastelLavender
import com.example.vapestoreapp.ui.theme.PastelMint
import com.example.vapestoreapp.ui.theme.PastelPink
import com.example.vapestoreapp.ui.theme.TextOnPastel

enum class KPICardColor(val bg: Color, val textColor: Color) {
    MINT(PastelMint, TextOnPastel),
    BLUE(PastelBlue, TextOnPastel),
    LAVENDER(PastelLavender, TextOnPastel),
    PINK(PastelPink, TextOnPastel)
}

@Composable
fun KPICard(
    title: String,
    value: String,
    icon: ImageVector,
    color: KPICardColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = color.bg,
                shape = MaterialTheme.shapes.large
            )
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    color = color.textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.textColor.copy(alpha = 0.4f)
                )
            }
            Text(
                text = value,
                color = color.textColor,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
