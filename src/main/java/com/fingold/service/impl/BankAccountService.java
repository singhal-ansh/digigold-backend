package com.fingold.service.impl;

import com.fingold.dto.response.ApiResponse;
import com.fingold.entity.BankAccount;
import com.fingold.entity.User;
import com.fingold.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.fingold.repository.BankAccountRepository;
import com.fingold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ApiResponse.BankAccountResponse getByEmail(String email) {
        User user = getUser(email);
        BankAccount ba = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No bank account linked yet."));
        return toResponse(ba);
    }

    @Transactional
    public ApiResponse.BankAccountResponse save(String email, String accountNumber,
                                                String ifscCode, String accountHolderName,
                                                String bankName) {
        User user = getUser(email);
        BankAccount ba = bankAccountRepository.findByUserId(user.getId())
                .orElse(BankAccount.builder().user(user).build());

        ba.setAccountNumber(accountNumber);
        ba.setIfscCode(ifscCode.toUpperCase());
        ba.setAccountHolderName(accountHolderName);
        ba.setBankName(bankName);
        ba.setVerified(false);

        return toResponse(bankAccountRepository.save(ba));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public ApiResponse.BankAccountResponse toResponse(BankAccount ba) {
        return ApiResponse.BankAccountResponse.builder()
                .id(ba.getId())
                .accountNumber(ba.getAccountNumber())
                .ifscCode(ba.getIfscCode())
                .accountHolderName(ba.getAccountHolderName())
                .bankName(ba.getBankName())
                .verified(ba.isVerified())
                .build();
    }
}
