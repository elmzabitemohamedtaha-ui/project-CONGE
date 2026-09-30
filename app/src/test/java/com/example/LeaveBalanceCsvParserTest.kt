package com.example

import com.example.util.LeaveBalanceCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LeaveBalanceCsvParserTest {

    @Test
    fun testParseValidCsvWithExampleData() {
        val validUuid1 = UUID.randomUUID().toString()
        val validUuid2 = UUID.randomUUID().toString()

        val csv = """
            user_id,employee_email,leave_type,action,days_delta,balance_after,reason
            $validUuid1,employe@example.com,annual,accrual,21,21,Solde annuel initial
            $validUuid2,employe@example.com,annual,deduction,-2,19,Demande de congé approuvée
        """.trimIndent()

        val result = LeaveBalanceCsvParser.parse(csv)

        assertEquals(2, result.totalRows)
        assertEquals(2, result.validCount)
        assertEquals(0, result.errorCount)
        assertTrue(result.headerErrors.isEmpty())

        val row1 = result.rows[0]
        assertEquals(validUuid1, row1.userId)
        assertEquals("employe@example.com", row1.employeeEmail)
        assertEquals("annual", row1.leaveType)
        assertEquals("accrual", row1.action)
        assertEquals(21.0, row1.daysDelta, 0.001)
        assertEquals(21.0, row1.balanceAfter, 0.001)
        assertEquals("Solde annuel initial", row1.reason)
        assertTrue(row1.isValid)

        val row2 = result.rows[1]
        assertEquals(validUuid2, row2.userId)
        assertEquals("employe@example.com", row2.employeeEmail)
        assertEquals("annual", row2.leaveType)
        assertEquals("deduction", row2.action)
        assertEquals(-2.0, row2.daysDelta, 0.001)
        assertEquals(19.0, row2.balanceAfter, 0.001)
        assertEquals("Demande de congé approuvée", row2.reason)
        assertTrue(row2.isValid)
    }

    @Test
    fun testParseSemicolonDelimiter() {
        val validUuid = UUID.randomUUID().toString()
        val csv = """
            user_id;employee_email;leave_type;action;days_delta;balance_after;reason
            $validUuid;collab@example.com;rtt;adjustment;1.5;15.5;Ajustement RH
        """.trimIndent()

        val result = LeaveBalanceCsvParser.parse(csv)
        assertEquals(1, result.totalRows)
        assertEquals(1, result.validCount)
        assertEquals(0, result.errorCount)
        assertEquals("adjustment", result.rows[0].action)
        assertEquals(1.5, result.rows[0].daysDelta, 0.001)
        assertEquals(15.5, result.rows[0].balanceAfter, 0.001)
    }

    @Test
    fun testDetectErrorsInCsvRows() {
        val validUuid = UUID.randomUUID().toString()
        val csv = """
            user_id,employee_email,leave_type,action,days_delta,balance_after,reason
            $validUuid,employe@example.com,annual,accrual,21,21,Valide
            NOT_A_VALID_UUID,test@example.com,annual,accrual,10,10,UUID invalide
            $validUuid,test2@example.com,annual,unsupported_action,5,15,Action invalide
            $validUuid,test3@example.com,annual,deduction,not_a_number,10,Delta invalide
            $validUuid,test4@example.com,annual,adjustment,5,invalid_number,Solde invalide
        """.trimIndent()

        val result = LeaveBalanceCsvParser.parse(csv)
        assertEquals(5, result.totalRows)
        assertEquals(1, result.validCount)
        assertEquals(4, result.errorCount)

        // Row 1 is valid
        assertTrue(result.rows[0].isValid)

        // Row 2: invalid UUID
        assertFalse(result.rows[1].isValid)
        assertTrue(result.rows[1].validationErrors.any { it.contains("user_id") })

        // Row 3: invalid action
        assertFalse(result.rows[2].isValid)
        assertTrue(result.rows[2].validationErrors.any { it.contains("action") })

        // Row 4: invalid days_delta
        assertFalse(result.rows[3].isValid)
        assertTrue(result.rows[3].validationErrors.any { it.contains("days_delta") })

        // Row 5: invalid balance_after
        assertFalse(result.rows[4].isValid)
        assertTrue(result.rows[4].validationErrors.any { it.contains("balance_after") })
    }

    @Test
    fun testAllowedActions() {
        assertTrue(LeaveBalanceCsvParser.ALLOWED_ACTIONS.contains("accrual"))
        assertTrue(LeaveBalanceCsvParser.ALLOWED_ACTIONS.contains("deduction"))
        assertTrue(LeaveBalanceCsvParser.ALLOWED_ACTIONS.contains("adjustment"))
        assertEquals(3, LeaveBalanceCsvParser.ALLOWED_ACTIONS.size)
    }

    @Test
    fun testUuidValidationHelper() {
        assertTrue(LeaveBalanceCsvParser.isValidUuid("123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(LeaveBalanceCsvParser.isValidUuid(UUID.randomUUID().toString()))
        assertFalse(LeaveBalanceCsvParser.isValidUuid("invalid-uuid"))
        assertFalse(LeaveBalanceCsvParser.isValidUuid(""))
        assertFalse(LeaveBalanceCsvParser.isValidUuid("123e4567-e89b-12d3-a456-42661417400Z"))
    }
}
