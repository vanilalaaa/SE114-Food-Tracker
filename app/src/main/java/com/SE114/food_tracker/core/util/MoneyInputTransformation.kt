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
                var commas = 0
                // Đếm số dấu phẩy được thêm vào trước vị trí offset gốc
                for (i in 0 until offset) {
                    // Cứ mỗi 3 chữ số tính từ phải qua trái (trừ chữ số đầu tiên) sẽ xuất hiện 1 dấu phẩy
                    val digitsBeforeEnd = rawText.length - i
                    if (digitsBeforeEnd > 3 && (digitsBeforeEnd - 1) % 3 == 0) {
                        commas++
                    }
                }
                // Vị trí mới = Vị trí cũ + Số lượng dấu phẩy xuất hiện phía trước nó
                val transformedOffset = offset + (formatted.length - rawText.length)
                return transformedOffset.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var commas = 0
                val originalTextLength = rawText.length

                // Thuật toán quét ngược từ chuỗi đã định dạng về chuỗi thô ban đầu
                for (i in 0 until offset) {
                    if (i < formatted.length && formatted[i] == ',') {
                        commas++
                    }
                }
                val originalOffset = offset - commas
                return originalOffset.coerceIn(0, originalTextLength)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}