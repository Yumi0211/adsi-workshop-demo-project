package com.example.attendance.leave.controller;

import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveRejectRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.dto.LeaveSummaryResponse;
import com.example.attendance.leave.dto.PendingLeaveResponse;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(
            @RequestParam UUID requesterId,
            @Valid @RequestBody LeaveCreateRequest request) {
        return leaveService.create(requesterId, request);
    }

    @GetMapping
    public List<LeaveResponse> findByRequester(
            @RequestParam UUID requesterId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Integer fiscalYear) {
        return leaveService.findByRequester(requesterId, status, fiscalYear);
    }

    @GetMapping("/balance")
    public LeaveBalanceResponse getBalance(
            @RequestParam UUID employeeId,
            @RequestParam(required = false) Integer fiscalYear) {
        return leaveService.getBalance(employeeId, fiscalYear);
    }

    @GetMapping("/pending")
    public List<PendingLeaveResponse> findPending(@RequestParam UUID managerId) {
        return leaveService.findPending(managerId);
    }

    @PatchMapping("/{id}/approve")
    public LeaveResponse approve(
            @PathVariable UUID id,
            @RequestParam UUID approverId) {
        return leaveService.approve(id, approverId);
    }

    @PatchMapping("/{id}/reject")
    public LeaveResponse reject(
            @PathVariable UUID id,
            @RequestParam UUID approverId,
            @Valid @RequestBody LeaveRejectRequest request) {
        return leaveService.reject(id, approverId, request.rejectReason(), request.version());
    }

    @GetMapping("/summary")
    public List<LeaveSummaryResponse> getSummary(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) UUID departmentId) {
        return leaveService.getSummary(fiscalYear, departmentId);
    }
}
