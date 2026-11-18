package vn.tunglv.browser.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/** Một tỉnh/thành để chọn tay khi không dùng vị trí của máy. */
data class Province(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** 63 tỉnh/thành, tọa độ lấy gần trung tâm để tra thời tiết. */
val PROVINCES = listOf(
    Province("an-giang", "An Giang", 10.52, 105.12),
    Province("ba-ria-vung-tau", "Bà Rịa - Vũng Tàu", 10.54, 107.24),
    Province("bac-lieu", "Bạc Liêu", 9.29, 105.72),
    Province("bac-giang", "Bắc Giang", 21.27, 106.19),
    Province("bac-kan", "Bắc Kạn", 22.15, 105.83),
    Province("bac-ninh", "Bắc Ninh", 21.12, 106.08),
    Province("ben-tre", "Bến Tre", 10.24, 106.38),
    Province("binh-duong", "Bình Dương", 11.33, 106.48),
    Province("binh-dinh", "Bình Định", 13.78, 109.22),
    Province("binh-phuoc", "Bình Phước", 11.75, 106.93),
    Province("binh-thuan", "Bình Thuận", 11.09, 108.11),
    Province("ca-mau", "Cà Mau", 9.18, 105.15),
    Province("can-tho", "Cần Thơ", 10.05, 105.75),
    Province("cao-bang", "Cao Bằng", 22.67, 106.26),
    Province("da-nang", "Đà Nẵng", 16.05, 108.21),
    Province("dak-lak", "Đắk Lắk", 12.71, 108.04),
    Province("dak-nong", "Đắk Nông", 12.26, 107.61),
    Province("dien-bien", "Điện Biên", 21.39, 103.02),
    Province("dong-nai", "Đồng Nai", 11.07, 107.05),
    Province("dong-thap", "Đồng Tháp", 10.47, 105.69),
    Province("gia-lai", "Gia Lai", 13.98, 108.00),
    Province("ha-giang", "Hà Giang", 22.82, 104.98),
    Province("ha-nam", "Hà Nam", 20.58, 105.88),
    Province("ha-noi", "Hà Nội", 21.03, 105.85),
    Province("ha-tinh", "Hà Tĩnh", 18.34, 105.91),
    Province("hai-duong", "Hải Dương", 20.94, 106.33),
    Province("hai-phong", "Hải Phòng", 20.85, 106.69),
    Province("hau-giang", "Hậu Giang", 9.76, 105.64),
    Province("hoa-binh", "Hòa Bình", 20.82, 105.34),
    Province("hung-yen", "Hưng Yên", 20.85, 106.09),
    Province("khanh-hoa", "Khánh Hòa", 12.26, 109.19),
    Province("kien-giang", "Kiên Giang", 10.01, 105.08),
    Province("kon-tum", "Kon Tum", 14.35, 108.00),
    Province("lai-chau", "Lai Châu", 22.39, 103.16),
    Province("lam-dong", "Lâm Đồng", 11.94, 108.46),
    Province("lang-son", "Lạng Sơn", 21.85, 106.76),
    Province("lao-cai", "Lào Cai", 22.48, 103.98),
    Province("long-an", "Long An", 10.70, 106.42),
    Province("nam-dinh", "Nam Định", 20.42, 106.17),
    Province("nghe-an", "Nghệ An", 19.23, 104.92),
    Province("ninh-binh", "Ninh Bình", 20.25, 105.97),
    Province("ninh-thuan", "Ninh Thuận", 11.57, 108.99),
    Province("phu-tho", "Phú Thọ", 21.34, 105.37),
    Province("phu-yen", "Phú Yên", 13.09, 109.30),
    Province("quang-binh", "Quảng Bình", 17.47, 106.60),
    Province("quang-nam", "Quảng Nam", 15.54, 108.19),
    Province("quang-ngai", "Quảng Ngãi", 15.12, 108.80),
    Province("quang-ninh", "Quảng Ninh", 20.95, 107.08),
    Province("quang-tri", "Quảng Trị", 16.75, 107.19),
    Province("soc-trang", "Sóc Trăng", 9.60, 105.98),
    Province("son-la", "Sơn La", 21.33, 103.92),
    Province("tay-ninh", "Tây Ninh", 11.34, 106.13),
    Province("thai-binh", "Thái Bình", 20.45, 106.34),
    Province("thai-nguyen", "Thái Nguyên", 21.59, 105.85),
    Province("thanh-hoa", "Thanh Hóa", 19.81, 105.78),
    Province("thua-thien-hue", "Thừa Thiên Huế", 16.46, 107.58),
    Province("tien-giang", "Tiền Giang", 10.45, 106.34),
    Province("tp-ho-chi-minh", "TP. Hồ Chí Minh", 10.82, 106.63),
    Province("tra-vinh", "Trà Vinh", 9.95, 106.34),
    Province("tuyen-quang", "Tuyên Quang", 21.82, 105.22),
    Province("vinh-long", "Vĩnh Long", 10.25, 105.97),
    Province("vinh-phuc", "Vĩnh Phúc", 21.36, 105.55),
    Province("yen-bai", "Yên Bái", 21.72, 104.87),
)

fun provinceById(id: String): Province? = PROVINCES.firstOrNull { it.id == id }

/** Thời tiết hiện tại tại một vị trí. */
data class WeatherInfo(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val placeName: String,
    val fetchedAt: Long,
)

/** Mô tả tiếng Việt cho mã thời tiết WMO của Open-Meteo. */
fun weatherDescription(code: Int): String = when (code) {
    0 -> "Trời quang"
    1 -> "Gần như quang"
    2 -> "Ít mây"
    3 -> "Nhiều mây"
    45, 48 -> "Sương mù"
    51, 53, 55 -> "Mưa phùn"
    56, 57 -> "Mưa phùn đóng băng"
    61 -> "Mưa nhẹ"
    63 -> "Mưa vừa"
    65 -> "Mưa to"
    66, 67 -> "Mưa đóng băng"
    71, 73, 75, 77 -> "Tuyết"
    80, 81, 82 -> "Mưa rào"
    85, 86 -> "Mưa tuyết"
    95 -> "Dông"
    96, 99 -> "Dông kèm mưa đá"
    else -> "Không rõ"
}

/** Vị trí đã xác định kèm tên gọi để hiển thị. */
data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val placeName: String,
)

/**
 * Lấy vị trí xấp xỉ của máy: thử vị trí lưu sẵn trước, không có thì xin
 * một lần cập nhật rồi thôi. Trả về null khi chưa cấp quyền hoặc không
 * lấy được.
 */
suspend fun getDeviceLocation(context: Context): ResolvedLocation? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    return withContext(Dispatchers.IO) {
        try {
            var best: Location? = null
            for (provider in manager.getProviders(true)) {
                try {
                    val last = manager.getLastKnownLocation(provider) ?: continue
                    if (best == null || last.time > best.time) best = last
                } catch (_: SecurityException) {
                    // Bỏ qua nguồn vị trí không được phép đọc.
                }
            }
            val found = best ?: withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            manager.removeUpdates(this)
                            if (cont.isActive) cont.resume(location)
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                        override fun onProviderEnabled(provider: String) {}

                        override fun onProviderDisabled(provider: String) {}
                    }
                    cont.invokeOnCancellation { manager.removeUpdates(listener) }
                    try {
                        val provider = when {
                            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                                LocationManager.NETWORK_PROVIDER
                            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                                LocationManager.GPS_PROVIDER
                            else -> null
                        }
                        if (provider == null) {
                            if (cont.isActive) cont.resume(null)
                        } else {
                            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        }
                    } catch (_: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            } ?: return@withContext null
            ResolvedLocation(
                found.latitude,
                found.longitude,
                resolvePlaceName(context, found.latitude, found.longitude),
            )
        } catch (_: Exception) {
            null
        }
    }
}

/** Tên khu vực từ tọa độ, dùng Geocoder của máy. Lỗi thì trả tên chung. */
@Suppress("DEPRECATION")
private fun resolvePlaceName(context: Context, latitude: Double, longitude: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale("vi"))
        val first = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        val locality = first?.locality
        val admin = first?.adminArea
        when {
            !locality.isNullOrBlank() && !admin.isNullOrBlank() && locality != admin ->
                "$locality, $admin"
            !locality.isNullOrBlank() -> locality
            !admin.isNullOrBlank() -> admin
            else -> "Vị trí của bạn"
        }
    } catch (_: Exception) {
        "Vị trí của bạn"
    }
}

/**
 * Lấy thời tiết hiện tại từ Open-Meteo (miễn phí, không cần khóa API).
 * Trả về null khi không có mạng hoặc dịch vụ lỗi.
 */
suspend fun fetchWeather(latitude: Double, longitude: Double, placeName: String): WeatherInfo? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&timezone=auto",
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
            WeatherInfo(
                temperature = current.optDouble("temperature_2m", Double.NaN),
                feelsLike = current.optDouble("apparent_temperature", Double.NaN),
                humidity = current.optInt("relative_humidity_2m", -1),
                windSpeed = current.optDouble("wind_speed_10m", Double.NaN),
                weatherCode = current.optInt("weather_code", -1),
                placeName = placeName,
                fetchedAt = System.currentTimeMillis(),
            ).takeIf { !it.temperature.isNaN() && it.weatherCode >= 0 }
        } catch (_: Exception) {
            null
        }
    }
}

/** Trạng thái khối thời tiết trên trang chủ. */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Ready(val info: WeatherInfo) : WeatherUiState

    /** Không lấy được thời tiết (mất mạng, dịch vụ lỗi): khối giờ vẫn hiện. */
    data object Unavailable : WeatherUiState
}

/**
 * Giữ trạng thái thời tiết cho trang chủ, tránh gọi API lại mỗi lần vẽ lại.
 * Làm mới tối đa mỗi 10 phút, trừ khi người dùng bấm làm mới.
 */
object WeatherRepository {

    private const val MIN_REFRESH_MS = 10 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    @Volatile
    private var lastKey: String? = null

    @Volatile
    private var lastFetchedAt: Long = 0L

    fun load(latitude: Double, longitude: Double, placeName: String, force: Boolean = false) {
        val key = "$latitude,$longitude"
        val now = System.currentTimeMillis()
        if (!force && key == lastKey && now - lastFetchedAt < MIN_REFRESH_MS &&
            _state.value is WeatherUiState.Ready
        ) return
        lastKey = key
        scope.launch {
            if (_state.value !is WeatherUiState.Ready) _state.value = WeatherUiState.Loading
            val info = fetchWeather(latitude, longitude, placeName)
            if (info != null) {
                lastFetchedAt = System.currentTimeMillis()
                _state.value = WeatherUiState.Ready(info)
            } else if (_state.value !is WeatherUiState.Ready) {
                _state.value = WeatherUiState.Unavailable
            }
        }
    }
}
