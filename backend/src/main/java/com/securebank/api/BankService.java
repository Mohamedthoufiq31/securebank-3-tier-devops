package com.securebank.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.securebank.api.ApiModels.*;

@Service
public class BankService {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final ObjectMapper mapper = new ObjectMapper();
  private final JavaMailSender mailSender;
  private final String mailFrom;
  private final String smtpHost;
  private final Logger log = LoggerFactory.getLogger(BankService.class);
  private final SecureRandom random = new SecureRandom();
  public BankService(JdbcTemplate jdbc, PasswordEncoder encoder, JavaMailSender mailSender,
      @Value("${app.mail-from}") String mailFrom, @Value("${spring.mail.host:}") String smtpHost) {
    this.jdbc = jdbc; this.encoder = encoder; this.mailSender = mailSender; this.mailFrom = mailFrom; this.smtpHost = smtpHost;
  }

  public OtpChallenge register(RegisterRequest request) {
    if (request.email() == null || !request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        || request.password() == null || request.password().length() < 12
        || request.fullName() == null || request.fullName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a valid name, email, and password of at least 12 characters");
    }
    UUID id = UUID.randomUUID();
    try {
      jdbc.update("INSERT INTO app_users(id,email,password_hash,full_name,email_verified) VALUES(?,?,?,?,false)", id,
          request.email().toLowerCase(), encoder.encode(request.password()), request.fullName().trim());
      jdbc.update("INSERT INTO accounts(user_id,account_number,account_type,balance) VALUES(?,?,?,?)",
          id, "SB" + id.toString().replace("-", "").substring(0, 10).toUpperCase(), "CHECKING", new BigDecimal("1000.00"));
    } catch (DuplicateKeyException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    }
    String code = String.format("%06d", random.nextInt(1_000_000));
    jdbc.update("INSERT INTO email_verification_codes(user_id,code_hash,expires_at) VALUES(?,?,now()+interval '10 minutes')",
        id, encoder.encode(code));
    boolean delivered = sendVerificationEmail(request.email().toLowerCase(), code);
    return new OtpChallenge(request.email().toLowerCase(),
        delivered ? "A verification code was sent to your email." : "SMTP is not configured. Use the local development code below.",
        delivered ? null : code);
  }

  public void verifyEmail(VerifyEmailRequest request) {
    var row = jdbc.query("SELECT u.id,c.code_hash,c.expires_at,c.attempts FROM app_users u JOIN email_verification_codes c ON c.user_id=u.id WHERE u.email=?",
        (rs, n) -> new Object[]{rs.getObject("id", UUID.class), rs.getString("code_hash"), rs.getTimestamp("expires_at"), rs.getInt("attempts")},
        request.email().toLowerCase()).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request not found"));
    if (((java.sql.Timestamp) row[2]).before(new java.util.Date()) || ((Integer) row[3]) >= 5)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expired or too many attempts");
    jdbc.update("UPDATE email_verification_codes SET attempts=attempts+1 WHERE user_id=?", row[0]);
    if (!encoder.matches(request.code(), (String) row[1]))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
    jdbc.update("UPDATE app_users SET email_verified=true WHERE id=?", row[0]);
    jdbc.update("DELETE FROM email_verification_codes WHERE user_id=?", row[0]);
  }

  private boolean sendVerificationEmail(String email, String code) {
    if (smtpHost == null || smtpHost.isBlank()) {
      log.warn("SMTP_HOST is not configured. Development verification code for {}: {}", email, code);
      return false;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(mailFrom); message.setTo(email); message.setSubject("SecureBank email verification");
    message.setText("Your SecureBank verification code is " + code + ". It expires in 10 minutes.");
    mailSender.send(message);
    return true;
  }

  public Profile profile(UUID id) {
    return jdbc.queryForObject("SELECT id,email,full_name,role,status FROM app_users WHERE id=?",
        (rs, row) -> new Profile(rs.getObject("id", UUID.class), rs.getString("email"),
            rs.getString("full_name"), rs.getString("role"), rs.getString("status")), id);
  }

  public List<Account> accounts(UUID userId) {
    return jdbc.query("SELECT id,account_number,account_type,currency,balance,status FROM accounts WHERE user_id=? ORDER BY created_at",
        (rs, row) -> new Account(rs.getObject("id", UUID.class), rs.getString("account_number"),
            rs.getString("account_type"), rs.getString("currency"), rs.getBigDecimal("balance"),
            rs.getString("status")), userId);
  }

  public List<Beneficiary> beneficiaries(UUID userId) {
    return jdbc.query("SELECT id,name,destination_account FROM beneficiaries WHERE user_id=? ORDER BY name",
        (rs, row) -> new Beneficiary(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("destination_account")), userId);
  }

  public Beneficiary addBeneficiary(UUID userId, BeneficiaryRequest request) {
    if (request.name() == null || request.name().isBlank() || request.destinationAccount() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and destination account are required");
    }
    Integer count = jdbc.queryForObject("SELECT count(*) FROM accounts WHERE account_number=? AND status='ACTIVE'",
        Integer.class, request.destinationAccount());
    if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination account is not eligible");
    try {
      UUID id = UUID.randomUUID();
      jdbc.update("INSERT INTO beneficiaries(id,user_id,name,destination_account) VALUES(?,?,?,?)",
          id, userId, request.name().trim(), request.destinationAccount());
      return new Beneficiary(id, request.name().trim(), request.destinationAccount());
    } catch (DuplicateKeyException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Beneficiary already exists");
    }
  }

  public void removeBeneficiary(UUID userId, UUID id) {
    if (jdbc.update("DELETE FROM beneficiaries WHERE id=? AND user_id=?", id, userId) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found");
  }

  @Transactional
  public TransferResponse transfer(UUID userId, String idempotencyKey, TransferRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Idempotency-Key is required");
    TransferResponse prior = jdbc.query("SELECT response::text FROM idempotency_records WHERE user_id=? AND idempotency_key=?",
        (rs, row) -> readTransfer(rs.getString(1)), userId, idempotencyKey).stream().findFirst().orElse(null);
    if (prior != null) return prior;
    if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0 || request.amount().scale() > 2
        || request.destinationAccount() == null || request.destinationAccount().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a positive amount with at most two decimals and a destination");
    }
    Account source = jdbc.query("SELECT id,account_number,account_type,currency,balance,status FROM accounts WHERE id=? AND user_id=? FOR UPDATE",
        (rs, row) -> account(rs), request.sourceAccountId(), userId).stream().findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));
    Account destination = jdbc.query("SELECT id,account_number,account_type,currency,balance,status FROM accounts WHERE account_number=? AND status='ACTIVE' FOR UPDATE",
        (rs, row) -> account(rs), request.destinationAccount()).stream().findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination account not found"));
    if (!source.currency().equals(destination.currency())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
    if (source.id().equals(destination.id())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination must differ");
    if (source.balance().compareTo(request.amount()) < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
    String reference = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    jdbc.update("UPDATE accounts SET balance=balance-? WHERE id=?", request.amount(), source.id());
    jdbc.update("UPDATE accounts SET balance=balance+? WHERE id=?", request.amount(), destination.id());
    String description = request.description() == null || request.description().isBlank() ? "Simulated transfer" : request.description().trim();
    jdbc.update("INSERT INTO transactions(account_id,type,amount,currency,description,reference,counterparty_account) VALUES(?,?,?,?,?,?,?)",
        source.id(), "TRANSFER", request.amount(), source.currency(), description, reference, destination.accountNumber());
    jdbc.update("INSERT INTO transactions(account_id,type,amount,currency,description,reference,counterparty_account) VALUES(?,?,?,?,?,?,?)",
        destination.id(), "DEPOSIT", request.amount(), destination.currency(), description, reference, source.accountNumber());
    TransferResponse response = new TransferResponse(reference, request.amount(), source.currency(), "COMPLETED");
    try {
      jdbc.update("INSERT INTO idempotency_records(user_id,idempotency_key,response) VALUES(?,?,?::jsonb)",
          userId, idempotencyKey, write(response));
    } catch (DuplicateKeyException e) {
      return jdbc.queryForObject("SELECT response::text FROM idempotency_records WHERE user_id=? AND idempotency_key=?",
          (rs, row) -> readTransfer(rs.getString(1)), userId, idempotencyKey);
    }
    jdbc.update("INSERT INTO audit_events(user_id,action,details) VALUES(?,?,?::jsonb)", userId, "TRANSFER_COMPLETED",
        "{\"reference\":\"" + reference + "\"}");
    return response;
  }

  public List<Transaction> transactions(UUID userId, String search) {
    String term = search == null ? "" : search.trim();
    return jdbc.query("""
      SELECT t.id,t.type,t.amount,t.currency,t.description,t.reference,t.counterparty_account,t.status,t.created_at
      FROM transactions t JOIN accounts a ON a.id=t.account_id
      WHERE a.user_id=? AND (t.description ILIKE ? OR t.reference ILIKE ?)
      ORDER BY t.created_at DESC LIMIT 100
      """, (rs, row) -> new Transaction(rs.getObject("id", UUID.class), rs.getString("type"),
        rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("description"),
        rs.getString("reference"), rs.getString("counterparty_account"), rs.getString("status"),
        rs.getObject("created_at", OffsetDateTime.class)), userId, "%" + term + "%", "%" + term + "%");
  }

  private Account account(ResultSet rs) throws SQLException {
    return new Account(rs.getObject("id", UUID.class), rs.getString("account_number"), rs.getString("account_type"),
        rs.getString("currency"), rs.getBigDecimal("balance"), rs.getString("status"));
  }
  private String write(TransferResponse response) {
    try { return mapper.writeValueAsString(response); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
  }
  private TransferResponse readTransfer(String json) {
    try { return mapper.readValue(json, TransferResponse.class); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
  }
}
