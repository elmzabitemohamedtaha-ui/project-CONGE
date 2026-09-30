package com.example.util

import com.example.data.CsvParseResult
import com.example.data.LeaveBalanceHistoryCsvRow
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

/**
 * Parser and validator for Supabase `leave_balance_history` CSV imports.
 *
 * Requirements:
 * - Columns: user_id, employee_email, leave_type, action, days_delta, balance_after, reason
 * - user_id: Must be a valid UUID
 * - action: Must be strictly 'accrual', 'deduction' or 'adjustment'
 * - days_delta & balance_after: Must be valid numbers
 * - Support for both ',' and ';' delimiters
 */
object LeaveBalanceCsvParser {

    val ALLOWED_ACTIONS = setOf("accrual", "deduction", "adjustment")
    val VALID_ACTIONS = ALLOWED_ACTIONS

    /**
     * Parses CSV text content into validated [CsvParseResult].
     */
    fun parse(csvContent: String): CsvParseResult {
        if (csvContent.isBlank()) {
            return CsvParseResult(
                totalRows = 0,
                validCount = 0,
                errorCount = 0,
                rows = emptyList(),
                headerErrors = listOf("Le contenu du fichier CSV est vide."),
                rawContent = csvContent
            )
        }

        val lines = csvContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }

        if (lines.isEmpty()) {
            return CsvParseResult(
                totalRows = 0,
                validCount = 0,
                errorCount = 0,
                rows = emptyList(),
                headerErrors = listOf("Aucune ligne de données trouvée dans le CSV."),
                rawContent = csvContent
            )
        }

        // Detect delimiter (comma or semicolon) from first line
        val firstLine = lines.first()
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

        // Parse header
        val headerTokens = parseCsvLine(firstLine, delimiter).map { it.trim().lowercase() }
        val isHeaderPresent = headerTokens.any { it.contains("user_id") || it.contains("email") || it.contains("action") }

        val columnMapping: Map<String, Int>
        val dataLines: List<String>
        val startLineNumber: Int

        if (isHeaderPresent) {
            columnMapping = mapHeaderColumns(headerTokens)
            dataLines = lines.drop(1)
            startLineNumber = 2
        } else {
            // Default expected column order
            columnMapping = mapOf(
                "user_id" to 0,
                "employee_email" to 1,
                "leave_type" to 2,
                "action" to 3,
                "days_delta" to 4,
                "balance_after" to 5,
                "reason" to 6
            )
            dataLines = lines
            startLineNumber = 1
        }

        val headerErrors = mutableListOf<String>()
        val requiredCols = listOf("user_id", "employee_email", "action", "days_delta", "balance_after")
        for (col in requiredCols) {
            if (!columnMapping.containsKey(col)) {
                headerErrors.add("Colonne obligatoire manquante dans l'en-tête CSV : '$col'")
            }
        }

        val rows = mutableListOf<LeaveBalanceHistoryCsvRow>()

        dataLines.forEachIndexed { index, line ->
            val lineNumber = startLineNumber + index
            val tokens = parseCsvLine(line, delimiter)
            val parsedRow = validateAndBuildRow(lineNumber, tokens, columnMapping)
            rows.add(parsedRow)
        }

        val validCount = rows.count { it.isValid }
        val errorCount = rows.count { !it.isValid }

        return CsvParseResult(
            totalRows = rows.size,
            validCount = validCount,
            errorCount = errorCount,
            rows = rows,
            headerErrors = headerErrors,
            rawContent = csvContent
        )
    }

    /**
     * Parses an [InputStream] (e.g. from an Android URI content resolver).
     */
    fun parse(inputStream: InputStream): CsvParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.use { it.readText() }
        return parse(content)
    }

    private fun mapHeaderColumns(headers: List<String>): Map<String, Int> {
        val mapping = mutableMapOf<String, Int>()
        headers.forEachIndexed { index, h ->
            val clean = h.trim().lowercase().replace("\"", "").replace("'", "")
            when {
                clean == "user_id" || clean == "userid" || clean == "id_utilisateur" -> mapping["user_id"] = index
                clean == "employee_email" || clean == "email" || clean == "user_email" -> mapping["employee_email"] = index
                clean == "leave_type" || clean == "type" || clean == "leavetype" -> mapping["leave_type"] = index
                clean == "action" || clean == "type_action" -> mapping["action"] = index
                clean == "days_delta" || clean == "daysdelta" || clean == "delta" || clean == "jours" -> mapping["days_delta"] = index
                clean == "balance_after" || clean == "balanceafter" || clean == "nouveau_solde" -> mapping["balance_after"] = index
                clean == "reason" || clean == "motif" || clean == "commentaire" -> mapping["reason"] = index
            }
        }
        return mapping
    }

    private fun validateAndBuildRow(
        lineNumber: Int,
        tokens: List<String>,
        mapping: Map<String, Int>
    ): LeaveBalanceHistoryCsvRow {
        val errors = mutableListOf<String>()

        fun getToken(key: String, defaultIndex: Int): String {
            val idx = mapping[key] ?: defaultIndex
            return if (idx in tokens.indices) {
                tokens[idx].trim().removeSurrounding("\"").removeSurrounding("'").trim()
            } else {
                ""
            }
        }

        val rawUserId = getToken("user_id", 0)
        val rawEmail = getToken("employee_email", 1)
        val rawLeaveType = getToken("leave_type", 2)
        val rawAction = getToken("action", 3)
        val rawDaysDelta = getToken("days_delta", 4)
        val rawBalanceAfter = getToken("balance_after", 5)
        val rawReason = getToken("reason", 6)

        // 1. Validate user_id as a valid UUID
        if (rawUserId.isBlank()) {
            errors.add("user_id est requis (doit être un UUID)")
        } else if (!isValidUuid(rawUserId)) {
            errors.add("user_id '$rawUserId' n'est pas un UUID valide (format attendu : 8-4-4-4-12 hexadécimal, ex: 123e4567-e89b-12d3-a456-426614174000)")
        }

        // 2. Validate employee_email
        if (rawEmail.isBlank()) {
            errors.add("employee_email est requis")
        } else if (!rawEmail.contains("@") || !rawEmail.contains(".")) {
            errors.add("employee_email '$rawEmail' est un format d'adresse email non valide")
        }

        // 3. Validate action: ONLY 'accrual', 'deduction' or 'adjustment'
        val cleanAction = rawAction.lowercase().trim()
        if (cleanAction.isBlank()) {
            errors.add("action est requise (doit être 'accrual', 'deduction' ou 'adjustment')")
        } else if (cleanAction !in VALID_ACTIONS) {
            errors.add("action '$rawAction' invalide : doit être strictement 'accrual', 'deduction' ou 'adjustment'")
        }

        // 4. Validate days_delta as number
        val cleanDaysStr = rawDaysDelta.replace(',', '.').trim()
        val daysDeltaNum = cleanDaysStr.toDoubleOrNull()
        if (rawDaysDelta.isBlank()) {
            errors.add("days_delta est requis")
        } else if (daysDeltaNum == null) {
            errors.add("days_delta '$rawDaysDelta' doit être un nombre valide (ex: 21, -2, 1.5)")
        }

        // 5. Validate balance_after as number
        val cleanBalanceStr = rawBalanceAfter.replace(',', '.').trim()
        val balanceAfterNum = cleanBalanceStr.toDoubleOrNull()
        if (rawBalanceAfter.isBlank()) {
            errors.add("balance_after est requis")
        } else if (balanceAfterNum == null) {
            errors.add("balance_after '$rawBalanceAfter' doit être un nombre valide (ex: 21, 19, 10.5)")
        }

        val leaveType = if (rawLeaveType.isNotBlank()) rawLeaveType else "annual"
        val reason = if (rawReason.isNotBlank()) rawReason else "Ajustement de solde"

        return LeaveBalanceHistoryCsvRow(
            lineNumber = lineNumber,
            userId = rawUserId,
            employeeEmail = rawEmail,
            leaveType = leaveType,
            action = if (cleanAction.isNotBlank()) cleanAction else "adjustment",
            daysDelta = daysDeltaNum ?: 0.0,
            balanceAfter = balanceAfterNum ?: 0.0,
            reason = reason,
            rawDaysDelta = rawDaysDelta,
            rawBalanceAfter = rawBalanceAfter,
            isValid = errors.isEmpty(),
            validationErrors = errors
        )
    }

    /**
     * Checks if a string conforms to the RFC 4122 UUID specification.
     */
    fun isValidUuid(str: String): Boolean {
        val trimmed = str.trim()
        if (trimmed.length != 36) return false
        return try {
            val parsed = UUID.fromString(trimmed)
            parsed.toString().equals(trimmed, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parses a single CSV line into tokens, respecting quotes.
     */
    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var insideQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> {
                    insideQuotes = !insideQuotes
                }
                c == delimiter && !insideQuotes -> {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
                else -> {
                    currentToken.append(c)
                }
            }
        }
        tokens.add(currentToken.toString())
        return tokens
    }

    /**
     * Returns standard valid sample CSV as described in the specification.
     */
    fun getSampleCsv(defaultUuid: String = "c7b84b90-7f2e-48a5-9653-53d9e8674900"): String {
        return buildString {
            append("user_id,employee_email,leave_type,action,days_delta,balance_after,reason\n")
            append("$defaultUuid,employe@example.com,annual,accrual,21,21,Solde annuel initial\n")
            append("$defaultUuid,employe@example.com,annual,deduction,-2,19,Demande de congé approuvée\n")
            append("$defaultUuid,employe@example.com,rtt,adjustment,5,5,Attribution RTT forfaitaire\n")
            append("$defaultUuid,employe2@example.com,annual,deduction,-1,18,Congé imprévu approuvé")
        }
    }

    /**
     * Returns a sample CSV with deliberate errors to test UI error detection & display.
     */
    fun getSampleWithErrorsCsv(): String {
        return buildString {
            append("user_id,employee_email,leave_type,action,days_delta,balance_after,reason\n")
            append("c7b84b90-7f2e-48a5-9653-53d9e8674900,employe@example.com,annual,accrual,21,21,Solde annuel initial\n")
            append("UUID_UTILISATEUR_INVALIDE,employe@example.com,annual,deduction,-2,19,Erreur UUID non valide\n")
            append("c7b84b90-7f2e-48a5-9653-53d9e8674900,email_invalide,annual,bonus,dix,inconnu,Erreur action + nombres\n")
            append("c7b84b90-7f2e-48a5-9653-53d9e8674900,employe2@example.com,annual,adjustment,3,22,Ajustement exceptionnel")
        }
    }
}
