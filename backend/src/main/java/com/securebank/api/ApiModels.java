package com.securebank.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApiModels {
  private ApiModels() {}
  public record RegisterRequest(String email, String password, String fullName) {}
  public record LoginRequest(String email, String password) {}
  public record OtpChallenge(String email, String message, String developmentCode) {}
  public record VerifyEmailRequest(String email, String code) {}
  public record Profile(UUID id, String email, String fullName, String role, String status) {}
  public record Account(UUID id, String accountNumber, String accountType, String currency,
                        BigDecimal balance, String status) {}
  public record Beneficiary(UUID id, String name, String destinationAccount) {}
  public record BeneficiaryRequest(String name, String destinationAccount) {}
  public record TransferRequest(UUID sourceAccountId, String destinationAccount, BigDecimal amount,
                                String description) {}
  public record Transaction(UUID id, String type, BigDecimal amount, String currency,
                            String description, String reference, String counterpartyAccount,
                            String status, OffsetDateTime createdAt) {}
  public record TransferResponse(String reference, BigDecimal amount, String currency,
                                 String status) {}
}
