@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.oddjobs.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.oddjobs.app.domain.Catalog
import com.oddjobs.app.domain.Category
import com.oddjobs.app.ui.LocalLang
import com.oddjobs.app.ui.tr
import com.oddjobs.app.ui.theme.Accent
import com.oddjobs.app.ui.theme.Border
import com.oddjobs.app.ui.theme.Canvas
import com.oddjobs.app.ui.theme.CardColor
import com.oddjobs.app.ui.theme.ErrorRed
import com.oddjobs.app.ui.theme.Muted
import com.oddjobs.app.ui.theme.Primary
import com.oddjobs.app.ui.theme.PrimaryLight
import com.oddjobs.app.ui.theme.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary, contentColor = Color.White,
            disabledContainerColor = Border, disabledContentColor = Muted
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    danger: Boolean = false
) {
    val color = if (danger) ErrorRed else Primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (enabled) color else Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun PhotoImage(path: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            } catch (e: Throwable) {
                null
            }
        }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(Border))
    }
}

private val avatarColors = listOf(
    Color(0xFF0E5C4A), Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFC4922E),
    Color(0xFFDB2777), Color(0xFF0891B2), Color(0xFFEA580C), Color(0xFF4F46E5)
)

fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    val first = parts[0].take(1)
    val second = if (parts.size > 1) parts[1].take(1) else ""
    return (first + second).uppercase()
}

@Composable
fun Avatar(name: String, photoPath: String?, size: Dp = 44.dp) {
    val color = avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size]
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null && File(photoPath).exists()) {
            PhotoImage(photoPath, Modifier.fillMaxSize())
        } else {
            Text(initialsOf(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.6f).sp)
        }
    }
}

@Composable
fun RatingStars(value: Double, size: Dp = 16.dp) {
    Row {
        for (i in 1..5) {
            val icon = when {
                value >= i - 0.25 -> Icons.Filled.Star
                value >= i - 0.75 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(size))
        }
    }
}

@Composable
fun RatingInput(value: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..5) {
            IconButton(onClick = { onChange(i) }, modifier = Modifier.size(52.dp)) {
                Icon(
                    if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = i.toString(),
                    tint = Accent,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
fun Pill(text: String, bg: Color = PrimaryLight, fg: Color = Primary) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), maxLines = 1
        )
    }
}

@Composable
fun VerifiedBadge(level: Int) {
    if (level < 2) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(tr("ID verified", "আইডি যাচাইকৃত"), color = Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = CardColor)
    val border = BorderStroke(1.dp, Border)
    val shape = RoundedCornerShape(16.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(14.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String = "",
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(actionText, onAction, Modifier.width(220.dp))
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "পিছনে"))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
    )
}

@Composable
fun ScreenFrame(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { TopBar(title, onBack, actions) },
        bottomBar = bottomBar,
        containerColor = Canvas,
        content = content
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int = 0,
    prefix: String? = null,
    error: Boolean = false,
    supporting: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (maxLength == 0 || it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        isError = error,
        prefix = if (prefix != null) ({ Text(prefix) }) else null,
        supportingText = if (supporting != null) ({ Text(supporting) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary, focusedLabelColor = Primary, unfocusedBorderColor = Border,
            focusedContainerColor = CardColor, unfocusedContainerColor = CardColor
        )
    )
}

@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Border, focusedContainerColor = CardColor, unfocusedContainerColor = CardColor
            )
        )
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
fun CategoryGrid(
    selected: Set<String>,
    onToggle: (Category) -> Unit,
    columns: Int = 3,
    modifier: Modifier = Modifier,
    items: List<Category> = Catalog.categories
) {
    val lang = LocalLang.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in items.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in row) {
                    val sel = c.id in selected
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onToggle(c) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) PrimaryLight else CardColor,
                        border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) Primary else Border)
                    ) {
                        Column(
                            Modifier.padding(vertical = 12.dp, horizontal = 6.dp).heightIn(min = 76.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(c.emoji, fontSize = 26.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                c.name(lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AreaPickerDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onAny: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var q by remember { mutableStateOf("") }
    val areas = Catalog.areas.filter {
        q.isBlank() || listOf(it.en, it.bn, it.district, it.districtBn).any { s -> s.contains(q.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "বন্ধ করুন")) } },
        title = { Text(tr("Choose area", "এলাকা বাছাই করুন")) },
        text = {
            Column {
                AppTextField(q, { q = it }, tr("Search area or district", "এলাকা বা জেলা খুঁজুন"))
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (onAny != null) {
                        item {
                            Text(
                                tr("Anywhere", "যেকোনো জায়গা"), fontWeight = FontWeight.Bold, color = Primary,
                                modifier = Modifier.fillMaxWidth().clickable { onAny() }.padding(vertical = 12.dp)
                            )
                            HorizontalDivider(color = Border)
                        }
                    }
                    items(areas) { a ->
                        Column(Modifier.fillMaxWidth().clickable { onSelect(a.id) }.padding(vertical = 10.dp)) {
                            Text(a.name(lang), fontWeight = FontWeight.SemiBold)
                            Text(a.districtName(lang), color = Muted, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Border)
                    }
                }
            }
        }
    )
}

@Composable
fun TagChips(
    tags: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    label: @Composable (String) -> String
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in tags) {
            val sel = t in selected
            Surface(
                modifier = Modifier.clickable { onToggle(t) },
                shape = RoundedCornerShape(50),
                color = if (sel) PrimaryLight else CardColor,
                border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) Primary else Border)
            ) {
                Text(
                    label(t), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (sel) Primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) ErrorRed else Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}
