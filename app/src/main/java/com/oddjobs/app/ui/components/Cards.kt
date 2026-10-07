@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.oddjobs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oddjobs.app.domain.AppStatus
import com.oddjobs.app.domain.Application
import com.oddjobs.app.domain.Catalog
import com.oddjobs.app.domain.Job
import com.oddjobs.app.domain.RankedSeeker
import com.oddjobs.app.domain.Ranking
import com.oddjobs.app.domain.User
import com.oddjobs.app.ui.LocalLang
import com.oddjobs.app.ui.ago
import com.oddjobs.app.ui.digitsFor
import com.oddjobs.app.ui.money
import com.oddjobs.app.ui.num
import com.oddjobs.app.ui.payLabel
import com.oddjobs.app.ui.rating
import com.oddjobs.app.ui.tr
import com.oddjobs.app.ui.urgencyLabel
import com.oddjobs.app.ui.theme.Accent
import com.oddjobs.app.ui.theme.AccentLight
import com.oddjobs.app.ui.theme.Border
import com.oddjobs.app.ui.theme.Muted
import com.oddjobs.app.ui.theme.Primary
import com.oddjobs.app.ui.theme.PrimaryLight
import com.oddjobs.app.ui.theme.Success
import com.oddjobs.app.ui.theme.SuccessBg
import com.oddjobs.app.ui.theme.Warn
import com.oddjobs.app.ui.theme.WarnBg

@Composable
fun km(d: Double): String {
    val s = if (d < 1.0) tr("< 1 km", "১ কিমির কম") else digitsFor(LocalLang.current, String.format(java.util.Locale.US, "%.1f", d)) + " " + tr("km", "কিমি")
    return s
}

@Composable
fun JobCard(
    job: Job,
    client: User?,
    distanceKm: Double?,
    now: Long,
    onClick: () -> Unit,
    saved: Boolean? = null,
    onSave: (() -> Unit)? = null,
    badge: String? = null,
    badgeGood: Boolean = true
) {
    val lang = LocalLang.current
    val cat = Catalog.category(job.categoryId)
    val area = Catalog.area(job.areaId)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(46.dp).background(PrimaryLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(cat.emoji, fontSize = 24.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    job.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        area.full(lang) + if (distanceKm != null) " · " + km(distanceKm) else "",
                        color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(job.budget), fontWeight = FontWeight.ExtraBold, color = Primary, fontSize = 17.sp)
                if (job.negotiable) Text(tr("negotiable", "আলোচনা সাপেক্ষ"), color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(cat.name(lang))
            Pill(urgencyLabel(job.urgency), AccentLight, Accent)
            Pill(payLabel(job.payMethod), SuccessBg, Success)
            if (job.onlyVerified) Pill(tr("Verified only", "যাচাইকৃত কর্মী"), WarnBg, Warn)
            if (badge != null) Pill(badge, if (badgeGood) PrimaryLight else WarnBg, if (badgeGood) Primary else Warn)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Border)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (client != null) {
                Avatar(client.name, client.photoPath, 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(client.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
            }
            Text(ago(job.createdAt, now), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (job.applications.isNotEmpty()) {
                Text(
                    tr("${job.applications.count { it.status != AppStatus.WITHDRAWN }} offers", "${digitsFor(lang, job.applications.count { it.status != AppStatus.WITHDRAWN }.toString())} টি অফার"),
                    color = Muted, fontSize = 12.sp
                )
            }
            if (saved != null && onSave != null) {
                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = tr("Save", "সংরক্ষণ"),
                        tint = if (saved) Accent else Muted
                    )
                }
            }
        }
    }
}

@Composable
fun SeekerCard(
    r: RankedSeeker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    onFavorite: (() -> Unit)? = null,
    offer: Application? = null,
    actions: @Composable () -> Unit = {}
) {
    val lang = LocalLang.current
    val u = r.user
    val s = r.stats
    AppCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(u.name, u.photoPath, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(u.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    VerifiedBadge(u.level)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.ratingCount > 0) {
                        RatingStars(s.ratingAvg, 14.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(rating(s.ratingAvg) + " (" + num(s.ratingCount) + ")", fontSize = 12.sp, color = Muted)
                    } else {
                        Pill(tr("New", "নতুন"), AccentLight, Accent)
                    }
                }
                Text(
                    u.categories.joinToString(" · ") { Catalog.category(it).name(lang) },
                    color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (favorite != null && onFavorite != null) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null, tint = if (favorite) Accent else Muted
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(tr("Trust ", "ট্রাস্ট ") + num(Math.round(r.trust).toInt()), PrimaryLight, Primary)
            if (r.trust >= 75 && s.ratingCount >= 5) Pill(tr("Top rated", "শীর্ষ রেটিং"), AccentLight, Accent)
            if (s.medianResponseMin in 1..10) Pill(tr("Replies fast", "দ্রুত উত্তর দেয়"), SuccessBg, Success)
            if (s.completed > 0) Pill(tr("${s.completed} jobs done", "${digitsFor(lang, s.completed.toString())} টি কাজ সম্পন্ন"))
            if (r.distanceKm > 0) Pill(km(r.distanceKm), WarnBg, Warn)
        }
        Spacer(Modifier.height(8.dp))
        if (offer != null) {
            Text(
                tr("Offer: ", "অফার: ") + money(offer.price) + " · " +
                    tr("arrives in ${offer.etaMin} min", "${digitsFor(lang, offer.etaMin.toString())} মিনিটে পৌঁছাবে"),
                fontWeight = FontWeight.Bold, color = Primary
            )
            if (offer.message.isNotBlank()) Text(offer.message, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            Text(
                money(u.rateMin) + " – " + money(u.rateMax) + " " + tr("per job", "প্রতি কাজ"),
                fontWeight = FontWeight.SemiBold
            )
        }
        actions()
    }
}
