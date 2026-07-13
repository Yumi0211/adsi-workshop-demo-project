package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByEmployeeIdAndFiscalYear(UUID employeeId, int fiscalYear);

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    List<LeaveBalance> findByFiscalYear(int fiscalYear);

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    List<LeaveBalance> findByEmployeeDepartmentIdAndFiscalYear(UUID departmentId, int fiscalYear);
}
