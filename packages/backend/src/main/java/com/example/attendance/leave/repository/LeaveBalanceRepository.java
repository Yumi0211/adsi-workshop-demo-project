package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByEmployeeIdAndFiscalYear(UUID employeeId, int fiscalYear);

    List<LeaveBalance> findByFiscalYear(int fiscalYear);

    List<LeaveBalance> findByEmployeeDepartmentIdAndFiscalYear(UUID departmentId, int fiscalYear);
}
