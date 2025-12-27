package io.clroot.selah.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse? = null,
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(true, data, null)

        fun <T> error(error: ErrorResponse) = ApiResponse<T>(false, null, error)
    }
}
