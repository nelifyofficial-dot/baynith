package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeliBluePrimary
import com.example.ui.theme.NeliCyanAccent
import com.example.ui.theme.NeliSurface
import com.example.ui.theme.NeliSurfaceElevated
import com.example.ui.theme.NeliSurfaceVariant
import com.example.ui.theme.NeliTextPrimary
import com.example.ui.theme.NeliTextSecondary
import com.example.util.Country
import com.example.util.RegionService

@Composable
fun CountryPickerDialog(
    selectedCountryCode: String,
    onCountrySelected: (Country) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        RegionService.searchCountries(searchQuery)
    }

    val eastAfricaList = remember(filteredCountries) {
        filteredCountries.filter { it.isEastAfrica }
    }
    val otherCountriesList = remember(filteredCountries) {
        filteredCountries.filter { !it.isEastAfrica }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
            .testTag("country_picker_dialog"),
        containerColor = NeliSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeliBluePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = NeliCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Choose your country",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeliTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select your region to personalize available movies, audio languages, and Swahili releases.",
                    fontSize = 12.sp,
                    color = NeliTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or code...", fontSize = 13.sp, color = NeliTextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeliTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = NeliTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeliSurface,
                        unfocusedContainerColor = NeliSurface,
                        focusedBorderColor = NeliCyanAccent,
                        unfocusedBorderColor = NeliSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("country_search_field")
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (eastAfricaList.isNotEmpty()) {
                    item {
                        Text(
                            text = "EAST AFRICA (SWAHILI REGION)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeliCyanAccent,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    items(eastAfricaList, key = { it.code }) { country ->
                        CountryRowItem(
                            country = country,
                            isSelected = country.code.equals(selectedCountryCode, ignoreCase = true),
                            onClick = {
                                onCountrySelected(country)
                                onDismissRequest()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = NeliSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (otherCountriesList.isNotEmpty()) {
                    item {
                        Text(
                            text = "ALL COUNTRIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeliTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    items(otherCountriesList, key = { it.code }) { country ->
                        CountryRowItem(
                            country = country,
                            isSelected = country.code.equals(selectedCountryCode, ignoreCase = true),
                            onClick = {
                                onCountrySelected(country)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = NeliTextSecondary)
            }
        }
    )
}

@Composable
private fun CountryRowItem(
    country: Country,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) NeliBluePrimary.copy(alpha = 0.25f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("country_item_${country.code}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = country.flagEmoji,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = country.name,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else NeliTextPrimary
                    )
                    if (country.isEastAfrica) {
                        Text(
                            text = "Full Swahili library access",
                            fontSize = 11.sp,
                            color = NeliCyanAccent
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = country.code,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) NeliCyanAccent else NeliTextSecondary
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = NeliCyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
