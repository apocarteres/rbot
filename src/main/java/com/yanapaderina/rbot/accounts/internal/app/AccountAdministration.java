package com.yanapaderina.rbot.accounts.internal.app;

import com.yanapaderina.rbot.access.Roles;
import io.github.apocarteres.platform.auth.Account;
import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.AuthRefused;
import io.github.apocarteres.platform.auth.NoProfile;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-01, RBOT-DATA-001, REQ-AUTH-009, REQ-DATA-ACCESS-003
@Service
public class AccountAdministration {

  public static final int PAGE_SIZE = 20;
  static final Set<String> GRANTABLE = Set.of(Roles.ADMIN, Roles.PSYCHOLOGIST);

  private final Accounts accounts;

  AccountAdministration(Accounts accounts) {
    this.accounts = accounts;
  }

  @Transactional(readOnly = true)
  public List<Account> page(String query, int page) {
    return accounts.search(query.trim(), Math.max(page, 0) * PAGE_SIZE, PAGE_SIZE);
  }

  @Transactional(readOnly = true)
  public long count(String query) {
    return accounts.count(query.trim());
  }

  @Transactional
  public Account create(String email, String password, Set<String> roles) {
    if (roles.isEmpty() || !GRANTABLE.containsAll(roles)) {
      throw new AccountRefused(AccountRefused.ROLES, "Роли вне перечня " + GRANTABLE);
    }
    if (accounts.findByEmail(email).isPresent()) {
      throw new AuthRefused(AuthRefused.EMAIL_TAKEN, "Почта занята");
    }
    return accounts.create(email, password, roles, true, new NoProfile());
  }

  @Transactional
  public void setPassword(UUID id, String password) {
    accounts.setPassword(existing(id).id(), password);
  }

  @Transactional
  public void block(UUID id, Optional<UUID> actor) {
    Account account = existing(id);
    if (actor.filter(account.id()::equals).isPresent()) {
      throw new AccountRefused(AccountRefused.SELF, "Своя учётная запись не блокируется");
    }
    accounts.block(account.id());
  }

  @Transactional
  public void unblock(UUID id) {
    accounts.unblock(existing(id).id());
  }

  private Account existing(UUID id) {
    return accounts.find(id).orElseThrow(() -> new AccountRefused(AccountRefused.MISSING, "Учётной записи нет"));
  }
}
