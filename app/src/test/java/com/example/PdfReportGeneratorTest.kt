package com.example

import com.example.data.LeaveRequestEntity
import com.example.service.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfReportGeneratorTest {

    @Test
    fun calculateStatistics_computesTotalsCorrectly() {
        val sampleRequests = listOf(
            LeaveRequestEntity(
                id = "1",
                employeeEmail = "khadija@gmail.com",
                employeeName = "khadija el ferrouni",
                department = "Tech / IT",
                leaveType = "Congés payés",
                startDate = "01/10/2026",
                endDate = "05/10/2026",
                startDay = 1,
                endDay = 5,
                month = 9,
                year = 2026,
                daysCount = 4,
                reason = "Vacances",
                status = "APPROVED"
            ),
            LeaveRequestEntity(
                id = "2",
                employeeEmail = "mouad@gmail.com",
                employeeName = "mouad daoudi",
                department = "Tech / IT",
                leaveType = "Congés payés",
                startDate = "10/10/2026",
                endDate = "15/10/2026",
                startDay = 10,
                endDay = 15,
                month = 9,
                year = 2026,
                daysCount = 5,
                reason = "Repos",
                status = "APPROVED"
            ),
            LeaveRequestEntity(
                id = "3",
                employeeEmail = "khadija@gmail.com",
                employeeName = "khadija el ferrouni",
                department = "Tech / IT",
                leaveType = "RTT",
                startDate = "20/10/2026",
                endDate = "22/10/2026",
                startDay = 20,
                endDay = 22,
                month = 9,
                year = 2026,
                daysCount = 2,
                reason = "RTT",
                status = "APPROVED"
            )
        )

        val stats = PdfReportGenerator.calculateStatistics(sampleRequests)

        assertEquals(3, stats.totalRequests)
        assertEquals(11, stats.totalDays)
        assertEquals(2, stats.uniqueEmployeesCount)
        assertEquals(9, stats.byType["Congés payés"])
        assertEquals(2, stats.byType["RTT"])
        assertEquals(11, stats.byDepartment["Tech / IT"])
    }

    @Test
    fun calculateStatistics_handlesEmptyList() {
        val stats = PdfReportGenerator.calculateStatistics(emptyList())

        assertEquals(0, stats.totalRequests)
        assertEquals(0, stats.totalDays)
        assertEquals(0, stats.uniqueEmployeesCount)
        assertTrue(stats.byType.isEmpty())
        assertTrue(stats.byDepartment.isEmpty())
    }
}
