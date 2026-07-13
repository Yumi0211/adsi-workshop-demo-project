package com.example.attendance.leave.repository;

import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<LeaveRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, LeaveStatus status);

    List<LeaveRequest> findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(UUID departmentId, LeaveStatus status);

    boolean existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusIn(
            UUID requesterId, LocalDate leaveDate, LeaveType leaveType, List<LeaveStatus> statuses);
}
