package com.SE114.food_tracker.core.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * Tự động thêm dấu phẩy phân tách hàng nghìn khi người dùng gõ số vào TextField.
 * Giá trị gốc bên trong `value` vẫn chỉ là chuỗi số thuần túy (digits only).
 */
class MoneyInputTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        if (rawText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Chuyển chuỗi số thành Long để định dạng theo chuẩn Locale.US (dùng dấu phẩy)
        val parsed = rawText.toLongOrNull() ?: 0L
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)

        // Ánh xạ vị trí con trỏ chuột (Cursor Offset Mapping) để tránh lỗi trỏ chuột nhảy lung tung khi có dấu phẩy xuất hiện
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                // Đếm xem có bao nhiêu ký tự số từ đầu chuỗi gốc đến vị trí offset hiện tại
                val digitsBeforeOffset = rawText.take(offset).length
                var digitCount = 0
                // Tìm vị trí tương ứng trong chuỗi đã định dạng chứa đúng bấy nhiêu chữ số
                formatted.forEachIndexed { index, char ->
                    if (char.isDigit()) digitCount++
                    if (digitCount == digitsBeforeOffset) return index + 1
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                // Đếm số lượng chữ số xuất hiện trong chuỗi định dạng từ đầu đến vị trí offset
                val digitsBeforeOffset = formatted.take(offset).count { it.isDigit() }
                return digitsBeforeOffset.coerceIn(0, rawText.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}