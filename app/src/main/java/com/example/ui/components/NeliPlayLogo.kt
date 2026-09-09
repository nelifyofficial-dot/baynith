package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NeliMagentaAccent
import com.example.ui.theme.NeliOrangeAccent

@Composable
fun NeliPlayLogo(
    modifier: Modifier = Modifier,
    size: Int = 36,
    showSubtitle: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_neliplay_logo),
            contentDescription = "NeliPlay Logo",
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (size * 0.65).sp,
                            letterSpacing = (-0.5).sp
                        )
                    ) {
                        append("Neli")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = NeliOrangeAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = (size * 0.65).sp,
                            letterSpacing = (-0.5).sp
                        )
                    ) {
                        append("play")
                    }
                }
            )

            if (showSubtitle) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MOVIES • SERIES • MORE",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
