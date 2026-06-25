package com.SE114.food_tracker.feature.admin

import androidx.annotation.StringRes
import com.SE114.food_tracker.R

/**
 * Ban length offered to admins. [seconds] is the temporary-ban window passed to the ban RPC;
 * [PERMANENT] carries null (no expiry). Month/half-year use calendar-approximate day counts.
 */
enum class BanDuration(@StringRes val labelRes: Int, val seconds: Long?) {
    ONE_DAY(R.string.admin_ban_duration_1d, 24L * 60 * 60),
    ONE_WEEK(R.string.admin_ban_duration_1w, 7L * 24 * 60 * 60),
    ONE_MONTH(R.string.admin_ban_duration_1mo, 30L * 24 * 60 * 60),
    SIX_MONTHS(R.string.admin_ban_duration_6mo, 182L * 24 * 60 * 60),
    PERMANENT(R.string.admin_ban_duration_permanent, null)
}
