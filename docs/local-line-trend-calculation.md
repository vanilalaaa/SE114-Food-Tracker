# Tài liệu Thuật toán Dự báo Xu hướng Chi tiêu (Trend Forecast)

Tài liệu này giải thích chi tiết về luồng dữ liệu (Flow) và công thức toán học được sử dụng trong component `LocalLineTrendChartCard` để tính toán con số **Dự báo cuối kỳ (Projected Total)**.

---

## 1. Công thức Toán học (Mathematical Formula)

Hệ thống áp dụng phương pháp **Nội suy tuyến tính dựa trên Tiến độ thực tế và Mức chi lịch sử (Linear Extrapolation with Historical Baseline)**.

**Công thức tổng quát:**

> `Projected Total = Current Actual + (Cycle Average * Remaining Cycles)`

Trong đó:
* **Current Actual**: Tổng số tiền người dùng **đã chi tiêu thực tế** trong kỳ hiện tại tính đến thời điểm kiểm tra.
* **Remaining Cycles**: Số chu kỳ con (sub-cycles) còn lại trong kỳ đang xét.
    * Kỳ Tuần (`WEEK`) / Tháng (`MONTH`): Chu kỳ con là **Ngày** (Days).
    * Kỳ Năm (`YEAR`): Chu kỳ con là **Tuần** (Weeks).
    * Kỳ Ngày (`DAY`): Không có chu kỳ con -> Remaining = 0$ (Dự báo bằng đúng thực tế).
* **Cycle Average**: Mức chi tiêu trung bình của **7 chu kỳ con tương ứng trong quá khứ** (được lấy từ tầng Repository qua hàm `getHistoricalCycleAverage`).

---

## 2. Luồng Xử lý Dữ liệu (Data Flow)

Dữ liệu được xử lý reactive thông qua `Kotlin Flow` trong `StatisticsViewModel` theo các bước sau:

```
[User Selects TimeFrame/Date] 
         │
         ▼
[Calculate DateRange & Previous DateRange]
         │
         ▼
[Parallel DB Queries (Room/SQLite)]
 ├── 1. Get current actual spend (currentActual)
 └── 2. Get 7-cycle historical average (cycleAverage)
         │
         ▼
[ViewModel: buildTrendForecast()]
 ├── Compute remaining sub-cycles via TimeRangeProvider
 └── Apply formula: projected = currentActual + (cycleAverage * remaining)
         │
         ▼
[UI Layer: LocalLineTrendChartCard]
 ├── Render Solid Line: Previous Total ───► Current Actual
 └── Render Dashed Line (Pink): Current Actual ˗ ˗ ˗ ► Projected Total

```

---

## 3. Ví dụ Minh họa Thực tế (Case Study)

Dựa trên số liệu thực tế từ màn hình Thống kê Tuần:

* **Tuần trước:** Người dùng tiêu `162K đ`.
* **Tuần này (Hiện tại):** Người dùng mới tiêu `2K đ` (`currentActual = 2000`).
* **Số kỳ con còn lại:** Còn `5 ngày` nữa mới hết tuần (`remaining = 5`).

### Bước tính toán của hệ thống:

1. Từ lịch sử 7 tuần trước, hệ thống tính được trung bình mỗi ngày người dùng này sẽ tiêu khoảng **`23K đ / ngày`** (`cycleAverage = 23000`).
2. Áp dụng công thức dự báo cho 5 ngày còn lại của tuần:

> `Projected Total = 2.000 + (23.000 * 5) = 117.000 đ`

3. **Kết luận:** Mặc dù hiện tại người dùng mới tiêu rất ít (2K), hệ thống dựa vào thói quen trong quá khứ để cảnh báo rằng nếu giữ nguyên tiến độ sinh hoạt cũ, cuối tuần này họ sẽ tiêu chạm mốc **117K đ**.

---

## 4. Ưu điểm & Hạn chế trong Thực tế

### ✅ Ưu điểm (Why this formula?)

* **Tính thời gian thực (Real-time tracking):** Dự báo dịch chuyển linh hoạt theo từng ngày. Nếu hôm nay tiêu nhiều, mốc dự báo sẽ tự động kéo cao lên và ngược lại.
* **Tránh dự báo ngây ngô:** Không dùng "trung bình cộng đơn giản" của các tuần trước (ví dụ: `162K + tuần trước nữa / 2$`), vì cách đó bỏ qua việc tuần này đã đi qua được bao nhiêu ngày, dễ dẫn đến dự báo phi thực tế ở những ngày cuối kỳ.

### ❌ Hạn chế

* Chưa tự động nhận diện được **tính mùa vụ đặc biệt** (chẳng hạn người dùng luôn tiêu đột biến gấp 5 lần vào Thứ 7, Chủ Nhật). Hệ thống đang cào bằng mức chi tiêu trung bình `cycleAverage` cho tất cả các ngày còn lại.

---