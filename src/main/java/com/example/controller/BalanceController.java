package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dto.BalanceDTO;
import com.example.dto.BalanceUpdateResponseDTO;
import com.example.entity.Balance;
import com.example.service.BalanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/balance")
@CrossOrigin(origins = "*")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    // Get Available Balance
    @GetMapping
    public ResponseEntity<Balance> getAvailableBalance() {
        return ResponseEntity.ok(balanceService.getBalance());
    }

    // Add Money
    @PostMapping("/add")
    public ResponseEntity<BalanceUpdateResponseDTO> addBalance(@Valid @RequestBody BalanceDTO balanceDTO) {

        Balance updatedBalance = balanceService.addBalance(balanceDTO.getAmount());
        BalanceUpdateResponseDTO response = new BalanceUpdateResponseDTO(
                "Balance added successfully.",
                balanceDTO.getAmount(),
                updatedBalance.getAvailableBalance());

        return ResponseEntity.ok(response);
    }
}