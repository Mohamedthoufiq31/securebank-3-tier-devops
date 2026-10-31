package com.securebank.api;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import static com.securebank.api.ApiModels.*;

@RestController
@RequestMapping("/api")
public class BankController {
  private final BankService service;
  private final AuthenticationManager authenticationManager;
  public BankController(BankService service, AuthenticationManager authenticationManager) {
    this.service = service; this.authenticationManager = authenticationManager;
  }
  @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
  public OtpChallenge register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }
  @PostMapping("/auth/verify-email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void verifyEmail(@RequestBody VerifyEmailRequest request) { service.verifyEmail(request); }
  @PostMapping("/auth/login")
  public Profile login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
    if (request.email() == null || request.email().isBlank() || request.password() == null || request.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }
    Authentication auth;
    try {
      auth = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
    } catch (DisabledException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Please verify your email before signing in");
    } catch (AuthenticationException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Incorrect email or password");
    }
    SecurityContextHolder.getContext().setAuthentication(auth);
    new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), httpRequest, response);
    return service.profile(((BankUser) auth.getPrincipal()).id());
  }
  @GetMapping("/auth/me") public Profile me(@AuthenticationPrincipal BankUser user) { return service.profile(user.id()); }
  @GetMapping("/accounts") public List<Account> accounts(@AuthenticationPrincipal BankUser user) { return service.accounts(user.id()); }
  @GetMapping("/beneficiaries") public List<Beneficiary> beneficiaries(@AuthenticationPrincipal BankUser user) { return service.beneficiaries(user.id()); }
  @PostMapping("/beneficiaries") @ResponseStatus(HttpStatus.CREATED)
  public Beneficiary add(@AuthenticationPrincipal BankUser user, @RequestBody BeneficiaryRequest request) { return service.addBeneficiary(user.id(), request); }
  @DeleteMapping("/beneficiaries/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@AuthenticationPrincipal BankUser user, @PathVariable UUID id) { service.removeBeneficiary(user.id(), id); }
  @PostMapping("/transfers")
  public TransferResponse transfer(@AuthenticationPrincipal BankUser user, @RequestHeader("X-Idempotency-Key") String key,
                                   @RequestBody TransferRequest request) { return service.transfer(user.id(), key, request); }
  @GetMapping("/transactions") public List<Transaction> transactions(@AuthenticationPrincipal BankUser user, @RequestParam(required = false) String search) {
    return service.transactions(user.id(), search);
  }
}
