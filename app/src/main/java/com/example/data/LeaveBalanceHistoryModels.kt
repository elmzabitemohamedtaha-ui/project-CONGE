package com.example.data

/**
 * Data models for Supabase `leave_balance_history` CSV import & tracking.
 *
 * Supported CSV format:
 * user_id,employee_email,leave_type,action,days_delta,balance_after,reason
 *
 * Action values: 'accrual', 'deduction', 'adjustment'
 */
data class LeaveBalanceHistoryCsvRow(
    val lineNumber: Int,
    val userId: String,
    val employeeEmail: String,
    val leaveType: String,
    val action: String,
    val daysDelta: Double,
    val balanceAfter: Double,
    val reason: String,
    val rawDaysDelta: String = "",
    val rawBalanceAfter: String = "",
    val isValid: Boolean = true,
    val validationErrors: List<String> = emptyList()
)

/**
 * Summary result after parsing a CSV file or text content.
 */
data class CsvParseResult(
    val totalRows: Int,
    val validCount: Int,
    val errorCount: Int,
    val rows: List<LeaveBalanceHistoryCsvRow>,
    val headerErrors: List<String> = emptyList(),
    val rawContent: String = ""
) {
    val isValid: Boolean get() = headerErrors.isEmpty() && validCount > 0
    val validRows: List<LeaveBalanceHistoryCsvRow> get() = rows.filter { it.isValid }
    val errorRows: List<LeaveBalanceHistoryCsvRow> get() = rows.filter { !it.isValid }
}

/**
 * Result returned by Supabase after performing the import.
 */
sealed class SupabaseImportResult {
    data class Success(
        val importedCount: Int,
        val message: String
    ) : SupabaseImportResult()

    data class Error(
        val message: String,
        val details: String? = null
    ) : SupabaseImportResult()
}
